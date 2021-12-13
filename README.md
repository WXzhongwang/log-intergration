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

# MDC 核心

> MDC（Mapped Diagnostic Context，映射调试上下文）是 log4j 、logback及log4j2 提供的一种方便在多线程条件下记录日志的功能。 MDC 可以看成是一个与当前线程绑定的哈希表， 可以往其中添加键值对。 MDC 中包含的内容可以被同一线程中执行的代码所访问。当前线程的子线程会继承其父线程中的 MDC 的内容。 当需要记录日志时，只需要从 MDC 中获取所需的信息即可。MDC 的内容则由程序在适当的时候保存进去。 对于一个 Web 应用来说，通常是在请求被处理的最开始保存这些数据。

实现思路： 通过拦截器，对调用HTTP服务进行拦截，打印TRACE_ID.

## step1: 定义拦截器

```
package com.example.log4j2.configure;

import com.example.log4j2.interceptor.LogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * 拦截器配置类
 *
 * @author zhongshengwang
 */
@Configuration
public class InterceptorConfig extends WebMvcConfigurationSupport {

    /**
     * 实现拦截器 要拦截的路径以及不拦截的路径
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册自定义拦截器，添加拦截路径和排除拦截路径
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");
    }
}
```

## step2: 拦截器

```
package com.example.log4j2.interceptor;

import com.example.log4j2.constant.Constants;
import com.example.log4j2.util.TraceIdUtil;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 拦截器，为所有请求添加一个traceId
 *
 * @author zhongshengwang
 */
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果有上层调用就用上层的ID
        String traceId = request.getHeader(Constants.TRACE_ID);
        if (traceId == null) {
            traceId = TraceIdUtil.getTraceId();
        }

        MDC.put(Constants.TRACE_ID, traceId);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 调用结束后删除
        MDC.remove(Constants.TRACE_ID);
    }
}

```

## step3: 包装HTTP REQUEST

对常用HTTP请求方式添加MDC context

### HttpClient

```
package com.example.log4j2.util;

import com.alibaba.fastjson.JSONObject;
import com.example.log4j2.interceptor.HttpClientTraceIdInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * HttpClient工具类
 *
 * @author zhongshengwang
 */
public class HttpClientUtil {

    private static CloseableHttpClient httpClient = HttpClientBuilder.create()
            .addInterceptorFirst(new HttpClientTraceIdInterceptor())
            .build();

    /**
     * GET请求
     *
     * @param url 请求地址
     * @return
     */
    public static String doGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = null;
        try {
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSONObject.toJSONString(result);
    }
}


```

### OkHttp

```
package com.example.log4j2.util;

import com.alibaba.fastjson.JSONObject;
import com.example.log4j2.interceptor.OkHttpTraceIdInterceptor;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * OkHttp工具类
 *
 * @author zhongshengwang
 */
public class OkHttpUtil {
    private static OkHttpClient client = new OkHttpClient.Builder()
            .addNetworkInterceptor(new OkHttpTraceIdInterceptor())
            .build();

    /**
     * GET请求
     *
     * @param url 请求地址
     * @return
     */
    public static String doGet(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSONObject.toJSONString(response.body());
    }
}

```

### RestTemplate

```
package com.example.log4j2.util;

import com.alibaba.fastjson.JSONObject;
import com.example.log4j2.interceptor.RestTemplateTraceIdInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * RestTemplate工具类
 *
 * @author zhongshengwang
 */
public class RestTemplateUtil {
    
    private static RestTemplate restTemplate = new RestTemplate();

    /**
     * GET请求
     *
     * @param url 请求地址
     * @return
     */
    public static String doGet(String url) {
        restTemplate.setInterceptors(Arrays.asList(new RestTemplateTraceIdInterceptor()));
        return JSONObject.toJSONString(restTemplate.getForObject(url, String.class));
    }
}

```

## step4： 多线程调用时产生的MDC使用

```
思路： 复写线程池
package com.example.log4j2.wrapper;

import com.example.log4j2.util.ThreadMdcUtil;
import org.slf4j.MDC;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池包装类
 *
 * @author zhongshengwang
 */
public class ThreadPoolExecutorMdcWrapper extends ThreadPoolExecutor {
    public ThreadPoolExecutorMdcWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public ThreadPoolExecutorMdcWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPoolExecutorMdcWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPoolExecutorMdcWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                        RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable task) {
        super.execute(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }
}





```

ThreadMdcUtil:

```
package com.example.log4j2.util;

import com.example.log4j2.constant.Constants;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 线程MDC包装类
 * <p>
 * 线程级别记录 MDC
 *
 * @author
 */
public class ThreadMdcUtil {

    public static void setTraceIdIfAbsent() {
        if (MDC.get(Constants.TRACE_ID) == null) {
            MDC.put(Constants.TRACE_ID, TraceIdUtil.getTraceId());
        }
    }

    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                return callable.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return new Runnable() {
            @Override
            public void run() {
                if (context == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(context);
                }
                setTraceIdIfAbsent();
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            }
        };
    }
}

```

### step5: HTTP RESPONSE

还有一个比较重要的点是，我们需要在接口返回时将 TraceId 返回给前端，我们当然不可能在每个接口那里植入返回 TraceId 的代码 ，而是利用 ResponseBodyAdvice，可以在接口结果返回前，对返回结果进行进一步处理。

> 这个代码我就没写到这里了, 偷个懒。==^==

```
/**
 * Response Advice
 * @author zhongshengwang
 **/
@RestControllerAdvice(basePackages = "com.example.log4j2")
public class WebResponseModifyAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(final MethodParameter methodParameter, final Class converterType) {
        // 返回 class 为 ApiResult（带 TraceId 属性） & converterType 为 Json 转换
        return methodParameter.getMethod().getReturnType().isAssignableFrom(ApiResult.class)
                && converterType.isAssignableFrom(MappingJackson2HttpMessageConverter.class);
    }

    @Override
    public Object beforeBodyWrite(final Object body, final MethodParameter methodParameter, final MediaType mediaType, final Class aClass,
                                  final ServerHttpRequest serverHttpRequest, final ServerHttpResponse serverHttpResponse) {
        // 设置 TraceId
        ((ApiResult<?>) body).setTraceId(MDC.get(Constants.TRACE_ID));
        return body;
    }
}
```