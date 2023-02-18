package com.partner.boot.common;

import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发限流
 */
@Component
@Slf4j
public class MyFilter implements Filter {
    //时间窗口
    // 1秒 之内只允许通过 2个 请求
    private static volatile long startTime =  System.currentTimeMillis();
    private static final long windowTime = 1000L;
    public static final int door = 100;
    private static final AtomicInteger bear = new AtomicInteger(0); //桶
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        int count = bear.incrementAndGet(); //只要来人就加1 原子递增
        if(count == 1) {    //并发安全的
            startTime = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        if(now - startTime <= windowTime) {//限制还在
            if( count > door) {    //超过了阈值
                //进行限流操作
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                httpServletResponse.setStatus(HttpStatus.HTTP_OK);
                httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());
                httpServletResponse.getWriter().print(JSONUtil.toJsonStr(Result.error("402", "接口请求太频繁")));
                return;
            }
        }else {
            //重新进入下一个窗口
            startTime = System.currentTimeMillis();
            bear.set(1);
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        chain.doFilter(request, response);
        log.info("接口请求的路径：{}", httpServletRequest.getServletPath());

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
