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

import org.springframework.messaging.Message;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface RocketMqRateLimiter {

    /**
     * @param destination formats: `topicName:tags`
     * @param message 消息
     */
    void sendMessage(String destination, Message<byte[]> message);

    /**
     * @param destination formats: `topicName:tags`
     * @param message 消息
     */
    void asyncSendMessage(String destination, Message<byte[]> message);

    default boolean isLimited(String destination) {
        return false;
    }

}
