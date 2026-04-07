package com.xxywebsite.startup.profiling.agent.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xuxiaoyin
 * @since 2022/12/29
 **/
@Data
public class Selector {
    private String classSelectorExpr;

    private String methodSelectorExpr;

    private String threadMode;

    private static final String USAGE =
            "用法: -javaagent:startup-profiling-agent.jar=[class=<类选择表达式>&]method=<方法选择表达式>[&thread=main|all]\n" +
            "  class  : 可选，类选择表达式，默认匹配所有类\n" +
            "  method : 必填，方法选择表达式\n" +
            "  thread : 可选，线程模式 main(默认) 或 all\n" +
            "示例: -javaagent:startup-profiling-agent.jar=method=any()\n" +
            "示例: -javaagent:startup-profiling-agent.jar=class=nameStartsWith(org.example)&method=any()\n" +
            "示例: -javaagent:startup-profiling-agent.jar=class=nameStartsWith(org.example)&method=any()&thread=all\n" +
            "示例: -javaagent:startup-profiling-agent.jar=class=nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=isAnnotatedWith(@org.springframework.context.annotation.Bean)";

    public Selector(String commandLineArg) {
        if (commandLineArg == null || commandLineArg.isEmpty()) {
            throw new IllegalArgumentException("错误: 未指定javaagent参数。\n" + USAGE);
        }

        // 按 & 分割，但只在紧跟已知参数名的位置分割，避免截断表达式内部内容
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : commandLineArg.split("&(?=(?:class|method|thread)=)")) {
            int idx = part.indexOf('=');
            if (idx < 0) {
                throw new IllegalArgumentException("错误: 参数格式不正确: \"" + part + "\"。\n" + USAGE);
            }
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            params.put(key, value);
        }

        if (!params.containsKey("method")) {
            throw new IllegalArgumentException("错误: 缺少必填参数 'method='。\n" + USAGE);
        }

        // class 可选，默认 any()（匹配所有类）
        String classSelectorExprRaw = params.getOrDefault("class", "any()");
        // 对参数做一些简单的补充，比如加双引号、将"@org.springframework.context.annotation.Bean"变成typePool.describe("org.springframework.context.annotation.Bean").resolve
        classSelectorExprRaw = addQuote(classSelectorExprRaw);
        classSelectorExprRaw = replaceAnnotate(classSelectorExprRaw);
        classSelectorExpr = classSelectorExprRaw;

        String methodSelectorExprRaw = params.get("method");
        methodSelectorExprRaw = addQuote(methodSelectorExprRaw);
        methodSelectorExprRaw = replaceAnnotate(methodSelectorExprRaw);
        methodSelectorExpr = methodSelectorExprRaw;

        // thread 可选，默认 main
        String threadModeRaw = params.getOrDefault("thread", "main");
        if (!"main".equals(threadModeRaw) && !"all".equals(threadModeRaw)) {
            throw new IllegalArgumentException("错误: thread 参数只能为 'main' 或 'all'，收到: \"" + threadModeRaw + "\"。\n" + USAGE);
        }
        threadMode = threadModeRaw;
    }

    private String addQuote(String expr) {
        Pattern pattern = Pattern.compile("(nameStartsWith|nameEndsWith|named)\\((.*?)\\)");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String methodName = matcher.group(1);
            String methodArg = matcher.group(2);
            matcher.appendReplacement(sb, String.format("%s(\"%s\")", methodName, methodArg));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String replaceAnnotate(String expr) {
        Pattern pattern = Pattern.compile("isAnnotatedWith\\(@(.*?)\\)");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String annotationName = matcher.group(1);
            matcher.appendReplacement(sb, String.format("isAnnotatedWith(typePool.describe(\"%s\").resolve())", annotationName));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void main(String[] args) {
        String arg = "class=nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=any()&thread=all";
        Selector selector = new Selector(arg);
        System.out.println(selector.getClassSelectorExpr());
        System.out.println(selector.getMethodSelectorExpr());
        System.out.println(selector.getThreadMode());
    }
}
