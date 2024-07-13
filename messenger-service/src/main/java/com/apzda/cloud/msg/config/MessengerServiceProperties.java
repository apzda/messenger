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
package com.apzda.cloud.msg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
@ConfigurationProperties(prefix = "apzda.cloud.postman")
public class MessengerServiceProperties {

    private boolean enabled = true;

    private boolean demoEnabled = true;

    private String group = "MAILBOX_CONSUMER";

    private String namespace;

    private String instanceName;

    private int maxReconsumeTimes = -1;

    private int consumeThreadMin = 20;

    private int consumeThreadMax = 20;

    private int consumeTimeout = 15;

    private int clientApiTimeout = 3 * 1000;

    private int executorCount = 1;

    private Duration delay = Duration.ofSeconds(30);

    private Duration period = Duration.ofSeconds(1);

    private List<Duration> retries = List.of(Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1),
            Duration.ofMinutes(2), Duration.ofMinutes(3), Duration.ofMinutes(4), Duration.ofMinutes(5),
            Duration.ofMinutes(6), Duration.ofMinutes(7), Duration.ofMinutes(8), Duration.ofMinutes(9),
            Duration.ofMinutes(10), Duration.ofMinutes(20), Duration.ofMinutes(30), Duration.ofHours(1),
            Duration.ofHours(2));

}
