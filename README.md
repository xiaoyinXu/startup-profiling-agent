# startup-profiling-agent

用于排查Java应用启动慢的瓶颈方法，主要是打印方法的调用耗时。

可以通过表达式(基于[ByteBuddy ElementMatchers Java API](https://javadoc.io/doc/net.bytebuddy/byte-buddy/latest/index.html)) 来选择具体对哪些类下的哪些方法进行监控。

### 举例
案例1：`org.example前缀类下的所有方法`

类选择器表达式: nameStartsWith(org.example)

方法选择器表达式: any()

命令如下：
```shell
java -javaagent:'{startup-profiling-agent}.jar=class=nameStartsWith(org.example)&method=any()'  -jar {your-spring-boot-application}.jar`
```

案例2：`org.example前缀并且被@Configuration注解类下的所有@Bean方法`

类选择器表达式: nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))

方法选择器表达式: isAnnotatedWith(@org.springframework.context.annotation.Bean)

命令如下：
```shell
java -javaagent:'{startup-profiling-agent}.jar=class=nameStartsWith(org.example).and(isAnnotatedWith(@org.springframework.context.annotation.Configuration))&method=isAnnotatedWith(@org.springframework.context.annotation.Bean)'  -jar {your-spring-boot-application}.jar`
```




### 控制台输出结果

```
startup-profiling-agent: 方法:org.example.a.b.MyConfiguration.fn1, 耗时:3ms
startup-profiling-agent: 方法:org.example.c.d.OtherConfiguration.fn2, 耗时:177ms
```
