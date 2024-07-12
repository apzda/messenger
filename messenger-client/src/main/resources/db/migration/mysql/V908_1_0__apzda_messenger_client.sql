CREATE TABLE `apzda_mailbox_trans`
(
    id            BIGINT UNIQUE                                       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at    BIGINT UNSIGNED                                     NULL     DEFAULT NULL,
    updated_at    BIGINT UNSIGNED                                     NULL     DEFAULT NULL,
    next_retry_at BIGINT UNSIGNED                                     NOT NULL DEFAULT 0,
    trans_id      CHAR(32)                                            NULL     DEFAULT NULL COMMENT 'transaction id',
    mail_id       VARCHAR(64)                                         NULL     DEFAULT NULL COMMENT 'The ID of this message',
    title         VARCHAR(128)                                        NULL COMMENT 'The Postman to send this mail',
    service       VARCHAR(32)                                         NULL COMMENT 'who sent this message',
    status        ENUM ('PENDING','SENDING','SENT','FAIL','RETRYING') NOT NULL DEFAULT 'PENDING' COMMENT 'The status of this mail',
    postman       VARCHAR(32)                                         NOT NULL COMMENT 'The Postman to send this mail',
    content       Longtext                                            NULL COMMENT 'The content of this mail',
    retries       SMALLINT UNSIGNED                                   NOT NULL DEFAULT 0 COMMENT '',
    remark        text                                                NULL COMMENT 'remark',
    INDEX IDX_STATUS (status, next_retry_at) using btree,
    INDEX IDX_CT (created_at ASC) using btree,
    INDEX IDX_MAIL_ID (mail_id ASC) using btree,
    INDEX IDX_TRANS_ID (trans_id ASC) using btree
) ENGINE = InnoDB COMMENT 'mailbox transactions';
