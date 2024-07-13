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
package com.apzda.cloud.msg.domain.service.impl;

import com.apzda.cloud.msg.config.MessengerServiceProperties;
import com.apzda.cloud.msg.domain.entity.Delivery;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.domain.mapper.DeliveryMapper;
import com.apzda.cloud.msg.domain.mapper.MailboxMapper;
import com.apzda.cloud.msg.domain.service.IMailboxService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl extends ServiceImpl<MailboxMapper, Mailbox> implements IMailboxService {

    private final Clock clock;

    private final TransactionTemplate transactionTemplate;

    private final MailboxMapper mailboxMapper;

    private final DeliveryMapper deliveryMapper;

    private final MessengerServiceProperties properties;

    @Override
    public Mailbox getByPostmanAndMsgId(String postman, String msgId) {
        return mailboxMapper.getByPostmanAndMsgId(postman, msgId);
    }

    @Override
    public Mailbox getByMsgId(String msgId) {
        return mailboxMapper.getByMsgId(msgId);
    }

    @Override
    public Mailbox getByStatusAndNextRetryAtLe(MailStatus mailStatus, long nextRetryAt) {
        val con = Wrappers.lambdaQuery(Mailbox.class);
        con.eq(Mailbox::getStatus, mailStatus);
        con.le(Mailbox::getNextRetryAt, nextRetryAt);
        con.orderByAsc(Mailbox::getNextRetryAt);
        con.last("LIMIT 1");

        return getOne(con);
    }

    @Override
    public void markSuccess(Mailbox mailbox) {
        transactionTemplate.execute(status -> {
            mailbox.setDeliveredAt(clock.millis());
            mailbox.setStatus(MailStatus.SENT);
            mailbox.setRemark("");
            if (updateById(mailbox)) {
                val deliver = new Delivery();
                deliver.setDeliveredAt(mailbox.getDeliveredAt());
                deliver.setMailboxId(mailbox.getId());
                deliver.setStatus(MailStatus.SENT);
                deliveryMapper.insert(deliver);
            }
            else {
                status.setRollbackOnly();
                throw new IllegalStateException("Cannot update mailbox status to SENT: " + mailbox.getMsgId());
            }
            return true;
        });
    }

    @Override

    public void markFailure(Mailbox mailbox, String error) {
        transactionTemplate.execute(status -> {
            mailbox.setDeliveredAt(clock.millis());
            mailbox.setRemark(error);
            // retries
            val retries = properties.getRetries();
            val currentRetry = mailbox.getRetries();
            if (retries.size() >= currentRetry + 1) {
                val duration = retries.get(currentRetry);
                mailbox.setRetries(currentRetry + 1);
                mailbox.setNextRetryAt(mailbox.getDeliveredAt() + duration.toMillis());
                mailbox.setStatus(MailStatus.RETRYING);
            }
            else {
                mailbox.setStatus(MailStatus.FAIL);
            }

            if (updateById(mailbox)) {
                val deliver = new Delivery();
                deliver.setDeliveredAt(mailbox.getDeliveredAt());
                deliver.setMailboxId(mailbox.getId());
                deliver.setStatus(MailStatus.FAIL);
                deliver.setRetries(currentRetry);
                deliver.setRemark(error);
                deliveryMapper.insert(deliver);
            }
            else {
                status.setRollbackOnly();
                throw new IllegalStateException("Cannot update mailbox status to FAIL: " + mailbox.getMsgId());
            }
            return true;
        });

    }

    @Override
    public boolean updateStatus(Mailbox mailbox, MailStatus fromStatus) {
        val con = Wrappers.lambdaUpdate(Mailbox.class);
        con.eq(Mailbox::getStatus, fromStatus);
        con.eq(Mailbox::getId, mailbox.getId());

        return update(mailbox, con);
    }

    @Override
    public boolean resend(Mailbox mailbox) {
        val status = mailbox.getStatus();
        mailbox.setStatus(MailStatus.PENDING);
        mailbox.setNextRetryAt(clock.millis());
        mailbox.setRetries(0);
        mailbox.setRemark("");

        return updateStatus(mailbox, status);
    }

    @Override
    public IPage<Delivery> deliveries(IPage<Delivery> page, Wrapper<Delivery> wrapper) {
        return deliveryMapper.selectPage(page, wrapper);
    }

}
