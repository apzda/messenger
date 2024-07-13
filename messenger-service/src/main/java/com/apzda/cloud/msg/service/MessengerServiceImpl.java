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
package com.apzda.cloud.msg.service;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.msg.converter.MailboxConverter;
import com.apzda.cloud.msg.domain.entity.Delivery;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.domain.service.IMailboxService;
import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.apzda.cloud.msg.proto.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.apzda.cloud.msg.ErrorCode.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class MessengerServiceImpl implements MessengerService {

    private final IMailboxService mailboxService;

    private final MailboxConverter mailboxConverter;

    @Override
    public GsvcExt.CommonRes resend(ReSendReq request) {
        val res = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        val msgId = request.getMsgId();
        if (StringUtils.isBlank(msgId)) {
            res.setErrCode(MSG_ID_REQUIRED);
            return res.build();
        }

        val mailbox = mailboxService.getByMsgId(msgId);
        if (mailbox == null) {
            res.setErrCode(MSG_NOT_FOUND);
            return res.build();
        }

        if (!mailboxService.resend(mailbox)) {
            res.setErrCode(MSG_CANNOT_RESEND);
            return res.build();
        }

        return res.build();
    }

    @Override
    public MailboxQueryResult query(MailboxQuery request) {
        val res = MailboxQueryResult.newBuilder();
        val pager = request.getPager();
        val query = Wrappers.lambdaQuery(Mailbox.class);
        if (request.hasMsgId()) {
            query.eq(Mailbox::getMsgId, request.getMsgId());
        }
        if (request.hasPostman()) {
            query.eq(Mailbox::getPostman, request.getPostman());
        }
        if (request.hasService()) {
            query.eq(Mailbox::getService, request.getService());
        }
        if (request.hasStatus()) {
            query.eq(Mailbox::getStatus, request.getStatus());
        }
        if (request.hasStartTime()) {
            query.ge(Mailbox::getCreatedAt, request.getStartTime());
        }
        if (request.hasEndTime()) {
            query.lt(Mailbox::getCreatedAt, request.getEndTime());
        }

        val page = PagerUtils.of(pager, Mailbox.class);
        val result = mailboxService.page(page, query);

        res.setPager(PagerUtils.of(result));
        res.addAllResult(mailboxConverter.toMailboxVos(result.getRecords()));

        return res.build();
    }

    @Override
    public DeliveryQueryResult delivery(DeliveryQuery request) {
        val res = DeliveryQueryResult.newBuilder();
        // 查询条件
        val query = Wrappers.lambdaQuery(Delivery.class);
        query.eq(Delivery::getMailboxId, request.getMailboxId());
        if (request.hasStatus()) {
            query.eq(Delivery::getStatus, request.getStatus());
        }
        // 查询分页
        val pager = request.getPager();
        val page = PagerUtils.of(pager, Delivery.class);

        // 查询结果
        val result = mailboxService.deliveries(page, query);

        // 响应
        res.setPager(PagerUtils.of(result));
        res.addAllResult(mailboxConverter.toDeliveryVos(result.getRecords()));

        return res.build();
    }

    @Override
    public DictRes dict(Empty request) {
        val builder = DictRes.newBuilder();
        for (String name : EnumUtil.getNames(MailStatus.class)) {
            builder.addDict(GsvcExt.KeyValue.newBuilder()
                .setKey(name)
                .setValue(I18nUtils.t(StrUtil.format("messenger.status.{}", name.toLowerCase()), name)));
        }
        return builder.build();
    }

}
