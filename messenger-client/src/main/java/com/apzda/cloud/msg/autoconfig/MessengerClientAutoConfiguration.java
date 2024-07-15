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
import com.apzda.cloud.gsvc.i18n.MessageSourceNameResolver;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.client.MessengerImpl;
import com.apzda.cloud.msg.config.MessengerClientProperties;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.listener.MessengerTransactionListener;
import com.apzda.cloud.msg.proto.MessengerService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.support.RocketMQUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Clock;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@AutoConfiguration(after = RocketMQAutoConfiguration.class)
@EnableGsvcServices(MessengerService.class)
@ComponentScan({ "com.apzda.cloud.msg.domain.service" })
@MapperScan("com.apzda.cloud.msg.domain.mapper")
@ConditionalOnClass(RocketMQAutoConfiguration.class)
@EnableConfigurationProperties({ MessengerClientProperties.class })
@Slf4j
public class MessengerClientAutoConfiguration {

    @Bean
    Messenger messengerImpl(MessengerClientProperties properties, IMailboxTransService mailboxService, Clock clock,
            ObjectProvider<TransactionMQProducer> provider) throws MQClientException {
        return new MessengerImpl(properties, provider, mailboxService, clock);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "apzda.cloud.messenger", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    @SuppressWarnings("deprecation")
    @Primary
    TransactionMQProducer messengerMqProducer(MessengerClientProperties properties,
            RocketMQProperties rocketMQProperties, IMailboxTransService mailboxService) throws MQClientException {
        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        String groupName = defaultIfBlank(properties.getGroup(), producerConfig.getGroup());
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[apzda.cloud.messenger.group] must not be null");

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
        producer.setTransactionListener(new MessengerTransactionListener(mailboxService));
        producer.start();
        log.info("a producer used by Messenger ({}) init on namesrv {}", groupName, nameServer);
        return producer;
    }

    @Bean("messenger.MessageSourceNameResolver")
    MessageSourceNameResolver messageSourceNameResolver() {
        return () -> "messenger";
    }

}
