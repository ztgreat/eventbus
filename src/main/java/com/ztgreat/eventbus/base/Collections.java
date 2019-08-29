package com.ztgreat.eventbus.base;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * <p> the extend of {@link java.util.Collections} </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
public final class Collections {
    private Collections() {}

    public static <E> ArrayDeque<E> newArrayDeque(){
        return new ArrayDeque<>(16);
    }

    public static <E> ConcurrentLinkedQueue<E> newConcurrentLinkedQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>(16);
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>(16);
    }

    public static <E> Collection<E> newArrayList() {
        return new ArrayList<>();
    }
}
