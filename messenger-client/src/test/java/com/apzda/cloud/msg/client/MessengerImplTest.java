package com.apzda.cloud.msg.client;

import cn.hutool.core.util.RandomUtil;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.TextMail;
import com.apzda.cloud.msg.autoconfig.MessengerClientAutoConfiguration;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import lombok.val;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@MybatisPlusTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = MessengerImplTest.class)
@AutoConfigureGsvcTest
@ImportAutoConfiguration({ RocketMQAutoConfiguration.class, MessengerClientAutoConfiguration.class })
class MessengerImplTest {

    @Autowired
    private Messenger messenger;

    @Autowired
    private IMailboxTransService mailboxTransService;

    @Test
    @Commit
    void mail_should_be_sent_ok() {
        String id = RandomUtil.randomString(32);
        var trans = mailboxTransService.listByMailId(id);
        assertThat(trans).isNotNull();

        val mail = new TextMail(id, "rocketmq", "test");
        mail.setService("test");
        mail.setTitle(RandomUtil.randomString(18));
        mail.setRecipients("test:demo");
        // when
        messenger.send(mail);
        trans = mailboxTransService.listByMailId(id);
        // then
        assertThat(trans).isNotEmpty();
        assertThat(trans.size()).isEqualTo(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "skip.container", havingValue = "no", matchIfMissing = true)
    static class TestConfig {

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysql() {
            return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.35"))
                .withDatabaseName("demo_db")
                .withUsername("root")
                .withPassword("Abc12332!")
                .withStartupTimeout(Duration.ofMinutes(3));
        }

    }

}
