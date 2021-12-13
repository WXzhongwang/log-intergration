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
