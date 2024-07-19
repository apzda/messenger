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

import com.apzda.cloud.msg.Postman;
import com.apzda.cloud.msg.RocketMail;
import com.apzda.cloud.msg.TextMail;
import com.apzda.cloud.msg.mq.FixedRateLimiter;
import com.apzda.cloud.msg.mq.RocketMqRateLimiter;
import com.apzda.cloud.msg.postman.DemoPostman;
import com.apzda.cloud.msg.postman.RocketMqPostman;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@ComponentScan({ "com.apzda.cloud.msg.consumer", "com.apzda.cloud.msg.service", "com.apzda.cloud.msg.converter" })
@EnableConfigurationProperties(MessengerServiceProperties.class)
@Import(RedisBasedRateLimiterConfig.class)
public class MessengerServiceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "apzda.cloud.postman", name = "demo-enabled", havingValue = "true",
            matchIfMissing = true)
    Postman<String, TextMail> demoPostman() {
        return new DemoPostman();
    }

    @Bean
    @ConditionalOnMissingBean
    RocketMqRateLimiter rocketMqRateLimiter(RocketMQTemplate rocketMQTemplate, MessengerServiceProperties properties) {
        return new FixedRateLimiter(rocketMQTemplate, properties.getLimitRetry());
    }

    @Bean
    @ConditionalOnMissingBean(name = "rocketmqPostman")
    Postman<String, RocketMail> rocketmqPostman(RocketMqRateLimiter rocketMqRateLimiter) {
        return new RocketMqPostman(rocketMqRateLimiter);
    }

}
