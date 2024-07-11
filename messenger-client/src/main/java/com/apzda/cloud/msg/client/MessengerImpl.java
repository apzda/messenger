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

import com.apzda.cloud.msg.Mail;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.config.MessengerClientProperties;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.listener.MessengerTransactionListener;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.support.RocketMQUtil;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class MessengerImpl implements Messenger {

    private final TransactionMQProducer producer;

    private final String topic;

    public MessengerImpl(MessengerClientProperties properties, RocketMQProperties mqProperties,
            IMailboxTransService mailboxService, Duration transactionTimeout) throws MQClientException {
        this.producer = createTransactionMQProducer(properties, mqProperties);
        this.producer
            .setTransactionListener(new MessengerTransactionListener(mailboxService, transactionTimeout.toMillis()));
        this.topic = properties.getTopic();
        Assert.hasText(topic, "[apzda.cloud.messenger.topic] must not be null");
        this.producer.start();
    }

    @Override
    public void send(Mail mail) {
        send(mail, -1);
    }

    @Override
    public void send(Mail mail, int timeout) {
        try {
            val sender = mail.getSender();
            Assert.hasText(sender, "sender must not be null");
            val content = mail.getContent();
            Assert.hasText(content, "content must not be null");
            val message = new Message(topic, sender, content.getBytes(StandardCharsets.UTF_8));
            if (timeout >= 0) {
                message.putUserProperty("timeout", String.valueOf(timeout));
            }
            val result = this.producer.sendMessageInTransaction(message, mail);
            if (result == null) {
                throw new RuntimeException("Can't send mail: " + mail);
            }
            else if (SEND_OK != result.getSendStatus()) {
                throw new RuntimeException(result.getSendStatus().name());
            }
            log.error("Mail sent successfully: {}", mail);
        }
        catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void stop() {
        try {
            val nameServer = this.producer.getNamesrvAddr();
            val groupName = this.producer.getProducerGroup();
            this.producer.shutdown();
            log.info("a producer used by Messenger ({}) disconnect from namesrv {}", groupName, nameServer);
        }
        catch (Exception ignore) {
        }
    }

    private static TransactionMQProducer createTransactionMQProducer(MessengerClientProperties properties,
            RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer producerConfig = properties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        val defaultProducerCfg = rocketMQProperties.getProducer();

        String groupName = defaultIfBlank(producerConfig.getGroup(), defaultProducerCfg.getGroup());
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[apzda.cloud.messenger.producer.group] must not be null");

        String accessChannel = rocketMQProperties.getAccessChannel();

        String ak = defaultIfBlank(producerConfig.getAccessKey(), defaultProducerCfg.getAccessKey());
        String sk = defaultIfBlank(producerConfig.getSecretKey(), defaultProducerCfg.getSecretKey());
        boolean isEnableMsgTrace = producerConfig.isEnableMsgTrace();
        String customizedTraceTopic = defaultIfBlank(producerConfig.getCustomizedTraceTopic(),
                defaultProducerCfg.getCustomizedTraceTopic());

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
        val namespaces = defaultIfBlank(producerConfig.getNamespace(), defaultProducerCfg.getNamespace());
        if (StringUtils.hasText(namespaces)) {
            producer.setNamespace(namespaces);
        }
        producer
            .setInstanceName(defaultIfBlank(producerConfig.getInstanceName(), defaultProducerCfg.getInstanceName()));
        log.info("a producer used by Messenger ({}) init on namesrv {}", groupName, nameServer);
        return producer;
    }

}
