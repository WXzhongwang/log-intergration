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

# logback 接入方式

logback.properties

```
LOG_ERROR_HOME=./springboot-log/logback/error
LOG_INFO_HOME=./springboot-log/logback/info
```

logback-spring.xml

```
<?xml version="1.0" encoding="utf-8"?>
<configuration>
    <property resource="logback.properties"/>

    <appender name="CONSOLE-LOG" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%d [%t] %-5p [%c] - %m%n</pattern>
        </layout>
    </appender>

    <!--收集除error级别以外的日志-->
    <appender name="INFO-LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>DENY</onMatch>
            <onMismatch>ACCEPT</onMismatch>
        </filter>
        <encoder>
            <pattern>%d [%t] %-5p [%c] - %m%n</pattern>
        </encoder>

        <!--滚动策略-->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!--路径-->
            <fileNamePattern>${LOG_INFO_HOME}//%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    <appender name="ERROR-LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>%d [%t] %-5p [%c] - %m%n</pattern>
        </encoder>
        <!--滚动策略-->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!--路径-->
            <fileNamePattern>${LOG_ERROR_HOME}//%d.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="info">
        <appender-ref ref="CONSOLE-LOG"/>
        <appender-ref ref="INFO-LOG"/>
        <appender-ref ref="ERROR-LOG"/>
    </root>
</configuration>


```