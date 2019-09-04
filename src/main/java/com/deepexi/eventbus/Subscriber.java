/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.deepexi.eventbus;

import com.deepexi.eventbus.annotation.AllowConcurrentEvents;
import com.sun.istack.internal.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static com.deepexi.eventbus.base.Preconditions.checkNotNull;

/**
 * A subscriber method on a specific object, plus the executor that should be used for dispatching
 * events to it.
 *
 * <p>Two subscribers are equivalent when they refer to the same method on the same object (not
 * class). This property is used to ensure that no subscriber method is registered more than once.
 *
 * @author Colin Decker
 */
class Subscriber {
    private static final Logger LOGGER = Logger.getLogger(Subscriber.class.getName());

    /** Creates a {@code Subscriber} for {@code method} on event {@code class} of the {@code listener}. */
    static Subscriber create(EventBus bus, Object listener, SubscribeMethod subscribeMethod) {
        return isDeclaredThreadSafe(subscribeMethod.getMethod())
                ? new Subscriber(bus, listener, subscribeMethod)
                : new SynchronizedSubscriber(bus, listener, subscribeMethod);
    }

    /** The event bus this subscriber belongs to. */
    private EventBus bus;

    /** The object with the subscriber method. */
    final Object target;

    /** Subscriber method. */
    private final SubscribeMethod subscribeMethod;

    /** Executor to use for dispatching events to this subscriber. */
    private final Executor executor;

    private Subscriber(EventBus bus, Object target, SubscribeMethod subscribeMethod) {
        this.bus = bus;
        this.target = checkNotNull(target);
        this.subscribeMethod = subscribeMethod;
        subscribeMethod.getMethod().setAccessible(true);
        this.executor = bus.executor();
    }

    /** Dispatches {@code event} to this subscriber using the proper executor. */
    final void dispatchEvent(final Object event) {
        executor.execute(() -> {
            LOGGER.info("[EventBus-" + subscribeMethod.getName() +"] model starts invoke.");
            try {
                invokeSubscriberMethod(event);
            } catch (InvocationTargetException e) {
                bus.handleSubscriberException(e.getCause(), context(event));
            }
        });
    }

    /**
     * Invokes the subscriber method. This method can be overridden to make the invocation
     * synchronized.
     */
    void invokeSubscriberMethod(Object event) throws InvocationTargetException {
        try {
            subscribeMethod.getMethod().invoke(target, checkNotNull(event));
        } catch (IllegalArgumentException e) {
            throw new Error("Method rejected target/argument: " + event, e);
        } catch (IllegalAccessException e) {
            throw new Error("Method became inaccessible: " + event, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /** Gets the context for the given event. */
    private SubscriberExceptionContext context(Object event) {
        return new SubscriberExceptionContext(bus, event, target, subscribeMethod);
    }

    @Override
    public final int hashCode() {
        return (31 + subscribeMethod.getMethod().hashCode()) * 31 + System.identityHashCode(target);
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (obj instanceof Subscriber) {
            Subscriber that = (Subscriber) obj;
            // Use == so that different equal instances will still receive events.
            // We only guard against the case that the same object is registered
            // multiple times
            return target == that.target && subscribeMethod.getMethod().equals(that.subscribeMethod.getMethod());
        }
        return false;
    }

    /**
     * Checks whether {@code method} is thread-safe, as indicated by the presence of the {@link
     * com.deepexi.eventbus.annotation.AllowConcurrentEvents} annotation.
     */
    private static boolean isDeclaredThreadSafe(Method method) {
        return method.getAnnotation(AllowConcurrentEvents.class) != null;
    }

    /**
     * Subscriber that synchronizes invocations of a method to ensure that only one thread may enter
     * the method at a time.
     */
    static final class SynchronizedSubscriber extends Subscriber {

        private SynchronizedSubscriber(EventBus bus, Object target, SubscribeMethod subscribeMethod) {
            super(bus, target, subscribeMethod);
        }

        @Override
        void invokeSubscriberMethod(Object event) throws InvocationTargetException {
            synchronized (this) {
                super.invokeSubscriberMethod(event);
            }
        }
    }

    public EventBus getBus() {
        return bus;
    }

    public void setBus(EventBus bus) {
        this.bus = bus;
    }

    public Object getTarget() {
        return target;
    }

    public SubscribeMethod getSubscribeMethod() {
        return subscribeMethod;
    }

    public Executor getExecutor() {
        return executor;
    }
}
