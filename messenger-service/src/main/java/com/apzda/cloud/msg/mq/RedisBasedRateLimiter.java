/*
 * This file is part of fanhong-server created at 2023/6/26 by ningGf.
 */
package com.apzda.cloud.msg.mq;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于令牌桶机制的限速器.
 * <p>
 * Created at 2023/6/26 13:17.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class RedisBasedRateLimiter extends AbstractRateLimiter {

    public static final String RATE_LIMITER_KEY = "rocket_rate_limiter_valve";

    public static final String TOKEN_OFFICER_KEY = "rocket_rate_limiter_token";

    private final StringRedisTemplate redisTemplate;

    private final String tokenOfficer;

    private final int rateLimit;

    private ScheduledExecutorService scheduledExecutorService;

    public RedisBasedRateLimiter(RocketMQTemplate mqTemplate, StringRedisTemplate redisTemplate, int rateLimit,
            int maxRetry) {
        super(mqTemplate, maxRetry);
        this.redisTemplate = redisTemplate;
        this.tokenOfficer = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
        this.rateLimit = rateLimit;
        if (rateLimit > 0) {
            // 抢发牌的机会
            redisTemplate.opsForValue().set(RedisBasedRateLimiter.TOKEN_OFFICER_KEY, tokenOfficer);
            val rateLimitStr = String.valueOf(rateLimit);
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (tokenOfficer.equals(redisTemplate.opsForValue().get(RedisBasedRateLimiter.TOKEN_OFFICER_KEY))) {
                    try {
                        redisTemplate.opsForValue().set(RedisBasedRateLimiter.RATE_LIMITER_KEY, rateLimitStr);
                    }
                    catch (Exception e) {
                        log.warn("重置令牌数失败: {}", e.getMessage());
                    }
                }
                else {
                    log.warn("抢令牌数重置权失败，自动退出！");
                    scheduledExecutorService.shutdown();
                }
            }, 1, 1, TimeUnit.SECONDS);
            log.info("投递限速器初始化完成。限速: {}/秒，重试: {}, 令牌官ID: {}", rateLimit, maxRetry, tokenOfficer);
        }
    }

    @Override
    public boolean isLimited(String destination) {
        if (rateLimit > 0) {
            try {
                int wait = 0;
                Long valve = redisTemplate.opsForValue().decrement(RATE_LIMITER_KEY);
                while ((valve == null || valve <= 0) && wait < 10) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    }
                    catch (InterruptedException e) {
                        log.warn("投递消息到: {}，等待超时!", destination);
                    }
                    wait++;
                    valve = redisTemplate.opsForValue().decrement(RATE_LIMITER_KEY);
                }
                if (log.isTraceEnabled()) {
                    log.trace("获取投递机会, 阀值: {}, 等待: {}", valve, wait);
                }
            }
            catch (Exception e) {
                log.warn("获取投递机会时发生异常: {}", e.getMessage());
            }
        }

        return false;
    }

    @PreDestroy
    boolean shutdown() {
        if (scheduledExecutorService == null) {
            return true;
        }
        try {
            log.info("关闭限速器线程池, 等待: {}秒", 60);
            scheduledExecutorService.shutdown();
            return scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            log.warn("关闭限速器线程池出错: {}", e.getMessage());
        }
        finally {
            log.info("限速器线程池已关闭!");
        }
        return false;
    }

}
