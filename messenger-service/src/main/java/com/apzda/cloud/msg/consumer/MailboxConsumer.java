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

import cn.hutool.core.exceptions.ExceptionUtil;
import com.apzda.cloud.msg.Postman;
import com.apzda.cloud.msg.config.MessengerServiceProperties;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.domain.service.IMailboxService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RocketMQMessageListener(topic = "${apzda.cloud.messenger.producer.topic:MESSENGER_MAILBOX}",
        consumerGroup = "${apzda.cloud.messenger.consumer.group:MAILBOX_CONSUMER}")
@RequiredArgsConstructor
public class MailboxConsumer
        implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener, ApplicationContextAware {

    private final MessengerServiceProperties properties;

    private final IMailboxService mailboxService;

    private ApplicationContext applicationContext;

    @Override
    public void prepareStart(@Nonnull DefaultMQPushConsumer consumer) {
        val namespace = properties.getNamespace();

        consumer.setNamespaceV2(namespace);
        val instanceName = properties.getInstanceName();
        if (StringUtils.isNotBlank(instanceName)) {
            consumer.setInstanceName(instanceName);
        }
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());
        consumer.setConsumeThreadMax(properties.getConsumeThreadMin());
        consumer.setMaxReconsumeTimes(properties.getMaxReconsumeTimes());
        consumer.setConsumeTimeout(properties.getConsumeTimeout());
        consumer.setMqClientApiTimeout(properties.getClientApiTimeout());
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(MessageExt message) {
        val tags = message.getTags();
        val content = new String(message.getBody(), StandardCharsets.UTF_8);
        val msgId = message.getUserProperty("msgId");
        val service = message.getUserProperty("service");
        val title = message.getUserProperty("title");

        log.debug("收到消息: postman({}) - msgId({}) - content({})", tags, msgId, StringUtils.truncate(content, 128));

        Mailbox mailbox = mailboxService.getByPostmanAndMsgId(tags, msgId);
        if (mailbox != null) {
            log.trace("忽略已存在的消息: {}", mailbox);
            return;
        }
        // 保存到数据库
        mailbox = new Mailbox();
        mailbox.setContent(content);
        mailbox.setTitle(title);
        mailbox.setService(service);
        mailbox.setMsgId(msgId);
        mailbox.setPostman(tags);
        mailbox.setStatus(MailStatus.SENDING);
        if (!mailboxService.save(mailbox)) {
            throw new RuntimeException("Cannot save mail into mailbox: " + mailbox);
        }
        try {
            // 立即投递
            val postman = applicationContext.getBean(tags + "Postman", Postman.class);
            val mail = postman.encapsulate(msgId, tags, content);
            mail.setPostman(tags);
            mail.setService(service);
            mail.setTitle(title);
            mail.setId(msgId);
            if (postman.deliver(mail)) {
                mailboxService.markSuccess(mailbox);
            }
            else {
                mailboxService.markFailure(mailbox, "postman(" + tags + ") cannot deliver it.");
            }
        }
        catch (Exception e) {
            try {
                mailboxService.markFailure(mailbox, ExceptionUtil.getSimpleMessage(ExceptionUtil.getRootCause(e)));
            }
            catch (Exception ie) {
                log.warn("Cannot update mailbox status: postman({}) - msgId({}) - {}", tags, msgId,
                        ExceptionUtil.getSimpleMessage(ExceptionUtil.getRootCause(ie)));
            }
        }
    }

}
