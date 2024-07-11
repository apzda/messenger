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
package com.apzda.cloud.msg.listener;

import com.apzda.cloud.msg.Mail;
import com.apzda.cloud.msg.domain.entity.MailboxTrans;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class MessengerTransactionListener implements TransactionListener {

    private final IMailboxTransService mailboxService;

    private final long defaultTransactionTimeout;

    @Override
    public LocalTransactionState executeLocalTransaction(@Nonnull Message message, Object o) {
        val mail = (Mail) o;
        val transactionId = message.getTransactionId();
        val transId = DigestUtils.md5DigestAsHex(transactionId.getBytes(StandardCharsets.UTF_8));

        log.trace("the Mail({}) sent in transaction: {} - {}", mail.getId(), transactionId, transId);

        val sender = mail.getSender();
        val content = mail.getContent();
        val mailbox = new MailboxTrans();
        mailbox.setId(transId);
        mailbox.setSender(sender);
        mailbox.setMailId(mail.getId());
        mailbox.setStatus(MailStatus.SENT);
        mailbox.setContent(content);

        if (!mailboxService.save(mailbox)) {
            throw new RuntimeException(
                    "the mail cannot save into mailbox, transaction '" + transactionId + "' will rollback: " + mail);
        }

        log.trace("the Mail saved into mailbox： {}", mail);
        // 50秒后会调用checkLocalTransaction
        return LocalTransactionState.UNKNOW;
    }

    @Override
    public LocalTransactionState checkLocalTransaction(@Nonnull MessageExt message) {
        val transactionId = message.getTransactionId();
        val transId = DigestUtils.md5DigestAsHex(transactionId.getBytes(StandardCharsets.UTF_8));
        log.trace("checkLocalTransaction: {} - {} - {}", message.getReconsumeTimes(), transactionId, transId);
        val trans = mailboxService.getById(transId);
        val escaped = System.currentTimeMillis() - message.getBornTimestamp();
        val timeoutStr = message.getUserProperty("timeout");
        var timeout = defaultTransactionTimeout;
        if (StringUtils.hasText(timeoutStr) && !ObjectUtils.nullSafeEquals("-1", timeoutStr)) {
            try {
                timeout = Integer.parseInt(timeoutStr);
            }
            catch (Exception ignore) {
            }
        }
        timeout = timeout > 0 ? timeout + 1 : timeout + 61;

        val maxEscaped = Duration.ofSeconds(timeout).toMillis();

        if (escaped < maxEscaped) {
            return LocalTransactionState.UNKNOW;
        }

        if (trans == null) {
            log.trace("The transaction was rolled back");
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }

        try {
            mailboxService.removeById(transactionId);
        }
        catch (Exception ignore) {
        }

        return LocalTransactionState.COMMIT_MESSAGE;
    }

}
