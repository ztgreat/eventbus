# 基于谷歌guava的EventBus

> 大部分代码直接来源于 guava的EventBus

## 我们扩展了什么？
- 对同步EventBus增加了优先级概念，对于订阅同一Event的方法可以根据priority来指定其优先级，我们默认提供了5个优先级，值越大其优先级越高。
- 将EventBus从guava中解耦，无需其他依赖。
## 如何使用？
### 1.maven引入依赖
### 2.使用demo
#### 同步使用
```java
public class Main {
    public static void main(String[] args) {
        EventBus eventBus = new EventBus();
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

```
#### 异步使用
```java
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
```
