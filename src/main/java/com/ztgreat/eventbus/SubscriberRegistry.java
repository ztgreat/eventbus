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

package com.ztgreat.eventbus;

import com.sun.istack.internal.Nullable;
import com.ztgreat.eventbus.annotation.Subscribe;
import com.ztgreat.eventbus.base.Collections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.ztgreat.eventbus.base.Preconditions.checkNotNull;


/**
 * Registry of subscribers to a single event bus.
 *
 * @author Colin Decker
 */
final class SubscriberRegistry {

  /**
   * All registered subscribers, indexed by event type.
   *
   * <p>The {@link java.util.concurrent.CopyOnWriteArraySet} values make it easy and relatively lightweight to get an
   * immutable snapshot of all current subscribers to an event without any locking.
   */
  private final ConcurrentMap<Class<?>, CopyOnWriteArraySet<Subscriber>> subscribers =
      Collections.newConcurrentMap();

  /** The event bus this registry belongs to. */
  private final EventBus bus;

  SubscriberRegistry(EventBus bus) {
    this.bus = checkNotNull(bus);
  }

  /** Registers all subscriber methods on the given listener object. */
  void register(Object listener) {
    Map<Class<?>, Collection<Subscriber>> listenerMethods = findAllSubscribers(listener);

    for (Entry<Class<?>, Collection<Subscriber>> entry : listenerMethods.entrySet()) {
      Class<?> eventType = entry.getKey();
      Collection<Subscriber> eventMethodsInListener = entry.getValue();

      CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(eventType);

      if (eventSubscribers == null) {
        CopyOnWriteArraySet<Subscriber> newSet = new CopyOnWriteArraySet<>();
        newSet.addAll(eventMethodsInListener);
        subscribers.putIfAbsent(eventType, newSet);
      } else {
          eventSubscribers.addAll(eventMethodsInListener);
      }

//      eventSubscribers.addAll(eventMethodsInListener);
    }
  }

  /** Unregisters all subscribers on the given listener object. */
  void unregister(Object listener) {
    Map<Class<?>, Collection<Subscriber>> listenerMethods = findAllSubscribers(listener);

    for (Entry<Class<?>, Collection<Subscriber>> entry : listenerMethods.entrySet()) {
      Class<?> eventType = entry.getKey();
      Collection<Subscriber> listenerMethodsForType = entry.getValue();

      CopyOnWriteArraySet<Subscriber> currentSubscribers = subscribers.get(eventType);
      if (currentSubscribers == null || !currentSubscribers.removeAll(listenerMethodsForType)) {
        // if removeAll returns true, all we really know is that at least one subscriber was
        // removed... however, barring something very strange we can assume that if at least one
        // subscriber was removed, all subscribers on listener for that event type were... after
        // all, the definition of subscribers on a particular class is totally static
        throw new IllegalArgumentException(
            "missing event subscriber for an annotated method. Is " + listener + " registered?");
      }

      // don't try to remove the set if it's empty; that can't be done safely without a lock
      // anyway, if the set is empty it'll just be wrapping an array of length 0
    }
  }
//
//  Set<Subscriber> getSubscribersForTesting(Class<?> eventType) {
//    return MoreObjects.firstNonNull(subscribers.get(eventType), ImmutableSet.<Subscriber>of());
//  }

  /**
   * Gets an iterator representing an immutable snapshot of all subscribers to the given event at
   * the time this method is called.
   */
  Iterator<Subscriber> getSubscribers(Object event) {

    CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(event.getClass());
    if (eventSubscribers == null) {
        return null;
    }
    return eventSubscribers.iterator();
  }

  /**
   * A thread-safe cache that contains the mapping from each class to all methods in that class and
   * all super-classes, that are annotated with {@code @Subscribe}. The cache is shared across all
   * instances of this class; this greatly improves performance if multiple EventBus instances are
   * created and objects of the same class are registered on all of them.
   */
//  private static final LoadingCache<Class<?>, ImmutableList<Method>> subscriberMethodsCache =
//      CacheBuilder.newBuilder()
//          .weakKeys()
//          .build(
//              new CacheLoader<Class<?>, ImmutableList<Method>>() {
//                @Override
//                public ImmutableList<Method> load(Class<?> concreteClass) throws Exception {
//                  return getAnnotatedMethodsNotCached(concreteClass);
//                }
//              });

  /**
   * Returns all subscribers for the given listener grouped by the type of event they subscribe to.
   */
  private Map<Class<?>, Collection<Subscriber>> findAllSubscribers(Object listener) {
    Map<Class<?>, Collection<Subscriber>> methodsInListener = new HashMap<>(64);
    Class<?> clazz = listener.getClass();
    for (Method method : getAnnotatedMethodsNotCached(clazz)) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      Class<?> eventType = parameterTypes[0];
      Collection<Subscriber> subscribers = methodsInListener.get(eventType);
      if (subscribers == null || subscribers.size() == 0) {
        subscribers = Collections.newArrayList();
        methodsInListener.put(eventType, subscribers);
      }
      subscribers.add(Subscriber.create(bus, listener, method));
    }
    return methodsInListener;
  }

//  private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz) {
//    return subscriberMethodsCache.getUnchecked(clazz);
//  }

  private static Collection<Method> getAnnotatedMethodsNotCached(Class<?> clazz) {
    Map<MethodIdentifier, Method> identifiers = Collections.newHashMap();
    for (Method method : clazz.getMethods()) {
      if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
        // TODO(cgdecker): Should check for a generic parameter type and error out
        Class<?>[] parameterTypes = method.getParameterTypes();
        // check the count of parameters
        MethodIdentifier ident = new MethodIdentifier(method);
        if (!identifiers.containsKey(ident)) {
          identifiers.put(ident, method);
        }
      }
    }
    return identifiers.values();
  }

  /** Global cache of classes to their flattened hierarchy of supertypes. */
//  private static final LoadingCache<Class<?>, ImmutableSet<Class<?>>> flattenHierarchyCache =
//      CacheBuilder.newBuilder()
//          .weakKeys()
//          .build(
//              new CacheLoader<Class<?>, ImmutableSet<Class<?>>>() {
//                // <Class<?>> is actually needed to compile
//                @SuppressWarnings("RedundantTypeArguments")
//                @Override
//                public ImmutableSet<Class<?>> load(Class<?> concreteClass) {
//                  return ImmutableSet.<Class<?>>copyOf(
//                      TypeToken.of(concreteClass).getTypes().rawTypes());
//                }
//              });

  /**
   * Flattens a class's type hierarchy into a set of {@code Class} objects including all
   * superclasses (transitively) and all interfaces implemented by these superclasses.
   */
//  static ImmutableSet<Class<?>> flattenHierarchy(Class<?> concreteClass) {
//    try {
//      return flattenHierarchyCache.getUnchecked(concreteClass);
//    } catch (UncheckedExecutionException e) {
//      throw Throwables.propagate(e.getCause());
//    }
//  }

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
