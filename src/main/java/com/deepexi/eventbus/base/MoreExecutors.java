package com.deepexi.eventbus.base;

import cn.hutool.core.thread.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p> executor factory </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
public class MoreExecutors {

    /** current thread as the executor **/
    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    /** a new thread as the executor **/
    public static Executor oneThreadExecutor(String identifier) {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().setNamePrefix(identifier).build();
        return new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS
                , new LinkedBlockingQueue<>(65535), threadFactory);
    }
}
