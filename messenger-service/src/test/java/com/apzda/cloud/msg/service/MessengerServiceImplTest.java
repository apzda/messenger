package com.apzda.cloud.msg.service;

import com.apzda.cloud.gsvc.autoconfigure.MyBatisPlusAutoConfiguration;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.msg.config.MessengerServiceProperties;
import com.apzda.cloud.msg.proto.DeliveryQuery;
import com.apzda.cloud.msg.proto.MailboxQuery;
import com.apzda.cloud.msg.proto.MessengerService;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.google.protobuf.Empty;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
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
@ContextConfiguration(classes = MessengerServiceImplTest.class)
@AutoConfigureGsvcTest
@ImportAutoConfiguration(MyBatisPlusAutoConfiguration.class)
@ComponentScan({ "com.apzda.cloud.msg.domain.service", "com.apzda.cloud.msg.service", "com.apzda.cloud.msg.converter" })
@MapperScan("com.apzda.cloud.msg.domain.mapper")
@EnableConfigurationProperties({ MessengerServiceProperties.class, ServiceConfigProperties.class })
@Sql(value = "classpath:/mailbox.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class MessengerServiceImplTest {

    @Autowired
    private MessengerService messengerService;

    @Test
    void query_desc() {
        // given
        val pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Order.desc("msg_id")));
        val pager = PagerUtils.to(pageRequest);
        val request = MailboxQuery.newBuilder().setPager(pager).build();

        // when
        val res = messengerService.query(request);
        // then
        assertThat(res).isNotNull();
        assertThat(res.getErrCode()).isEqualTo(0);
        assertThat(res.getResultCount()).isEqualTo(2);
        assertThat(res.getResult(0).getMsgId()).isEqualTo("3");
        assertThat(res.getResult(1).getMsgId()).isEqualTo("2");
        assertThat(res.getPager().getTotalElements()).isEqualTo(3);
        assertThat(res.getPager().getTotalPages()).isEqualTo(2);
        assertThat(res.getPager().getNumberOfElements()).isEqualTo(2);
        assertThat(res.getPager().getPageNumber()).isEqualTo(0);
        assertThat(res.getPager().getPageSize()).isEqualTo(2);
        assertThat(res.getPager().getFirst()).isTrue();
        assertThat(res.getPager().getLast()).isFalse();
    }

    @Test
    void query_asc() {
        // given
        val pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Order.asc("msg_id")));
        val pager = PagerUtils.to(pageRequest);
        val request = MailboxQuery.newBuilder().setPager(pager).build();

        // when
        val res = messengerService.query(request);
        // then
        assertThat(res).isNotNull();
        assertThat(res.getErrCode()).isEqualTo(0);
        assertThat(res.getResultCount()).isEqualTo(1);
        assertThat(res.getResult(0).getMsgId()).isEqualTo("3");
        assertThat(res.getPager().getTotalElements()).isEqualTo(3);
        assertThat(res.getPager().getTotalPages()).isEqualTo(2);
        assertThat(res.getPager().getNumberOfElements()).isEqualTo(1);
        assertThat(res.getPager().getPageNumber()).isEqualTo(1);
        assertThat(res.getPager().getPageSize()).isEqualTo(2);
        assertThat(res.getPager().getFirst()).isFalse();
        assertThat(res.getPager().getLast()).isTrue();
    }

    @Test
    @Sql("classpath:/delivery.sql")
    void delivery() {
        val pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Order.asc("id")));
        val pager = PagerUtils.to(pageRequest);
        val request = DeliveryQuery.newBuilder().setMailboxId(1).setStatus("SENT").setPager(pager).build();

        // when
        val res = messengerService.delivery(request);
        // then
        assertThat(res).isNotNull();
        assertThat(res.getErrCode()).isEqualTo(0);
        assertThat(res.getResultCount()).isEqualTo(1);
    }

    @Test
    void dict() {
        // when
        val res = messengerService.dict(Empty.newBuilder().build());
        // then
        assertThat(res).isNotNull();
        assertThat(res.getErrCode()).isEqualTo(0);
        assertThat(res.getDictCount()).isEqualTo(5);
        assertThat(res.getDictList())
            .contains(GsvcExt.KeyValue.newBuilder().setKey("PENDING").setValue("PENDING").build());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "skip.container", havingValue = "no", matchIfMissing = true)
    static class TestConfig {

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysql() {
            return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.35")).withDatabaseName("demo_db")
                .withUsername("root")
                .withPassword("Abc12332!")
                .withStartupTimeout(Duration.ofMinutes(3));
        }

    }

}
