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
package com.apzda.cloud.msg;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public class RocketMail extends Mail<String> {

    private String topic;

    private String tags;

    @Override
    public void setRecipients(String recipients) {
        super.setRecipients(recipients);
        val segments = StringUtils.split(recipients, ":");
        if (segments != null) {
            if (segments.length > 1) {
                this.topic = segments[0];
                this.tags = segments[1];
            }
            else if (segments.length == 1) {
                this.topic = segments[0];
            }
        }
    }

    @Override
    public String getBody() {
        return getContent();
    }

    @Override
    public void setPostman(String postman) {
        if ("rocketmq".equals(postman)) {
            super.setPostman("rocketmq");
        }
    }

    @Override
    public String getPostman() {
        return "rocketmq";
    }

}
