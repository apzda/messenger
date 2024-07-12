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
import com.apzda.cloud.msg.config.PostmanConfigProperties;
import com.apzda.cloud.msg.domain.entity.MailboxTrans;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.apzda.cloud.msg.listener.MessengerTransactionListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.support.RocketMQUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
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

    private final MessengerClientProperties properties;

    private final PostmanConfigProperties postmanConfig;

    private final String topic;

    private final ScheduledThreadPoolExecutor executor;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public MessengerImpl(MessengerClientProperties properties, PostmanConfigProperties postmanConfig,
            RocketMQProperties mqProperties, IMailboxTransService mailboxService) throws MQClientException {
        this.properties = properties;
        this.postmanConfig = postmanConfig;
        this.mailboxService = mailboxService;
        this.producer = createTransactionMQProducer(properties, mqProperties);
        this.producer.setTransactionListener(new MessengerTransactionListener(mailboxService));
        this.topic = properties.getTopic();
        Assert.hasText(topic, "[apzda.cloud.messenger.producer.topic] must not be null");
        this.producer.start();
        val executorCount = postmanConfig.getExecutorCount();
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
        val executorCount = executor.getCorePoolSize();
        val delay = postmanConfig.getDelay().toSeconds();
        val period = postmanConfig.getPeriod().toSeconds();
        for (int i = 0; i < executorCount; i++) {
            executor.scheduleAtFixedRate(new MailSender(producer, mailboxService, topic, postmanConfig),
                    delay > 0 ? delay : 10, period > 0 ? period : 1, TimeUnit.SECONDS);
        }
        log.info("Messenger executor init: count={}, delay={}, period={} ", executorCount, delay, period);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void send(@Nonnull Mail<?> mail) {
        val postman = mail.getPostman();
        Assert.hasText(postman, "postman must not be null");
        val content = mail.getContent();
        Assert.hasText(content, "content must not be null");
        val mailbox = new MailboxTrans();
        mailbox.setPostman(postman);
        mailbox.setMailId(mail.getId());
        mailbox.setContent(content);
        mailbox.setService(mail.getService());
        mailbox.setTitle(mail.getTitle());

        if (!mailboxService.save(mailbox)) {
            throw new RuntimeException("the mail cannot save into mailbox" + mail);
        }
    }

    @PreDestroy
    void stop() {
        val nameServer = this.producer.getNamesrvAddr();
        val groupName = this.producer.getProducerGroup();
        try {
            this.producer.shutdown();
            log.info("a producer used by Messenger ({}) disconnect from namesrv {}", groupName, nameServer);
        }
        catch (Exception e) {
            log.warn("a producer used by Messenger ({}) did not disconnect from namesrv {} correctly", groupName,
                    nameServer);
        }

        try {
            executor.shutdown();
            if (executor.awaitTermination(90, TimeUnit.SECONDS)) {
                log.info("Messenger executor shutdown!");
            }
            else {
                log.warn("Shutdown executor timeout: 90s");
            }
        }
        catch (Exception e) {
            log.warn("cannot shutdown executor: {}", e.getMessage());
        }
    }

    @Nonnull
    private static TransactionMQProducer createTransactionMQProducer(MessengerClientProperties properties,
            @Nonnull RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        String groupName = defaultIfBlank(properties.getGroup(), producerConfig.getGroup());
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[apzda.cloud.messenger.producer.group] must not be null");

        String accessChannel = rocketMQProperties.getAccessChannel();

        String ak = producerConfig.getAccessKey();
        String sk = producerConfig.getSecretKey();
        boolean isEnableMsgTrace = producerConfig.isEnableMsgTrace();
        String customizedTraceTopic = producerConfig.getCustomizedTraceTopic();

        TransactionMQProducer producer = (TransactionMQProducer) RocketMQUtil.createDefaultMQProducer(groupName, ak, sk,
                isEnableMsgTrace, customizedTraceTopic);

        producer.setNamesrvAddr(nameServer);
        if (StringUtils.hasLength(accessChannel)) {
            producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        producer.setSendMsgTimeout(producerConfig.getSendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMessageBodyThreshold());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryNextServer());
        producer.setUseTLS(producerConfig.isTlsEnable());
        val namespaces = defaultIfBlank(properties.getNamespace(), producerConfig.getNamespace());
        if (StringUtils.hasText(namespaces)) {
            producer.setNamespace(namespaces);
        }
        producer.setInstanceName(defaultIfBlank(properties.getInstanceName(), producerConfig.getInstanceName()));
        log.info("a producer used by Messenger ({}) init on namesrv {}", groupName, nameServer);
        return producer;
    }

    @Slf4j
    private record MailSender(TransactionMQProducer producer, IMailboxTransService mailboxService, String topic,
            PostmanConfigProperties postmanConfig) implements Runnable {

        @Override
        public void run() {
            MailboxTrans trans;
            do {
                trans = mailboxService.getByStatus(MailStatus.PENDING);
                if (trans != null) {
                    try {
                        val postman = trans.getPostman();
                        Assert.hasText(postman, "postman must not be null");
                        val content = trans.getContent();
                        Assert.hasText(content, "content must not be null");
                        val message = new Message(topic, postman, content.getBytes(StandardCharsets.UTF_8));
                        message.putUserProperty("msgId", trans.getMailId());
                        message.putUserProperty("title", trans.getTitle());
                        message.putUserProperty("service", trans.getService());

                        trans.setStatus(MailStatus.SENDING);
                        if (!mailboxService.updateStatus(trans, MailStatus.PENDING)) {
                            continue;
                        }

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
                        trans.setStatus(MailStatus.PENDING);
                        trans.setRemark(message);
                        mailboxService.updateStatus(trans, MailStatus.SENDING);
                        log.warn("Cannot send mail: {} - {}", trans, message);
                    }
                }
            }
            while (trans != null);
        }
    }

}
