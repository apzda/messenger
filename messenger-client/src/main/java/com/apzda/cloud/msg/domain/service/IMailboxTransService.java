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

import com.apzda.cloud.msg.domain.entity.MailboxTrans;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface IMailboxTransService extends IService<MailboxTrans> {

    MailboxTrans getByStatus(MailStatus mailStatus);

    boolean updateStatus(MailboxTrans mailboxTrans, MailStatus fromStatus);

    boolean resetStatusByTransId(String transId, MailStatus toStatus);

    @Nonnull
    List<MailboxTrans> listByMailId(String id);

    boolean removeByTransId(String transactionId);

}
