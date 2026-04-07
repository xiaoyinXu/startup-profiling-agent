package com.xxywebsite.startup.profiling.agent.interceptor;

import lombok.SneakyThrows;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author xuxiaoyin
 * @since 2022/12/27
 **/
public class TimingInterceptor {
    public static Map<String, Long> map = new ConcurrentHashMap<>();

    static {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "startup-profiling-logger");
                t.setDaemon(true);
                return t;
            }
        });
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flushToLog();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    static void flushToLog() {
        if (map.isEmpty()) {
            return;
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(map.entrySet());
        entries.sort(new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) {
                int cmp = Long.compare(e2.getValue(), e1.getValue());
                return cmp != 0 ? cmp : e1.getKey().compareTo(e2.getKey());
            }
        });
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("profiling.log", false))) {
            for (Map.Entry<String, Long> entry : entries) {
                writer.write(String.format("方法:%s, 耗时:%dms", entry.getKey(), entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("startup-profiling-agent: 写入profiling.log失败: " + e.getMessage());
        }
    }

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
