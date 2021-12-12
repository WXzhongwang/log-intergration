package com.example.log4j2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhongshengwang
 * @description TODO
 * @date 2021/12/12 8:55 下午
 * @email zhongshengwang@shuwen.com
 */
@RestController
public class TestController {

    private final static Logger logger = LoggerFactory.getLogger(TestController.class);

    /**
     * Log4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     * <p>
     * DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     * 开启异步日志.Log4j2.0基于LMAX Disruptor的异步日志在多线程环境下性能会远远优于Log4j 1.x和logback（官方数据是10倍以上）。
     *
     * @return
     */
    @GetMapping("/api/test")
    public Object hello() {
        logger.trace("【TestController.class】trace level log input");
        System.out.println("【TestController.class】trace level log input");
        logger.debug("【TestController.class】debug level log input");
        System.out.println("【TestController.class】debug level log input");
        logger.info("【TestController.class】info level log input");
        System.out.println("【TestController.class】info level log input");
        logger.warn("【TestController.class】warn level log input");
        System.out.println("【TestController.class】warn level log input");
        logger.error("【TestController.class】error level log input");
        System.out.println("【TestController.class】error level log input");
        return "hello world";
    }
}
