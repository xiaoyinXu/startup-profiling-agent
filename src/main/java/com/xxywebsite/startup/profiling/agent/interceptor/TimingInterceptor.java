package com.xxywebsite.startup.profiling.agent.interceptor;

import lombok.SneakyThrows;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xuxiaoyin
 * @since 2022/12/27
 **/
public class TimingInterceptor {
    public static Map<String, Long> map = new ConcurrentHashMap<>();

    // 用于倒序打印。  暂未使用
//    public static Map<String, Long> getResultMap() {
//        TreeMap<String, Long> treeMap = new TreeMap<>(new Comparator<String>() {
//            @Override
//            public int compare(String method1, String method2) {
//                Long duration1 = map.get(method1);
//                Long duration2 = map.get(method2);
//                return duration1.compareTo(duration2) != 0 ? duration2.compareTo(duration1) : method1.compareTo(method2);
//            }
//        });
//        treeMap.putAll(map);
//        return treeMap;
//    }

    @RuntimeType
    @SneakyThrows
    public static Object fn(@Origin Method method, @This(optional = true) Object t, @SuperCall Callable<?> callable) {
        long startTs = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            long endTs = System.currentTimeMillis();
            String clazzName = t != null ? t.getClass().getName() : method.getDeclaringClass().getName();
            String name = String.format("%s.%s", clazzName, method.getName());
            long duration = endTs - startTs;
            System.out.println(String.format("startup-profiling-agent: 方法:%s, 耗时:%dms", name, duration));
            map.put(name, duration);
        }
    }
}
