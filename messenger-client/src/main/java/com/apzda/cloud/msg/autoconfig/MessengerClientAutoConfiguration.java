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
package com.apzda.cloud.msg.autoconfig;

import com.apzda.cloud.gsvc.config.EnableGsvcServices;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.client.MessengerImpl;
import com.apzda.cloud.msg.config.MessengerClientProperties;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.proto.MessengerService;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration
@EnableGsvcServices(MessengerService.class)
@ComponentScan({ "com.apzda.cloud.msg.domain.service" })
@MapperScan("com.apzda.cloud.msg.domain.mapper")
@ConditionalOnClass(RocketMQAutoConfiguration.class)
@EnableConfigurationProperties(MessengerClientProperties.class)
public class MessengerClientAutoConfiguration {

    @Value("${spring.transaction.default-timeout:-1}")
    private Duration defaultTransactionTimeout;

    @Bean
    Messenger messengerImpl(MessengerClientProperties properties, RocketMQProperties mqProperties,
            IMailboxTransService mailboxService) throws MQClientException {
        return new MessengerImpl(properties, mqProperties, mailboxService, defaultTransactionTimeout);
    }

}
