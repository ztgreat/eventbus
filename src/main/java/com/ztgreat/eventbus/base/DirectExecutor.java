package com.ztgreat.eventbus.base;

import java.util.concurrent.Executor;

/**
 * <p> </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
enum DirectExecutor implements Executor {
    /** current executor **/
    INSTANCE;

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
