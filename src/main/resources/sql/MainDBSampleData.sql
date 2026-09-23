-- 1. [부모] 권한 관리 (member_role)

CREATE TABLE member_role (
role_id          INT AUTO_INCREMENT PRIMARY KEY,
role_name        VARCHAR(20) NOT NULL UNIQUE,
role_description VARCHAR(100) NULL
);

INSERT INTO member_role (role_id, role_name, role_description) VALUES
(1, 'ROLE_USER', '일반 사용자'),
(2, 'ROLE_ADMIN', '관리자');


-- 2. [부모] 가입 유형 (member_type)

CREATE TABLE member_type (
signup_type_no INT AUTO_INCREMENT PRIMARY KEY,
signup_type    VARCHAR(20) NOT NULL
);

INSERT INTO member_type (signup_type_no, signup_type) VALUES
(101, '관리자'),
(201, '수출입기업'),
(301, '물류운송업체');


-- 3. [부모/자식] 회원 (member)

CREATE TABLE member (
member_id       INT AUTO_INCREMENT PRIMARY KEY,
signup_type_no  INT NOT NULL,
user_email      VARCHAR(100) NOT NULL UNIQUE,
user_password   VARCHAR(255) NOT NULL,
company_name    VARCHAR(100) NOT NULL,
manager_name    VARCHAR(50) NOT NULL,
department_name VARCHAR(50),
business_reg_no VARCHAR(12) NOT NULL UNIQUE,
user_phone      VARCHAR(20) NOT NULL,
company_address VARCHAR(300) NOT NULL,
created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_at       DATETIME NULL,
role_id         INT NOT NULL,
status          INT NOT NULL DEFAULT 1,
FOREIGN KEY (signup_type_no) REFERENCES member_type(signup_type_no),
FOREIGN KEY (role_id) REFERENCES member_role(role_id)
);

INSERT INTO member (member_id, signup_type_no, user_email, user_password, company_name, manager_name, department_name, business_reg_no, user_phone, company_address, created_at, update_at, role_id, status) VALUES
(1, 101, 'abcde@naver.com', 'asd123', 'G-Trade', '이은경', '시스템운영팀', '111-11-11111', '010-1234-5678', '경기도 성남시 분당구 정자일로 95(정자동, 네이버 1784)', '2026-09-21 09:47:23', '2026-09-21 09:47:23', 2, 1),
(2, 201, 'aa123@gmail.com', 'aaa000', '이노션', '조익현', '무역분석팀', '222-22-22222', '010-2244-8866', '서울특별시 강남구 강남대로 308(역삼동)', '2026-09-21 09:31:08', '2026-09-21 09:31:08', 1, 1),
(3, 201, 'bb321@naver.com', '001235', '네모토코리아', '이민재', '파트너십팀', '333-33-33333', '010-1313-2424', '서울특별시 서초구 효령로 424, 403호(서초동, 대명빌딩)', '2026-09-21 09:22:45', '2026-09-21 09:22:45', 2, 1),
(4, 301, 'gg999@gmail.com', 'dkssud', '그린월드라인', '남성욱', '글로벌영업팀', '444-44-44444', '010-3693-6936', '경기도 안양시 만안구 안양로455번길 15, 101호', '2026-09-20 18:52:30', '2026-09-21 09:21:08', 1, 1),
(5, 301, 'zzz33@naver.com', 'test1234', '썬샤인로직스', '권미연', '무역분석팀', '555-55-55555', '010-9876-5432', '서울특별시 중구 서소문로 103 924호 (서소문동, 배재빌딩)', '2026-09-20 17:34:11', '2026-09-20 17:40:11', 1, 2);


-- 4. [부모] 작업유형 (action)

CREATE TABLE action (
action_id   INT AUTO_INCREMENT PRIMARY KEY,
action_type VARCHAR(50) NOT NULL
);

INSERT INTO action (action_id, action_type) VALUES
(1, '엑셀 다운로드'),
(2, '로그인'),
(3, '로그인 실패'),
(4, '매칭 승인'),
(5, '매칭 거절'),
(6, '사용자 권한 변경'),
(7, '기업 정보 수정'),
(8, '기업 등록'),
(9, '데이터조회'),
(10, '회원가입'),
(11, '회원탈퇴');


-- 5. [자식] 감사로그 (audit_log)

CREATE TABLE audit_log (
audit_id      INT AUTO_INCREMENT PRIMARY KEY,
created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
member_id     INT NOT NULL,
action_id     INT NOT NULL,
action_detail VARCHAR(300) NOT NULL,
fip_address   VARCHAR(45) NOT NULL,
action_result INT NOT NULL,
FOREIGN KEY (member_id) REFERENCES member(member_id),
FOREIGN KEY (action_id) REFERENCES action(action_id)
);

INSERT INTO audit_log (audit_id, created_at, member_id, action_id, action_detail, fip_address, action_result) VALUES
(1, '2026-09-21 09:47:23', 4, 1, '무역 분석 데이터 다운로드', '10.10.4.52', 1),
(2, '2026-09-21 09:31:08', 1, 2, '정상 로그인(ROLE_ADMIN)', '10.10.2.21', 1),
(3, '2026-09-21 09:22:45', 5, 3, '비밀번호 오류(3회 연속)', '10.10.3.88', 2),
(4, '2026-09-20 18:52:30', 1, 4, 'LG전자-HMM 매칭 승인', '10.10.2.21', 1),
(5, '2026-09-20 17:34:11', 1, 5, '넥센타이어-현대글로비스 매칭 거절', '10.10.2.21', 1),
(6, '2026-09-20 11:05:42', 1, 6, '회원 73 권한 USER→ADMIN 변경', '10.10.2.21', 1);


-- 6. [자식] 수출입 유형 (trade_type)

CREATE TABLE trade_type (
trade_id       INT AUTO_INCREMENT PRIMARY KEY,
member_id      INT NOT NULL,
hs_code        VARCHAR(15) NOT NULL,
country_id     INT NOT NULL,
matching_agree BOOLEAN NOT NULL DEFAULT FALSE,
FOREIGN KEY (member_id) REFERENCES member(member_id)
);

INSERT INTO trade_type (trade_id, member_id, hs_code, country_id, matching_agree) VALUES
(1, 2, '8431200000', 1233, TRUE),
(2, 3, '8213001000', 1114, TRUE),
(3, 4, '8451909000', 1048, FALSE),
(4, 5, '7117191000', 1233, TRUE);


-- 7. [독립] 국가별 품목별 수출입 (trade_statistics)

CREATE TABLE trade_statistics (
country_trade_id INT AUTO_INCREMENT PRIMARY KEY,
trade_balance    DECIMAL(18,2) NOT NULL,
export_amount    DECIMAL(18,2) NOT NULL,
export_weight    DECIMAL(18,3) NULL,
hs_code          VARCHAR(15) NOT NULL,
import_amount    DECIMAL(18,2) NOT NULL,
import_weight    DECIMAL(18,3) NULL,
country_id       INT NOT NULL
);

INSERT INTO trade_statistics (country_trade_id, trade_balance, export_amount, export_weight, hs_code, import_amount, import_weight, country_id) VALUES
(1, -23972, 0, 0, '3506102000', 23972, 40, 1001),
(2, -13622485, 5685, 36, '4900003847', 11110133, 208442, 2087),
(3, 862528, 38431200000, 3441, '1020418213', 2535312, 268, 2130),
(4, 1000, 7140, 103, '10385', -5100, 8031, 1084),
(5, 1909000, 590, 340, '104362', 210394, 48694, 4594),
(6, 77117191000, 2276, 55, '34444', 1048, 7, -338),
(7, 71582, 5895, 439, '2119402034', 69731, 357, 510),
(8, 57867, 6393, 572, '3416230684', 50110000, 28959, 566),
(9, 240, 1057, 9, '-3205', 100, 200, 819),
(10, 20003205, 1132, 68, '1061', 10, -1737, 1885);


-- 8. [자식] 매칭 결과 로그 (match_log)

CREATE TABLE match_log (
match_id               INT AUTO_INCREMENT PRIMARY KEY,
company_member_id      INT NOT NULL,
logistics_member_id    INT NOT NULL,
matched_country_count  INT NOT NULL DEFAULT 0,
matched_hscode_count   INT NOT NULL DEFAULT 0,
country_trade_id       INT NOT NULL,
match_status_update_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
match_status           INT NOT NULL,
FOREIGN KEY (company_member_id) REFERENCES member(member_id),
FOREIGN KEY (logistics_member_id) REFERENCES member(member_id),
FOREIGN KEY (country_trade_id) REFERENCES trade_statistics(country_trade_id)
);

INSERT INTO match_log (match_id, company_member_id, logistics_member_id, matched_country_count, matched_hscode_count, country_trade_id, match_status_update_at, match_status) VALUES
(1, 1, 3, 2, 2, 1, '2026-09-20 09:15:00', 2),
(2, 2, 1, 1, 1, 2, '2026-09-21 15:12:00', 1),
(3, 3, 2, 1, 1, 3, '2026-09-22 14:30:00', 1),
(4, 4, 3, 2, 2, 4, '2026-09-23 11:08:00', 2),
(5, 5, 4, 1, 1, 5, '2026-09-24 10:00:00', 1);

