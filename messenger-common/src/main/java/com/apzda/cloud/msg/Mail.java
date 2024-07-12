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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@AllArgsConstructor
public abstract class Mail<T> {

    protected String id;

    protected String postman;

    protected String title;

    protected String service;

    protected String content;

    public Mail(String id, String postman, String content) {
        this.id = id;
        this.postman = postman;
        this.content = content;
    }

    public abstract T getBody();

    @Override
    public String toString() {
        return "[" + postman + "] id = " + getId() + ", content = " + content;
    }

    public String getId() {
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

}
