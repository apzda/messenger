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

import com.apzda.cloud.msg.domain.entity.MailboxTrans;
import com.apzda.cloud.msg.domain.mapper.MailboxTransMapper;
import com.apzda.cloud.msg.domain.service.IMailboxTransService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class MailboxTransServiceImpl extends ServiceImpl<MailboxTransMapper, MailboxTrans>
        implements IMailboxTransService {

    @Override
    public MailboxTrans getByStatusAndNextRetryAtLe(MailStatus mailStatus, long nextRetryAt) {
        val con = Wrappers.lambdaQuery(MailboxTrans.class);
        con.eq(MailboxTrans::getStatus, mailStatus);
        con.le(MailboxTrans::getNextRetryAt, nextRetryAt);
        con.orderByAsc(MailboxTrans::getNextRetryAt);
        con.last("LIMIT 1");

        return getOne(con);
    }

    @Override
    public boolean updateStatus(MailboxTrans mailboxTrans, MailStatus fromStatus) {
        val con = Wrappers.lambdaUpdate(MailboxTrans.class);
        con.eq(MailboxTrans::getStatus, fromStatus);
        con.eq(MailboxTrans::getId, mailboxTrans.getId());

        return update(mailboxTrans, con);
    }

    @Nonnull
    @Override
    public List<MailboxTrans> listByMailId(String id) {
        val con = Wrappers.lambdaQuery(MailboxTrans.class);
        con.eq(MailboxTrans::getMailId, id);
        con.orderByAsc(MailboxTrans::getCreatedAt);
        return list(con);
    }

    @Override
    public boolean removeByTransId(String transId) {
        val con = Wrappers.lambdaQuery(MailboxTrans.class);
        con.eq(MailboxTrans::getTransId, transId);

        return remove(con);
    }

}
