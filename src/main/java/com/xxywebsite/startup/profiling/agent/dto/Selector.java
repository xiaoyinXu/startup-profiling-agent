package com.xxywebsite.startup.profiling.agent.dto;

import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xuxiaoyin
 * @since 2022/12/29
 **/
@Data
public class Selector {
    private static final String ARG_PATTERN = "^class=(.*?)&method=(.*?)$";
    private static final String NAME_STARTS_WITH_PATTERN = "nameStartsWith\\((.*?)\\)";

    private String classSelectorExpr;

    private String methodSelectorExpr;

    private static final String USAGE =
            "用法: -javaagent:startup-profiling-agent.jar=class=<类选择表达式>&method=<方法选择表达式>\n" +
            "示例: -javaagent:startup-profiling-agent.jar=class=nameStartsWith(org.example)&method=any()\n" +
            "示例: -javaagent:startup-profiling-agent.jar=class=nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=isAnnotatedWith(@org.springframework.context.annotation.Bean)";

    // class=nameStartsWith(com.jd).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=isAnnotatedWith(@org.springframework.context.annotation.Bean)
    public Selector(String commandLineArg) {
        if (commandLineArg == null) {
            throw new IllegalArgumentException("错误: 未指定javaagent参数。\n" + USAGE);
        }
        if (commandLineArg.isEmpty()) {
            throw new IllegalArgumentException("错误: javaagent参数不能为空。\n" + USAGE);
        }
        if (!commandLineArg.startsWith("class=")) {
            throw new IllegalArgumentException("错误: 参数必须以 'class=' 开头，收到的参数为: \"" + commandLineArg + "\"。\n" + USAGE);
        }
        if (!commandLineArg.matches(".*&method=.*")) {
            throw new IllegalArgumentException("错误: 参数缺少 '&method=' 部分，收到的参数为: \"" + commandLineArg + "\"。\n" + USAGE);
        }
        if (!commandLineArg.matches(ARG_PATTERN)) {
            throw new IllegalArgumentException("错误: 参数格式不正确，收到的参数为: \"" + commandLineArg + "\"。\n" + USAGE);
        }
        Pattern pattern = Pattern.compile(Selector.ARG_PATTERN);
        Matcher matcher = pattern.matcher(commandLineArg);
        matcher.find();

        String classSelectorExprRaw =  matcher.group(1);
        String methodSelectorExprRaw = matcher.group(2);

        // 对参数做一些简单的补充，比如加双引号、将"@org.springframework.context.annotation.Bean"变成typePool.describe("org.springframework.context.annotation.Bean").resolve
        classSelectorExprRaw = addQuote(classSelectorExprRaw);
        classSelectorExprRaw = replaceAnnotate(classSelectorExprRaw);
        classSelectorExpr = classSelectorExprRaw;

        methodSelectorExprRaw = addQuote(methodSelectorExprRaw);
        methodSelectorExprRaw = replaceAnnotate(methodSelectorExprRaw);
        methodSelectorExpr = methodSelectorExprRaw;
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
        String arg = "class=nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=any()";
        Selector selector = new Selector(arg);
        System.out.println(selector.getClassSelectorExpr());
        System.out.println(selector.getMethodSelectorExpr());
    }
}
