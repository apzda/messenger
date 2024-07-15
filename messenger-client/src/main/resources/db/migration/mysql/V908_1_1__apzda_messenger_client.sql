alter table `apzda_mailbox_trans`
    add recipients text            null comment 'recipients' after postman,
    add post_time  BIGINT UNSIGNED not null DEFAULT 0 comment 'Delivery timestamp' after recipients;
