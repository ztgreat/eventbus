package com.deepexi.eventbus.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 反射工具类.
 * @author zt
 */
public class ReflectionUtils {

    /**
     *
     * 获取对象中的所有annotationClass注解的 方法（包含父类方法）
     *
     * @param targetClass
     *            目标对象Class
     * @param annotationClass
     *            注解类型Class
     *
     * @return List
     */
    public static <T extends Method> List<T> getMethodByAnnotation(
            Class targetClass, Class annotationClass) {
        Assert.notNull(targetClass, "targetClass不能为空");
        Assert.notNull(annotationClass, "annotationClass不能为空");

        List<T > result = new ArrayList<>();

        Method[] methods = targetClass.getDeclaredMethods();
        // 获取方法中的注解
        CollectionUtil.addAll(result, getMethodByAnnotation(methods, annotationClass));

        for (Class<?> superClass = targetClass.getSuperclass(); superClass != null
                && superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            List<T> temp = getMethodByAnnotation(superClass, annotationClass);
            if (CollectionUtil.isNotEmpty(temp)) {
                CollectionUtil.addAll(result, temp.iterator());
            }
        }

        return result;
    }


    /**
     * 获取method的annotationClass注解
     *
     * @param method
     *            method对象
     * @param annotationClass
     *            annotationClass注解
     *
     * @return {@link Annotation}
     */
    public static <T extends Annotation> T getAnnotation(Method method,
                                                         Class annotationClass) {

        Assert.notNull(method, "method不能为空");
        Assert.notNull(annotationClass, "annotationClass不能为空");

        method.setAccessible(true);
        if (method.isAnnotationPresent(annotationClass)) {
            return (T) method.getAnnotation(annotationClass);
        }
        return null;
    }


    /**
     * 获取method数组中匹配的annotationClass注解
     *
     * @param methods
     *            method对象数组
     * @param annotationClass
     *            annotationClass注解
     *
     * @return List
     */
    public static <T extends Method> List<T> getMethodByAnnotation(
            Method[] methods, Class annotationClass) {

        if (ArrayUtil.isEmpty(methods)) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();

        for (Method method : methods) {

            Annotation annotation = getAnnotation(method, annotationClass);
            if (annotation != null) {
                result.add((T) method);
            }
        }

        return result;
    }

}
