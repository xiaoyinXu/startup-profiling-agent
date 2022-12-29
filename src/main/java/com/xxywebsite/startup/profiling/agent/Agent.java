package com.xxywebsite.startup.profiling.agent;

import com.xxywebsite.startup.profiling.agent.dto.Selector;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author xuxiaoyin
 * @since 2022/12/27
 **/
public class Agent {
    /**
     * -javaagent:startup-profiling-agent.jar=class=nameStartsWith(com.jd)&method=isAnnotatedWith(org.springframework.context.annotation.Bean)
     *
     * @param args
     * @param instrumentation
     * @throws Exception
     */
    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        if (args == null || args.isEmpty()) {
            System.err.println("请指定javaagent参数(profiling的包路径)，例如org.example");
            System.exit(1);
        }

        Selector selector = null;
        try {
            selector = new Selector(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // 运行时编译 后续方便拓展
        Helper.instrument(selector.getClassSelectorExpr(), selector.getMethodSelectorExpr(), instrumentation);

        // 构建时编译
//        TypePool typePool = TypePool.Default.ofSystemLoader();
//        new AgentBuilder
//                .Default()
//                .type(isAnnotatedWith(typePool.describe("org.springframework.boot.autoconfigure.SpringBootApplication").resolve()))
//                .transform(new AgentBuilder.Transformer() {
//                    @Override
//                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain protectionDomain) {
//                        return builder.method(ElementMatchers.any())
//                                .intercept(MethodDelegation.to(AroundLogInterceptor.class));
//                    }
//                })
//                .type(nameStartsWith("org.example").and(isAnnotatedWith(typePool.describe("org.springframework.context.annotation.Configuration").resolve())))
//                .transform((new AgentBuilder.Transformer() {
//                    @Override
//                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain protectionDomain) {
//                        return builder.method(isAnnotatedWith(typePool.describe("org.springframework.context.annotation.Bean").resolve())).intercept(MethodDelegation.to(TimingInterceptor.class));
//                    }
//                }))
//                .installOn(instrumentation);

    }

    private static Selector resolveArgs(String args) {
        return null;
    }


    public static class Helper {
        /**
         * 以下代码可以build时编译，也可以运行时编译
         * 运行时编译的好处是可以随意修改源码，方便结合读取配置文件。
         *
         * @param instrumentation
         * @throws Exception
         */
        public static void instrument(String classSelectorExpr, String methodSelectorExpr, Instrumentation instrumentation) throws Exception {
            String source = String.format("import com.xxywebsite.startup.profiling.agent.interceptor.AroundLogInterceptor;\n" +
                    "import com.xxywebsite.startup.profiling.agent.interceptor.TimingInterceptor;\n" +
                    "import startup.shade.net.bytebuddy.agent.builder.AgentBuilder;\n" +
                    "import startup.shade.net.bytebuddy.description.type.TypeDescription;\n" +
                    "import startup.shade.net.bytebuddy.dynamic.DynamicType;\n" +
                    "import startup.shade.net.bytebuddy.implementation.MethodDelegation;\n" +
                    "import startup.shade.net.bytebuddy.matcher.ElementMatchers;\n" +
                    "import startup.shade.net.bytebuddy.pool.TypePool;\n" +
                    "import startup.shade.net.bytebuddy.utility.JavaModule;\n" +
                    "\n" +
                    "import javax.tools.JavaCompiler;\n" +
                    "import javax.tools.ToolProvider;\n" +
                    "import java.io.File;\n" +
                    "import java.lang.instrument.Instrumentation;\n" +
                    "import java.lang.reflect.Method;\n" +
                    "import java.net.URL;\n" +
                    "import java.net.URLClassLoader;\n" +
                    "import java.nio.charset.StandardCharsets;\n" +
                    "import java.nio.file.Files;\n" +
                    "import java.security.ProtectionDomain;\n" +
                    "\n" +
                    "import static startup.shade.net.bytebuddy.matcher.ElementMatchers.*;\n" +
                    "public class  StartupProfilingAgent {" +
                    "public static void instrument(Instrumentation instrumentation) {" +
                    "        TypePool typePool = TypePool.Default.ofSystemLoader();" +
                    "        new AgentBuilder\n" +
                    "                .Default()\n" +
                    "                .type(%s)\n" +
                    "                .transform((new AgentBuilder.Transformer() {\n" +
                    "                    @Override\n" +
                    "                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain protectionDomain) {\n" +
                    "                        return builder.method(%s).intercept(MethodDelegation.to(TimingInterceptor.class));\n" +
                    "                    }\n" +
                    "                }))\n" +
                    "                .installOn(instrumentation);\n" +
                    "}" +
                    "}", classSelectorExpr, methodSelectorExpr);

            File root = new File("./temp");
            File sourceFile = new File(root, "StartupProfilingAgent.java");
            sourceFile.getParentFile().mkdirs();
            Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, sourceFile.getPath());

            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
            Class<?> cls;
            cls = Class.forName("StartupProfilingAgent", true, classLoader);


            Method method = cls.getDeclaredMethod("instrument", Instrumentation.class);
            method.invoke(null, instrumentation);
        }
    }

}
