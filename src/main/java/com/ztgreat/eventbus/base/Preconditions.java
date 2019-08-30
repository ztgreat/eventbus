package com.ztgreat.eventbus.base;

/**
 * <p> the common check for other method </p>
 *
 * @author chenglu
 * @date 2019/8/29
 */
public class Preconditions {
    private Preconditions() {}

    public static <T extends Object> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
