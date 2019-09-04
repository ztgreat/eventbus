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

import cn.hutool.core.collection.CollectionUtil;
import com.deepexi.eventbus.util.ReflectionUtils;
import com.sun.istack.internal.Nullable;
import com.deepexi.eventbus.annotation.Subscribe;
import com.deepexi.eventbus.base.Collections;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static com.deepexi.eventbus.base.Preconditions.checkNotNull;


/**
 * Registry of subscribers to a single event bus.
 *
 * @author Colin Decker
 */
final class SubscriberRegistry {
    /**
     * log print utils
     */
    private static final Logger LOGGER = Logger.getLogger(SubscriberRegistry.class.getName());

    /**
     * All registered subscribers, indexed by event type.
     *
     * <p>The {@link java.util.List List} values make it easy and relatively lightweight to get an
     * immutable snapshot of all current subscribers to an event without any locking.
     */
    private final ConcurrentMap<Class<?>, List<Subscriber>> subscribersInSameEventType = Collections.newConcurrentMap();
    /**
     * record the register listeners in EventBus, the value is the listener's subscribe methods
     */
    private final ConcurrentMap<Object, List<Subscriber>> subscribersInSameListener = Collections.newConcurrentMap();

    private final ConcurrentMap<Class, List<SubscribeMethod>> classSubscribeMethods = Collections.newConcurrentMap();

    /** The event bus this registry belongs to. */
    private final EventBus bus;

    SubscriberRegistry(EventBus bus) {
        this.bus = checkNotNull(bus);
    }

    /** Registers all subscriber methods on the given listener object. */
    synchronized void register(Object listener) {
        Class clazz = listener.getClass();
        if (subscribersInSameListener.get(listener) != null) {
            LOGGER.info("Listener[" + clazz.getName() + ": " + listener + "] has been register in EventBus, ignore this register.");
            return;
        }
        // get the subscribe methods in the listener
        List<SubscribeMethod> subscribeMethods = findSubscriberMethods(clazz);
        if (subscribeMethods == null) {
            LOGGER.info("Listener[" + clazz.getName() + ": " + listener + "] has no subscribed methods, ignore this register.");
            return;
        }
        // create new Subscriber and register into subscribersInSameEventType
        doRegister(listener, subscribeMethods);
    }

    /**
     * create the Subscriber and register in subscribersInSameEventType
     * @param listener listener
     * @param subscribeMethods the subscribe methods in the listener
     */
    private void doRegister(Object listener, List<SubscribeMethod> subscribeMethods) {
        List<Subscriber> listenerSubscribers = Collections.newArrayList();
        for (SubscribeMethod subscribeMethod : subscribeMethods) {
            Class eventType = subscribeMethod.getEventType();
            List<Subscriber> subscribers = subscribersInSameEventType.computeIfAbsent(eventType, k -> Collections.newArrayList());
            Subscriber subscriber = Subscriber.create(bus, listener, subscribeMethod);
            if (subscribers.contains(subscriber)) {
                continue;
            }
            subscribers.add(subscriber);
            subscribers.sort((s1, s2) -> s2.getSubscribeMethod().getPriority() - s1.getSubscribeMethod().getPriority());
            listenerSubscribers.add(subscriber);
        }
        if (listenerSubscribers.size() == 0) {
            return;
        }
        subscribersInSameListener.put(listener, listenerSubscribers);
    }

    /**
     * get the class of the listener
     * @param listenerClazz listener class
     * @return the method subscribe in the listener
     */
    private List<SubscribeMethod> findSubscriberMethods(Class listenerClazz) {
        List<SubscribeMethod> subscribeMethods = classSubscribeMethods.get(listenerClazz);
        if (subscribeMethods != null) {
            return subscribeMethods;
        }
        Map<MethodIdentifier, SubscribeMethod> identifiers = Collections.newHashMap();
        List<Method> methods = ReflectionUtils.getMethodByAnnotation(listenerClazz, Subscribe.class);
        if (CollectionUtil.isEmpty(methods)) {
            return null;
        }
        for (Method method : methods) {
            if (method.isSynthetic()) {
                continue;
            }
            // check the count of parameters
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Target method[" + listenerClazz.getName() + "#" + method.getName()
                        + "] can only be defined in 1 parameter, but found " + parameterTypes.length + " now.");
            }
            Subscribe subscriber = method.getAnnotation(Subscribe.class);
            SubscribeMethod subscribeMethod = SubscribeMethod.Builder.aSubscribeMethod()
                    .withMethod(method)
                    .withEventType(parameterTypes[0])
                    .withPriority(subscriber.priority())
                    .build();
            // the unique checkout of the method
            MethodIdentifier ident = new MethodIdentifier(method);
            if (!identifiers.containsKey(ident)) {
                identifiers.put(ident, subscribeMethod);
            }
        }
        subscribeMethods = new ArrayList<>(identifiers.values());
        if (subscribeMethods.size() == 0) {
            return null;
        }
        classSubscribeMethods.put(listenerClazz, subscribeMethods);
        return subscribeMethods;
    }

    /** Unregisters all subscribers on the given listener object. */
    void unregister(Object listener) {
        List<Subscriber> listenerMethods = subscribersInSameListener.get(listener);
        if (listenerMethods == null) {
            return;
        }
        for (Subscriber subscriber : listenerMethods) {
            List<Subscriber> currentSubscribers = subscribersInSameEventType.get(subscriber.getSubscribeMethod().getEventType());
            if (currentSubscribers == null || !currentSubscribers.remove(subscriber)) {
                // if remove returns true, all we really know is that at least one subscriber was
                // removed... however, barring something very strange we can assume that if at least one
                // subscriber was removed, all subscribers on listener for that event type were... after
                // all, the definition of subscribers on a particular class is totally static
                throw new IllegalArgumentException(
                        "missing event subscriber for an annotated method. Is " + listener + " registered?");
            }
        }
    }

    /**
     * Gets an iterator representing an immutable snapshot of all subscribers to the given event at
     * the time this method is called.
     */
    Iterator<Subscriber> getSubscribers(Object event) {

        List<Subscriber> eventSubscribers = subscribersInSameEventType.get(event.getClass());
        if (eventSubscribers == null) {
            return null;
        }
        return eventSubscribers.iterator();
    }

    private static final class MethodIdentifier {

        private final String name;
        private final List<Class<?>> parameterTypes;

        MethodIdentifier(Method method) {
            this.name = method.getName();
            this.parameterTypes = Arrays.asList(method.getParameterTypes());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{name, parameterTypes});
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof MethodIdentifier) {
                MethodIdentifier ident = (MethodIdentifier) o;
                return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
            }
            return false;
        }
    }
}
