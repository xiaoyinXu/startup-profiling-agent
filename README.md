# startup-profiling-agent

用于排查Spring Boot应用启动慢的瓶颈方法，主要是打印@Bean方法的调用耗时。

### 使用方法，以路径org.example为例
启动时指定javaagent参数

`java -javaagent:{startup-profiling-agent}.jar=org.example -jar {your-spring-boot-application}.jar`

### 控制台输出结果

```
startup-profiling-agent: 方法:org.example.a.b.MyConfiguration.fn1, 耗时:3ms
startup-profiling-agent: 方法:org.example.c.d.OtherConfiguration.fn2, 耗时:177ms
```
