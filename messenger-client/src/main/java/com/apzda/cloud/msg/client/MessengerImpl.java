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
package com.apzda.cloud.msg.client;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.apzda.cloud.msg.Mail;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.config.MessengerClientProperties;
import com.apzda.cloud.msg.domain.entity.MailboxTrans;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class MessengerImpl implements Messenger, InitializingBean {

    private final TransactionMQProducer producer;

    private final IMailboxTransService mailboxService;

    private final Clock clock;

    private final MessengerClientProperties properties;

    private final String topic;

    private final ScheduledThreadPoolExecutor executor;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public MessengerImpl(MessengerClientProperties properties, ObjectProvider<TransactionMQProducer> provider,
            IMailboxTransService mailboxService, Clock clock) throws MQClientException {
        this.properties = properties;
        this.mailboxService = mailboxService;
        this.clock = clock;
        this.producer = provider.getIfAvailable();
        this.topic = properties.getTopic();
        Assert.hasText(topic, "[apzda.cloud.messenger.producer.topic] must not be null");
        val executorCount = properties.getExecutorCount();
        executor = new ScheduledThreadPoolExecutor(
                executorCount < 1 ? Math.max(1, Runtime.getRuntime().availableProcessors() / 4) : executorCount, r -> {
                    val thread = new Thread(r);
                    thread.setName("messenger-" + atomicInteger.getAndAdd(1));
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.producer != null) {
            val executorCount = executor.getCorePoolSize();
            val delay = properties.getDelay().toSeconds();
            val period = properties.getPeriod().toSeconds();
            for (int i = 0; i < executorCount; i++) {
                executor.scheduleAtFixedRate(new MailSender(producer, mailboxService, topic, properties, clock),
                        delay > 0 ? delay : 10, period > 0 ? period : 1, TimeUnit.SECONDS);
            }
            log.info("Messenger executor init: count={}, delay={}, period={} ", executorCount, delay, period);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void send(@Nonnull Mail<?> mail) {
        val postman = mail.getPostman();
        Assert.hasText(postman, "postman must not be null");
        val content = mail.getContent();
        Assert.hasText(content, "content must not be null");
        val recipients = mail.getRecipients();
        Assert.hasText(content, "recipients must not be null");

        val mailbox = new MailboxTrans();
        mailbox.setPostman(postman);
        mailbox.setMailId(mail.getId());
        mailbox.setContent(content);
        mailbox.setService(mail.getService());
        mailbox.setTitle(mail.getTitle());
        mailbox.setNextRetryAt(clock.millis());
        mailbox.setRetries(0);
        mailbox.setPostTime(mailbox.getNextRetryAt());
        mailbox.setRecipients(recipients);

        if (!mailboxService.save(mailbox)) {
            throw new RuntimeException("The mail cannot save into mailbox: " + mail);
        }
    }

    @PreDestroy
    void stop() {
        try {
            executor.shutdown();
            if (executor.awaitTermination(90, TimeUnit.SECONDS)) {
                log.info("Shutdown Messenger executor successfully!");
            }
            else {
                log.warn("Shutdown Messenger executor timeout: 90s");
            }
        }
        catch (Exception e) {
            log.warn("Cannot shutdown Messenger executor: {}", e.getMessage());
        }
    }

    @Slf4j
    private record MailSender(TransactionMQProducer producer, IMailboxTransService mailboxService, String topic,
            MessengerClientProperties postmanConfig, Clock clock) implements Runnable {

        @Override
        public void run() {
            MailboxTrans trans;
            do {
                trans = mailboxService.getByStatusAndNextRetryAtLe(MailStatus.PENDING, clock.millis());
                if (trans != null) {
                    try {
                        trans.setStatus(MailStatus.SENDING);
                        if (!mailboxService.updateStatus(trans, MailStatus.PENDING)) {
                            continue;
                        }

                        val postman = trans.getPostman();
                        Assert.hasText(postman, "postman must not be null");
                        val content = trans.getContent();
                        Assert.hasText(content, "content must not be null");
                        val message = createMessage(topic, postman, content, trans);
                        val result = this.producer.sendMessageInTransaction(message, trans);
                        if (result == null) {
                            throw new RuntimeException("Can't send mail: " + trans);
                        }
                        else if (SEND_OK != result.getSendStatus()) {
                            throw new RuntimeException(
                                    "Can't send mail with status(" + result.getSendStatus() + "): " + trans);
                        }
                    }
                    catch (Exception e) {
                        val message = ExceptionUtil.getSimpleMessage(ExceptionUtil.getRootCause(e));
                        val retries = postmanConfig.getRetries();
                        val currentRetry = trans.getRetries();
                        if (retries.size() >= (currentRetry + 1)) {
                            trans.setStatus(MailStatus.PENDING);
                            trans.setRetries(currentRetry + 1);
                            val duration = retries.get(currentRetry);
                            trans.setNextRetryAt(trans.getNextRetryAt() + duration.toMillis());
                        }
                        else {
                            trans.setStatus(MailStatus.FAIL);
                        }
                        trans.setRemark(message);
                        mailboxService.updateStatus(trans, MailStatus.SENDING);
                        log.warn("Cannot send mail: {} - {}", trans, message);
                    }
                }
            }
            while (trans != null);
        }
    }

    @Nonnull
    private static Message createMessage(String topic, String postman, String content, MailboxTrans trans) {
        val message = new Message(topic, postman, content.getBytes(StandardCharsets.UTF_8));
        message.putUserProperty("msgId", trans.getMailId());
        message.putUserProperty("title", trans.getTitle());
        message.putUserProperty("service", trans.getService());
        message.putUserProperty("recipients", trans.getRecipients());
        message.putUserProperty("postTime", String.valueOf(trans.getPostTime()));
        return message;
    }

}
