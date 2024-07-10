package com.apzda.cloud.msg.client;

import com.apzda.cloud.msg.Mail;
import com.apzda.cloud.msg.Messenger;
import com.apzda.cloud.msg.autoconfig.MessengerClientAutoConfiguration;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import lombok.val;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

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

    @Test
    void shouldOk() {
        // given
        val test = new Mail("1", "test", "aaaa");

        messenger.send(test);
    }

}
