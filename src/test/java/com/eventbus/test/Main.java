package com.eventbus.test;

import com.ztgreat.eventbus.AsyncEventBus;
import com.ztgreat.eventbus.EventBus;
import com.ztgreat.eventbus.annotation.Subscribe;
import com.ztgreat.eventbus.constant.Priority;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
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
        Executor executor = new ThreadPoolExecutor(1, 10, 10L, TimeUnit.SECONDS, new SynchronousQueue());
        EventBus eventBus = new AsyncEventBus(executor);
        Runner runner = new Runner();
        eventBus.register(runner);
        eventBus.register(new CountinueRunner());
        eventBus.post("Hello World");
    }
}

class Runner {
    @Subscribe(priority = Priority.S_LEVEL)
    public void run(String s) {
        System.out.println("[" + Priority.S_LEVEL + "]"  + "3" + s);
    }
}

class CountinueRunner {
    @Subscribe(priority = Priority.L_LEVEL)
    public void run(String s) {
        System.out.println("[" + Priority.L_LEVEL + "]"  + "1CountinueRunner Running");
    }


    @Subscribe
    public void run2(String s) {
        System.out.println("[" + Priority.M_LEVEL + "]" + "2CountinueRunner Running2");
    }
}

class Runner2 {
    int number;
    public Runner2(int n) {
        this.number = n;
    }

    @Subscribe(priority = Priority.XXL_LEVEL)
    public void run3(String s) {
        System.out.println( "[" + Priority.XXL_LEVEL + "]" +number + "2 Running");
    }
}

class EventRunner {
    @Subscribe
    public void eventRun(Event e){
        System.out.println(e);
    }
}

class Event {
    String a;
    String b;

    @Override
    public String toString() {
        return "Event{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                '}';
    }
}

class PayEvent extends Event {
    Integer money;

    @Override
    public String toString() {
        return "PayEvent{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", money=" + money +
                '}';
    }
}
