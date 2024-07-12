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

import com.apzda.cloud.msg.domain.entity.Deliver;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.domain.mapper.DeliverMapper;
import com.apzda.cloud.msg.domain.mapper.MailboxMapper;
import com.apzda.cloud.msg.domain.service.IMailboxService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl extends ServiceImpl<MailboxMapper, Mailbox> implements IMailboxService {

    private final MailboxMapper mailboxMapper;

    private final DeliverMapper deliverMapper;

    @Override
    public Mailbox getByPostmanAndMsgId(String postman, String msgId) {
        return mailboxMapper.getByPostmanAndMsgId(postman, msgId);
    }

    @Override
    @Transactional
    public void markSuccess(Mailbox mailbox) {
        mailbox.setDeliveredAt(System.currentTimeMillis());
        mailbox.setStatus(MailStatus.SENT);
        mailbox.setRemark("");
        if (updateById(mailbox)) {
            val deliver = new Deliver();
            deliver.setDeliveredAt(mailbox.getDeliveredAt());
            deliver.setMailboxId(mailbox.getId());
            deliver.setStatus(MailStatus.SENT);
            deliverMapper.insert(deliver);
        }
        else {
            throw new IllegalStateException("Cannot update mailbox status to SENT: " + mailbox.getMsgId());
        }
    }

    @Override
    @Transactional
    public void markFailure(Mailbox mailbox, String error) {
        mailbox.setDeliveredAt(System.currentTimeMillis());
        mailbox.setStatus(MailStatus.FAIL);
        mailbox.setRemark(error);
        if (updateById(mailbox)) {
            val deliver = new Deliver();
            deliver.setDeliveredAt(mailbox.getDeliveredAt());
            deliver.setMailboxId(mailbox.getId());
            deliver.setStatus(MailStatus.FAIL);
            deliver.setRemark(error);
            deliverMapper.insert(deliver);
        }
        else {
            throw new IllegalStateException("Cannot update mailbox status to FAIL: " + mailbox.getMsgId());
        }
    }

}
