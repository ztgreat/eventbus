package com.ztgreat.eventbus;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * <p> the total information about the method with annotation {@link com.ztgreat.eventbus.annotation.Subscribe} </p>
 *
 * @author chenglu
 * @date 2019/8/30
 */
public class SubscribeMethod implements Serializable {
    /** the subscribe method **/
    private Method method;
    /** the method's priority, the methods execute order will be decide by the value of this field  **/
    private int priority;
    /** the subscribe event type **/
    private Class eventType;
    /** the method's business name defined by user **/
    private String name;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getEventType() {
        return eventType;
    }

    public void setEventType(Class eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "SubscribeMethod{" +
                "method=" + method +
                ", priority=" + priority +
                ", eventType=" + eventType +
                ", name='" + name + '\'' +
                '}';
    }

    public static final class Builder {
        private Method method;
        private int priority;
        private Class eventType;
        private String name;

        private Builder() {
        }

        public static Builder aSubscribeMethod() {
            return new Builder();
        }

        public Builder withMethod(Method method) {
            this.method = method;
            return this;
        }

        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder withEventType(Class eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public SubscribeMethod build() {
            SubscribeMethod subscribeMethod = new SubscribeMethod();
            subscribeMethod.setMethod(method);
            subscribeMethod.setPriority(priority);
            subscribeMethod.setEventType(eventType);
            subscribeMethod.setName(name);
            return subscribeMethod;
        }
    }
}
