package com.example.log4j2.util;

import java.util.UUID;

/**
 * @author zhongshengwang
 */
public class TraceIdUtil {

    public static String getTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }
}
