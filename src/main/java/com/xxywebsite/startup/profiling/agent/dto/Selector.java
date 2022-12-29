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

    // class=nameStartsWith(com.jd).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=isAnnotatedWith(@org.springframework.context.annotation.Bean)
    public Selector(String commandLineArg) {
        if (commandLineArg == null || !commandLineArg.matches(ARG_PATTERN)) {
            throw new RuntimeException("请输入正确的命令行参数,格式参照:\"class=...&method=...\"");
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
