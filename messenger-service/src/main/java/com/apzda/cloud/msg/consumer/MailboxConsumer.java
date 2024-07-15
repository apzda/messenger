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
import jakarta.annotation.PreDestroy;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RocketMQMessageListener(topic = "${apzda.cloud.messenger.topic:MESSENGER_MAILBOX}",
        consumerGroup = "${apzda.cloud.postman.group:MAILBOX_CONSUMER}")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "apzda.cloud.postman", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MailboxConsumer implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener,
        ApplicationContextAware, Runnable {

    private final MessengerServiceProperties properties;

    private final IMailboxService mailboxService;

    private final Clock clock;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    private ScheduledThreadPoolExecutor executor;

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

        log.info("a consumer used by Messenger ({}) init on namesrv {}", consumer.getConsumerGroup(),
                consumer.getNamesrvAddr());

        val executorCount = properties.getExecutorCount();
        executor = new ScheduledThreadPoolExecutor(
                executorCount < 1 ? Math.max(1, Runtime.getRuntime().availableProcessors() / 4) : executorCount, r -> {
                    val thread = new Thread(r);
                    thread.setName("postman-" + atomicInteger.getAndAdd(1));
                    thread.setDaemon(true);
                    return thread;
                });

        val delay = properties.getDelay().toSeconds();
        val period = properties.getPeriod().toSeconds();
        for (int i = 0; i < executorCount; i++) {
            executor.scheduleAtFixedRate(this, delay > 0 ? delay : 10, period > 0 ? period : 1, TimeUnit.SECONDS);
        }
        log.info("Postman executor init: count={}, delay={}, period={} ", executorCount, delay, period);
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onMessage(MessageExt message) {
        val tags = message.getTags();
        val content = new String(message.getBody(), StandardCharsets.UTF_8);
        val msgId = message.getUserProperty("msgId");
        val service = message.getUserProperty("service");
        val title = message.getUserProperty("title");
        val recipients = message.getUserProperty("recipients");
        val postTime = message.getUserProperty("postTime");

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
        mailbox.setRetries(0);
        mailbox.setNextRetryAt(clock.millis());
        mailbox.setRecipients(recipients);
        try {
            mailbox.setPostTime(Long.parseLong(postTime));
        }
        catch (Exception ignored) {
            mailbox.setPostTime(mailbox.getNextRetryAt());
        }

        if (!mailboxService.save(mailbox)) {
            // 利用RocketMQ的重试机制
            throw new RuntimeException("Cannot save mail into mailbox: " + mailbox);
        }
        // 立即投递
        deliver(mailbox);
    }

    @Override
    public void run() {
        Mailbox mailbox;
        do {
            mailbox = mailboxService.getByStatusAndNextRetryAtLe(MailStatus.RETRYING, clock.millis());
            if (mailbox != null) {
                mailbox.setStatus(MailStatus.SENDING);
                if (!mailboxService.updateStatus(mailbox, MailStatus.RETRYING)) {
                    continue;
                }
                deliver(mailbox);
            }
        }
        while (mailbox != null);
    }

    @PreDestroy
    void stop() {
        if (executor == null) {
            return;
        }

        try {
            executor.shutdown();
            if (executor.awaitTermination(90, TimeUnit.SECONDS)) {
                log.info("Shutdown Postman executor successfully!");
            }
            else {
                log.warn("Shutdown Postman executor timeout: 90s");
            }
        }
        catch (Exception e) {
            log.warn("Cannot shutdown Postman executor: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void deliver(@Nonnull Mailbox mailbox) {
        val tags = mailbox.getPostman();
        val msgId = mailbox.getMsgId();
        val service = mailbox.getService();
        val title = mailbox.getTitle();

        try {
            val content = mailbox.getContent();
            val postman = applicationContext.getBean(tags + "Postman", Postman.class);
            val mail = postman.encapsulate(msgId, tags, content);
            mail.setPostman(tags);
            mail.setService(service);
            mail.setTitle(title);
            mail.setId(msgId);
            mail.setRecipients(mailbox.getRecipients());
            if (postman.deliver(mail)) {
                mailboxService.markSuccess(mailbox);
            }
            else {
                try {
                    mailboxService.markFailure(mailbox, "postman(" + tags + ") cannot deliver it.");
                }
                catch (Exception e) {
                    log.warn("Cannot mark mailbox status fail: postman({}) - msgId({}) - {}", tags, msgId,
                            ExceptionUtil.getSimpleMessage(ExceptionUtil.getRootCause(e)));
                }
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
