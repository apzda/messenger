CREATE TABLE `apzda_mailbox_trans`
(
    id         VARCHAR(32)                              NOT NULL PRIMARY KEY,
    created_at BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    updated_at BIGINT UNSIGNED                          NULL     DEFAULT NULL,
    mail_id    VARCHAR(128)                             NULL     DEFAULT NULL COMMENT 'The ID of this message',
    status     ENUM ('PENDING','SENDING','SENT','FAIL') NOT NULL DEFAULT 'PENDING' COMMENT 'The status of this mail',
    sender     VARCHAR(128)                             NOT NULL COMMENT 'The Postman to send this mail',
    content    Longtext                                 NULL COMMENT 'The content of this mail',
    INDEX IDX_STATUS (status) using btree,
    INDEX IDX_CT (created_at ASC) using btree
) ENGINE = InnoDB COMMENT 'mailbox';
