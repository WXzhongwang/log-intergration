# log4j2 日志模块集成方式

## 注意点

1. Log4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector 异步写入，需要引入依赖

```
 <!-- log4j2 异步日志-->
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
    <version>3.4.2</version>
</dependency>
```

2. 如果项目中引入其他日志框架，需要统一时，记得去除其他依赖

```
比如：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

```

```
日志参数：
参考： https://www.cnblogs.com/bugzeroman/p/12858115.html
```