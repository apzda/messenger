/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.msg.config;

import com.apzda.cloud.msg.mq.RedisBasedRateLimiter;
import com.apzda.cloud.msg.mq.RocketMqRateLimiter;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(StringRedisTemplate.class)
class RedisBasedRateLimiterConfig {

    @Bean
    RocketMqRateLimiter rocketMqRateLimiter(RocketMQTemplate rocketMQTemplate, StringRedisTemplate stringRedisTemplate,
            MessengerServiceProperties properties) {
        return new RedisBasedRateLimiter(rocketMQTemplate, stringRedisTemplate, properties.getLimitRate(),
                properties.getLimitRetry());
    }

}
