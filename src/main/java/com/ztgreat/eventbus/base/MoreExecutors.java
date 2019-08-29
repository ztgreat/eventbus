package com.ztgreat.eventbus.base;

import java.util.concurrent.Executor;

/**
 * <p> executor factory </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
public class MoreExecutors {

    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }
}
