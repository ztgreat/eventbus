package com.eventbus.test;

import com.deepexi.eventbus.AsyncEventBus;
import com.deepexi.eventbus.EventBus;
import com.deepexi.eventbus.annotation.Subscribe;
import com.deepexi.eventbus.constant.Priority;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p> </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
public class Main {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
        EventBus eventBus = new EventBus();
        Runner runner = new Runner();
//        eventBus.register(runner);
        eventBus.register(new CountinueRunner());
        eventBus.register(new Runner2(2));
        eventBus.post("Hello World");
    }
}

class Runner {
    @Subscribe(priority = Priority.S_LEVEL)
    public void run(String s) {
        System.out.println("[" + Priority.S_LEVEL + "]" + s);
    }
}

class CountinueRunner {
    @Subscribe(priority = Priority.L_LEVEL, name = "员工模块")
    public void run(String s) {
        System.out.println("[" + Priority.L_LEVEL + "]"  + "CountinueRunner Running");
    }


    @Subscribe
    public void run2(String s) {
        System.out.println("[" + Priority.M_LEVEL + "]" + "CountinueRunner Running2");
    }
}

class Runner2 extends Runner{
    Runner runner = new Runner();
    int number;
    public Runner2(int n) {
        this.number = n;
    }

    @Override
    public void run(String s) {
        runner.run("Son" + s);
    }

    @Subscribe(priority = Priority.XXL_LEVEL)
    public void run3(String s) {
        System.out.println( "[" + Priority.XXL_LEVEL + "]" +number + "2 Running");
    }
}
