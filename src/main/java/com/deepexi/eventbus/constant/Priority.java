package com.deepexi.eventbus.constant;

/**
 * <p> the priority in method register in {@link com.deepexi.eventbus.EventBus}
 * , this class is used into the field priority in {@link com.deepexi.eventbus.Subscriber} </p>
 *
 * @author chenglu
 * @date 2019/8/30
 */
public class Priority {
    private Priority(){}
    public static final int S_LEVEL = 0;

    public static final int M_LEVEL = 100;

    public static final int L_LEVEL = 200;

    public static final int XL_LEVEL = 300;

    public static final int XXL_LEVEL = 400;
}
