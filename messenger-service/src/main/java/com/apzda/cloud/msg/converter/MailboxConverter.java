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
package com.apzda.cloud.msg.converter;

import com.apzda.cloud.msg.domain.entity.Delivery;
import com.apzda.cloud.msg.domain.entity.Mailbox;
import com.apzda.cloud.msg.proto.DeliveryVo;
import com.apzda.cloud.msg.proto.MailboxVo;
import org.mapstruct.*;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MailboxConverter {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Mailbox toEntity(MailboxVo mailbox);

    @InheritInverseConfiguration
    MailboxVo toVo(Mailbox mailbox);

    List<Mailbox> toMailboxes(Iterable<MailboxVo> vos);

    List<MailboxVo> toMailboxVos(Iterable<Mailbox> entities);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Delivery toEntity(DeliveryVo vo);

    @InheritInverseConfiguration
    DeliveryVo toVo(Delivery entity);

    List<Delivery> toDeliveries(Iterable<DeliveryVo> vos);

    List<DeliveryVo> toDeliveryVos(Iterable<Delivery> entities);

}
