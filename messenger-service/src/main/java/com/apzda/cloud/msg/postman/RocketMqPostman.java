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
package com.apzda.cloud.msg.postman;

import com.apzda.cloud.msg.Postman;
import com.apzda.cloud.msg.RocketMail;
import com.apzda.cloud.msg.mq.RocketMqRateLimiter;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class RocketMqPostman implements Postman<String, RocketMail> {

    private final RocketMqRateLimiter limiter;

    @Override
    public boolean supports(@Nonnull String postman) {
        return "rocketmq".equals(postman);
    }

    @Override
    public boolean deliver(@Nonnull RocketMail message) {
        val msg = MessageBuilder.withPayload(message.getContent().getBytes(StandardCharsets.UTF_8)).build();
        limiter.sendMessage(message.getRecipients(), msg);
        return true;
    }

    @Nonnull
    @Override
    public RocketMail encapsulate(String id, String postman, String content) {
        val mail = new RocketMail();
        mail.setId(id);
        mail.setPostman(postman);
        mail.setContent(content);
        return mail;
    }

}
