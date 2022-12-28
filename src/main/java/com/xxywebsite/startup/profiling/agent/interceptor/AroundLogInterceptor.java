package com.xxywebsite.startup.profiling.agent.interceptor;

import lombok.SneakyThrows;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author xuxiaoyin
 * @since 2022/12/27
 **/
public class AroundLogInterceptor {
    @RuntimeType
    @SneakyThrows
    public static Object fn(@Origin Method method, @This(optional = true) Object t, @SuperCall Callable<?> callable) {
        System.out.println("startup-profiling-agent start");
        try {
            return callable.call();
        } finally {
            System.out.println("startup-profiling-agent end");
        }
    }
}
