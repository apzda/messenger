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
package com.apzda.cloud.msg.consumer;

import com.apzda.cloud.msg.config.MessengerServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.springframework.stereotype.Service;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RocketMQMessageListener(topic = "${apzda.cloud.messenger.topic:MESSENGER_MAILBOX}", consumerGroup = "MAILBOX_CONSUMER")
@RequiredArgsConstructor
public class MailboxConsumer implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener {

    private final MessengerServiceProperties properties;

    private final RocketMQProperties rocketMQProperties;

    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        log.error("配置MAILBOX消费者:{}", consumer);
        log.error("配置信息 - 1: {}", properties);
        log.error("配置信息 - 2: {}", rocketMQProperties);
    }

    @Override
    public void onMessage(MessageExt message) {
        log.error("收到消息:{} - {}", message.getTags(), message.getBody());
    }

}
