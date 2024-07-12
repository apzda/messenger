CREATE TABLE `apzda_mailbox`
(
    id            BIGINT UNSIGNED                          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at    BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    updated_at    BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    delivered_at  BIGINT UNSIGNED                          NULL     DEFAULT NULL COMMENT 'delivered time',
    next_retry_at BIGINT UNSIGNED                          NOT NULL DEFAULT 0 COMMENT 'next retry time',
    msg_id        VARCHAR(64)                              NULL     DEFAULT NULL COMMENT 'The ID of this message',
    title         VARCHAR(128)                             NULL COMMENT 'The Postman to send this mail',
    service       VARCHAR(32)                              NULL COMMENT 'who sent this message',
    status        ENUM ('PENDING','SENDING','SENT','FAIL') NOT NULL DEFAULT 'PENDING' COMMENT 'The status of this mail',
    postman       VARCHAR(32)                              NOT NULL COMMENT 'The Postman to send this mail',
    content       Longtext                                 NULL COMMENT 'The content of this mail',
    retries       SMALLINT UNSIGNED                        NOT NULL DEFAULT 0 COMMENT 'retries',
    remark        text                                     NULL COMMENT 'remark',
    INDEX IDX_STATUS (status, next_retry_at) using btree,
    INDEX IDX_CT (created_at ASC) using btree,
    INDEX IDX_MSG_ID (msg_id ASC, postman) using btree,
    INDEX IDX_SENDER (postman ASC) using btree
) ENGINE = InnoDB COMMENT 'mailbox';

CREATE TABLE `apzda_mailbox_deliver`
(
    id           BIGINT UNSIGNED                          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at   BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    created_by   VARCHAR(32)                              NULL COMMENT 'Create User Id',
    updated_at   BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    updated_by   VARCHAR(32)                              NULL COMMENT 'Last updated by who',
    delivered_at BIGINT UNSIGNED                          NULL     DEFAULT NULL COMMENT 'delivered time',
    mailbox_id   BIGINT UNSIGNED                          NOT NULL COMMENT 'The ID of this message',
    status       ENUM ('PENDING','SENDING','SENT','FAIL') NOT NULL DEFAULT 'PENDING' COMMENT 'The status of this mail',
    retries      SMALLINT UNSIGNED                        NOT NULL DEFAULT 0 COMMENT 'retries',
    remark       text                                     NULL COMMENT 'remark',
    INDEX IDX_STATUS (status) using btree,
    INDEX IDX_CT (created_at ASC) using btree,
    INDEX IDX_MSG_ID (mailbox_id ASC) using btree
) ENGINE = InnoDB COMMENT 'mailbox log';
