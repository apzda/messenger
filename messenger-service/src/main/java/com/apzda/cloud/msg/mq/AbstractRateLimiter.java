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
package com.apzda.cloud.msg.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class AbstractRateLimiter implements RocketMqRateLimiter {

    private final RocketMQTemplate mqTemplate;

    private final int maxRetry;

    public AbstractRateLimiter(RocketMQTemplate mqTemplate, int maxRetry) {
        this.mqTemplate = mqTemplate;
        this.maxRetry = maxRetry;
    }

    /**
     * 投递消息.
     * @param destination 主题
     * @param message 消息
     */
    @Override
    public void sendMessage(String destination, Message<byte[]> message) {
        sendMessage(destination, message, 0);
    }

    @Override
    public void asyncSendMessage(String destination, Message<byte[]> message) {
        asyncSendMessage(destination, message, 0);
    }

    private void asyncSendMessage(String destination, Message<byte[]> message, int retry) {
        if (isLimited(destination)) {
            return;
        }

        mqTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.debug("第{}次消息投递到{}失败, 重试: {}/{}, 原因: {}", retry, destination, retry, maxRetry, e.getMessage());
                if (retry < maxRetry) {
                    asyncSendMessage(destination, message, retry + 1);
                }
            }
        });
    }

    private void sendMessage(String destination, Message<byte[]> message, int retry) {
        if (isLimited(destination)) {
            return;
        }

        try {
            mqTemplate.send(destination, message);
        }
        catch (Exception e) {
            log.debug("第{}次消息投递到{}失败, 重试: {}/{}, 原因: {}", retry, destination, retry, maxRetry, e.getMessage());

            if (retry < maxRetry && e instanceof MQClientException) {
                sendMessage(destination, message, retry + 1);
            }
            else {
                throw new RuntimeException(e);
            }
        }
    }

}
