package com.xxywebsite.startup.profiling.agent.advice;

import net.bytebuddy.asm.Advice;

/**
 * @author xuxiaoyin
 * @since 2022/12/27
 **/
public class AroundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter() {
        System.out.println("start");
    }
}
