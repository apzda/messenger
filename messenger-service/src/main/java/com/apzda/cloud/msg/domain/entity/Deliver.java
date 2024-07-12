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
package com.apzda.cloud.msg.domain.entity;

import com.apzda.cloud.msg.domain.vo.MailStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@TableName("apzda_mailbox_deliver")
public class Deliver {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    private String createdBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long updatedAt;

    private String updatedBy;

    private Long deliveredAt;

    private Long mailboxId;

    private MailStatus status;

    private Integer retries;

    private String remark;

}
