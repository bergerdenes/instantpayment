-- from postgres db
-- CREATE DATABASE instantpayments;

-- connect to 'instantpayments' db
CREATE TABLE IF NOT EXISTS account
(
    id      varchar(255) NOT NULL PRIMARY KEY,
    balance numeric(38, 2)
);

CREATE TABLE IF NOT EXISTS transaction
(
    id           bigserial PRIMARY KEY,
    amount       numeric(38, 2),
    recipient_id varchar(255),
    sender_id    varchar(255),
    timestamp    timestamp(6) with time zone
);

INSERT INTO account (id, balance) VALUES
    ('user1', 1000),
    ('user2', 2000),
    ('user3', 3000);

