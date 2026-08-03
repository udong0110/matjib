-- ============================================
-- 부맛로드(부산맛집로드) DDL (MySQL 8.x)
-- DB 생성:  CREATE DATABASE matjib DEFAULT CHARACTER SET utf8mb4;
-- CREATE TABLE IF NOT EXISTS 사용 → 재시작해도 데이터 유지됨
-- (테이블 구조를 바꿀 땐 해당 테이블을 직접 DROP 후 재시작)
-- ============================================

CREATE TABLE IF NOT EXISTS member (
    member_id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    point       INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS store (
    store_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    store_name  VARCHAR(100) NOT NULL,
    category    VARCHAR(30)  NOT NULL,
    region      VARCHAR(50)  NOT NULL,
    address     VARCHAR(200),
    phone       VARCHAR(30),
    latitude    DOUBLE,                                  -- 위도 (지도용)
    longitude   DOUBLE,                                  -- 경도 (지도용)
    image_path  VARCHAR(300),                            -- 대표 사진 (관리자 등록)
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review (
    review_id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    store_id    BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    rating      INT          NOT NULL,
    image_path  VARCHAR(300),                            -- 첨부 이미지 경로 (없으면 NULL)
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_review_store  FOREIGN KEY (store_id)  REFERENCES store(store_id)
);

CREATE TABLE IF NOT EXISTS point_history (
    history_id  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    amount      INT          NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    reason      VARCHAR(100) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_point_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 방문 인증 (이 기록이 있어야 해당 가게에 리뷰 작성 가능)
CREATE TABLE IF NOT EXISTS visit (
    visit_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    store_id    BIGINT       NOT NULL,
    visited_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_visit_store  FOREIGN KEY (store_id)  REFERENCES store(store_id)
);
