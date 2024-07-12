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

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class MessengerTransactionListener implements TransactionListener {

    private final IMailboxTransService mailboxService;

    @Override
    public LocalTransactionState executeLocalTransaction(@Nonnull Message message, Object o) {
        val mail = (MailboxTrans) o;
        mail.setStatus(MailStatus.SENT);
        mail.setTransId(message.getTransactionId());

        if (mailboxService.updateStatus(mail, MailStatus.SENDING)) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        return LocalTransactionState.UNKNOW;
    }

    @Override
    public LocalTransactionState checkLocalTransaction(@Nonnull MessageExt message) {
        if (mailboxService.removeByTransId(message.getTransactionId())) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        else {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

}
