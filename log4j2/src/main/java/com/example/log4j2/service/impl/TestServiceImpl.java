package com.example.log4j2.service.impl;

import com.example.log4j2.service.ITestService;
import com.example.log4j2.util.HttpClientUtil;
import com.example.log4j2.util.OkHttpUtil;
import com.example.log4j2.util.RestTemplateUtil;
import com.example.log4j2.wrapper.ThreadPoolExecutorMdcWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author zhongshengwang
 * @description TODO
 * @date 2021/12/13 8:50 下午
 * @email zhongshengwang@shuwen.com
 */
@Service
public class TestServiceImpl implements ITestService {

    private final static Logger logger = LoggerFactory.getLogger(TestServiceImpl.class);

    @Override
    public void sayHello(String name) {
        ThreadPoolExecutorMdcWrapper threadPoolExecutorMdcWrapper = new ThreadPoolExecutorMdcWrapper(0, Integer.MAX_VALUE,
                10, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        threadPoolExecutorMdcWrapper.execute(() -> logger.info("thread running")
        );

        OkHttpUtil.doGet("http://localhost:8084/api/test");
        HttpClientUtil.doGet("http://localhost:8084/api/test");
        RestTemplateUtil.doGet("http://localhost:8084/api/test");
    }
}
