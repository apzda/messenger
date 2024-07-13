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
package com.apzda.cloud.msg.domain.service;

import com.apzda.cloud.msg.domain.entity.Delivery;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface IMailboxService extends IService<Mailbox> {

    Mailbox getByPostmanAndMsgId(String postman, String msgId);

    Mailbox getByMsgId(String msgId);

    Mailbox getByStatusAndNextRetryAtLe(MailStatus mailStatus, long nextRetryAt);

    void markSuccess(Mailbox mailbox);

    void markFailure(Mailbox mailbox, String error);

    boolean updateStatus(Mailbox mailbox, MailStatus fromStatus);

    boolean resend(Mailbox mailbox);

    IPage<Delivery> deliveries(IPage<Delivery> page, Wrapper<Delivery> wrapper);

}
