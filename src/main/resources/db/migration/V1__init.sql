-- 초기 스키마 (2026-11-20)
-- 이미 운영 중인 DB에는 baseline-on-migrate 로 적용 건너뜀

CREATE TABLE IF NOT EXISTS watch_item (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(6)   NOT NULL,
    name       VARCHAR(100),
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_watch_item_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS holding (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    asset_type    VARCHAR(20)     NOT NULL,
    name          VARCHAR(100)    NOT NULL,
    code          VARCHAR(30),
    quantity      DECIMAL(24, 6)  NOT NULL,
    average_price DECIMAL(24, 4)  NOT NULL,
    manual_price  DECIMAL(24, 4),
    memo          VARCHAR(255),
    created_at    DATETIME        NOT NULL,
    updated_at    DATETIME        NOT NULL,
    PRIMARY KEY (id)
);
