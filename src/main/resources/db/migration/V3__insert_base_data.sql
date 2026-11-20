-- V3: 기초 데이터 (공통코드, 권한그룹, 메뉴, 약관, API 설정, 자산, 배치 작업)
-- 관리자 계정은 여기 넣지 않음 (앱 초기화 코드 또는 로컬 더미 데이터에서 생성)

-- 공통코드
INSERT INTO TB_CODE_GROUP (GROUP_CD, GROUP_NM, USE_YN, REG_ID, REG_DT) VALUES
('MEMBER_STAT', '회원상태', 'Y', 'SYSTEM', NOW()),
('ADMIN_STAT', '관리자상태', 'Y', 'SYSTEM', NOW()),
('TERMS_TYPE', '약관유형', 'Y', 'SYSTEM', NOW()),
('ASSET_TYPE', '자산유형', 'Y', 'SYSTEM', NOW()),
('MARKET', '시장', 'Y', 'SYSTEM', NOW()),
('ACCOUNT_STAT', '계좌상태', 'Y', 'SYSTEM', NOW()),
('ORDER_SIDE', '매매구분', 'Y', 'SYSTEM', NOW()),
('ORDER_TYPE', '주문유형', 'Y', 'SYSTEM', NOW()),
('ORDER_STAT', '주문상태', 'Y', 'SYSTEM', NOW()),
('TXN_TYPE', '원장거래유형', 'Y', 'SYSTEM', NOW()),
('REF_TYPE', '원장참조유형', 'Y', 'SYSTEM', NOW()),
('USER_TYPE', '사용자유형', 'Y', 'SYSTEM', NOW()),
('LOGIN_RESULT', '로그인결과', 'Y', 'SYSTEM', NOW()),
('MENU_AREA', '메뉴영역', 'Y', 'SYSTEM', NOW()),
('ACTION_TYPE', '관리자행위유형', 'Y', 'SYSTEM', NOW()),
('BATCH_RESULT', '배치결과', 'Y', 'SYSTEM', NOW());

INSERT INTO TB_CODE (GROUP_CD, CODE, CODE_NM, SORT_ORD, USE_YN, REG_ID) VALUES
('MEMBER_STAT', 'ACTIVE', '정상', 1, 'Y', 'SYSTEM'),
('MEMBER_STAT', 'LOCKED', '잠금', 2, 'Y', 'SYSTEM'),
('MEMBER_STAT', 'DORMANT', '휴면', 3, 'Y', 'SYSTEM'),
('MEMBER_STAT', 'WITHDRAWN', '탈퇴', 4, 'Y', 'SYSTEM'),
('ADMIN_STAT', 'ACTIVE', '정상', 1, 'Y', 'SYSTEM'),
('ADMIN_STAT', 'LOCKED', '잠금', 2, 'Y', 'SYSTEM'),
('ADMIN_STAT', 'RETIRED', '퇴직', 3, 'Y', 'SYSTEM'),
('TERMS_TYPE', 'SERVICE', '서비스 이용약관', 1, 'Y', 'SYSTEM'),
('TERMS_TYPE', 'PRIVACY', '개인정보 처리방침', 2, 'Y', 'SYSTEM'),
('TERMS_TYPE', 'MARKETING', '마케팅 수신 동의', 3, 'Y', 'SYSTEM'),
('ASSET_TYPE', 'GOLD', '금', 1, 'Y', 'SYSTEM'),
('ASSET_TYPE', 'FX', '외화', 2, 'Y', 'SYSTEM'),
('ASSET_TYPE', 'STOCK', '주식', 3, 'Y', 'SYSTEM'),
('MARKET', 'KRX_GOLD', 'KRX 금시장', 1, 'Y', 'SYSTEM'),
('MARKET', 'FX', '외환', 2, 'Y', 'SYSTEM'),
('MARKET', 'KOSPI', '코스피', 3, 'Y', 'SYSTEM'),
('MARKET', 'KOSDAQ', '코스닥', 4, 'Y', 'SYSTEM'),
('ACCOUNT_STAT', 'ACTIVE', '정상', 1, 'Y', 'SYSTEM'),
('ACCOUNT_STAT', 'SUSPENDED', '정지', 2, 'Y', 'SYSTEM'),
('ACCOUNT_STAT', 'CLOSED', '해지', 3, 'Y', 'SYSTEM'),
('ORDER_SIDE', 'BUY', '매수', 1, 'Y', 'SYSTEM'),
('ORDER_SIDE', 'SELL', '매도', 2, 'Y', 'SYSTEM'),
('ORDER_TYPE', 'MARKET', '시장가', 1, 'Y', 'SYSTEM'),
('ORDER_TYPE', 'LIMIT', '지정가', 2, 'Y', 'SYSTEM'),
('ORDER_STAT', 'RECEIVED', '접수', 1, 'Y', 'SYSTEM'),
('ORDER_STAT', 'PARTIAL', '부분체결', 2, 'Y', 'SYSTEM'),
('ORDER_STAT', 'FILLED', '체결완료', 3, 'Y', 'SYSTEM'),
('ORDER_STAT', 'CANCELED', '취소', 4, 'Y', 'SYSTEM'),
('ORDER_STAT', 'REJECTED', '거부', 5, 'Y', 'SYSTEM'),
('TXN_TYPE', 'INIT_DEPOSIT', '초기지급', 1, 'Y', 'SYSTEM'),
('TXN_TYPE', 'BUY_AMT', '매수대금', 2, 'Y', 'SYSTEM'),
('TXN_TYPE', 'SELL_AMT', '매도대금', 3, 'Y', 'SYSTEM'),
('TXN_TYPE', 'FEE', '수수료', 4, 'Y', 'SYSTEM'),
('TXN_TYPE', 'TAX', '세금', 5, 'Y', 'SYSTEM'),
('TXN_TYPE', 'ADMIN_ADJ', '관리자조정', 6, 'Y', 'SYSTEM'),
('REF_TYPE', 'ORDER', '주문', 1, 'Y', 'SYSTEM'),
('REF_TYPE', 'EXEC', '체결', 2, 'Y', 'SYSTEM'),
('REF_TYPE', 'ADMIN', '관리자', 3, 'Y', 'SYSTEM'),
('USER_TYPE', 'MEMBER', '회원', 1, 'Y', 'SYSTEM'),
('USER_TYPE', 'ADMIN', '관리자', 2, 'Y', 'SYSTEM'),
('LOGIN_RESULT', 'SUCCESS', '성공', 1, 'Y', 'SYSTEM'),
('LOGIN_RESULT', 'WRONG_PWD', '비밀번호 오류', 2, 'Y', 'SYSTEM'),
('LOGIN_RESULT', 'LOCKED', '잠금 계정', 3, 'Y', 'SYSTEM'),
('LOGIN_RESULT', 'NO_USER', '없는 아이디', 4, 'Y', 'SYSTEM'),
('MENU_AREA', 'ADMIN', '관리자', 1, 'Y', 'SYSTEM'),
('MENU_AREA', 'USER', '사용자', 2, 'Y', 'SYSTEM'),
('ACTION_TYPE', 'READ', '조회', 1, 'Y', 'SYSTEM'),
('ACTION_TYPE', 'CREATE', '등록', 2, 'Y', 'SYSTEM'),
('ACTION_TYPE', 'UPDATE', '수정', 3, 'Y', 'SYSTEM'),
('ACTION_TYPE', 'DELETE', '삭제', 4, 'Y', 'SYSTEM'),
('ACTION_TYPE', 'DOWNLOAD', '다운로드', 5, 'Y', 'SYSTEM'),
('BATCH_RESULT', 'SUCCESS', '성공', 1, 'Y', 'SYSTEM'),
('BATCH_RESULT', 'FAIL', '실패', 2, 'Y', 'SYSTEM'),
('BATCH_RESULT', 'PARTIAL', '부분성공', 3, 'Y', 'SYSTEM');

UPDATE TB_CODE SET REG_DT = NOW();

-- 권한그룹
INSERT INTO TB_AUTH_GROUP (AUTH_GRP_CD, AUTH_GRP_NM, USE_YN, REG_ID) VALUES
('SUPER', '슈퍼 관리자', 'Y', 'SYSTEM'),
('OPERATOR', '운영자', 'Y', 'SYSTEM'),
('VIEWER', '조회 전용', 'Y', 'SYSTEM');

-- 메뉴 (상위메뉴가 먼저 들어가도록 순서대로)
INSERT INTO TB_MENU (MENU_ID, UPPER_MENU_ID, MENU_NM, MENU_URL, MENU_AREA_CD, MENU_LEVEL, SORT_ORD, USE_YN, DISPLAY_YN, REG_ID) VALUES
('ADM0100', NULL, '대시보드', '/admin/dashboard', 'ADMIN', 1, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0200', NULL, '회원관리', NULL, 'ADMIN', 1, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0201', 'ADM0200', '회원 목록', '/admin/members', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0202', 'ADM0200', '약관 관리', '/admin/terms', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0300', NULL, '투자관리', NULL, 'ADMIN', 1, 3, 'Y', 'Y', 'SYSTEM'),
('ADM0301', 'ADM0300', '주문/체결 조회', '/admin/orders', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0302', 'ADM0300', '계좌/원장 조회', '/admin/accounts', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0400', NULL, '시세관리', NULL, 'ADMIN', 1, 4, 'Y', 'Y', 'SYSTEM'),
('ADM0401', 'ADM0400', '자산 관리', '/admin/assets', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0402', 'ADM0400', '시세 조회', '/admin/prices', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0500', NULL, '연동관리', NULL, 'ADMIN', 1, 5, 'Y', 'Y', 'SYSTEM'),
('ADM0501', 'ADM0500', 'API 설정', '/admin/api-configs', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0502', 'ADM0500', '배치 관리', '/admin/batches', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0503', 'ADM0500', 'API 호출 로그', '/admin/api-logs', 'ADMIN', 2, 3, 'Y', 'Y', 'SYSTEM'),
('ADM0600', NULL, '로그관리', NULL, 'ADMIN', 1, 6, 'Y', 'Y', 'SYSTEM'),
('ADM0601', 'ADM0600', '로그인 이력', '/admin/logs/login', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0602', 'ADM0600', '접속 로그', '/admin/logs/access', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0603', 'ADM0600', '관리자 행위 로그', '/admin/logs/action', 'ADMIN', 2, 3, 'Y', 'Y', 'SYSTEM'),
('ADM0700', NULL, '시스템관리', NULL, 'ADMIN', 1, 7, 'Y', 'Y', 'SYSTEM'),
('ADM0701', 'ADM0700', '관리자 계정', '/admin/system/admins', 'ADMIN', 2, 1, 'Y', 'Y', 'SYSTEM'),
('ADM0702', 'ADM0700', '권한그룹', '/admin/system/auth-groups', 'ADMIN', 2, 2, 'Y', 'Y', 'SYSTEM'),
('ADM0703', 'ADM0700', '메뉴 관리', '/admin/system/menus', 'ADMIN', 2, 3, 'Y', 'Y', 'SYSTEM'),
('ADM0704', 'ADM0700', '공통코드', '/admin/system/codes', 'ADMIN', 2, 4, 'Y', 'Y', 'SYSTEM'),
('USR0100', NULL, '홈', '/', 'USER', 1, 1, 'Y', 'Y', 'SYSTEM'),
('USR0200', NULL, '시세', NULL, 'USER', 1, 2, 'Y', 'Y', 'SYSTEM'),
('USR0201', 'USR0200', '금 시세', '/prices/gold', 'USER', 2, 1, 'Y', 'Y', 'SYSTEM'),
('USR0202', 'USR0200', '환율', '/prices/fx', 'USER', 2, 2, 'Y', 'Y', 'SYSTEM'),
('USR0203', 'USR0200', '주식', '/prices/stock', 'USER', 2, 3, 'Y', 'Y', 'SYSTEM'),
('USR0300', NULL, '모의투자', NULL, 'USER', 1, 3, 'Y', 'Y', 'SYSTEM'),
('USR0301', 'USR0300', '주문하기', '/invest/order', 'USER', 2, 1, 'Y', 'Y', 'SYSTEM'),
('USR0302', 'USR0300', '주문 내역', '/invest/orders', 'USER', 2, 2, 'Y', 'Y', 'SYSTEM'),
('USR0303', 'USR0300', '내 자산', '/invest/holdings', 'USER', 2, 3, 'Y', 'Y', 'SYSTEM'),
('USR0400', NULL, '마이페이지', NULL, 'USER', 1, 4, 'Y', 'Y', 'SYSTEM'),
('USR0401', 'USR0400', '내 정보', '/mypage/profile', 'USER', 2, 1, 'Y', 'Y', 'SYSTEM'),
('USR0402', 'USR0400', '로그인 기록', '/mypage/logins', 'USER', 2, 2, 'Y', 'Y', 'SYSTEM');

-- 권한그룹별 메뉴 권한 (SUPER: 전체 / OPERATOR: 시스템관리 제외, 삭제 불가 / VIEWER: 조회만)
INSERT INTO TB_AUTH_MENU (AUTH_GRP_CD, MENU_ID, READ_YN, WRITE_YN, DELETE_YN, DOWNLOAD_YN, REG_ID) VALUES
('SUPER', 'ADM0100', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0100', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0100', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0200', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0200', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0200', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0201', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0201', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0201', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0202', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0202', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0202', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0300', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0300', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0300', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0301', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0301', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0301', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0302', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0302', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0302', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0400', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0400', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0400', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0401', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0401', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0401', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0402', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0402', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0402', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0500', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0500', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0500', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0501', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0501', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0501', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0502', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0502', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0502', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0503', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0503', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0503', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0600', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0600', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0600', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0601', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0601', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0601', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0602', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0602', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0602', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0603', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('OPERATOR', 'ADM0603', 'Y', 'Y', 'N', 'Y', 'SYSTEM'),
('VIEWER', 'ADM0603', 'Y', 'N', 'N', 'N', 'SYSTEM'),
('SUPER', 'ADM0700', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('SUPER', 'ADM0701', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('SUPER', 'ADM0702', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('SUPER', 'ADM0703', 'Y', 'Y', 'Y', 'Y', 'SYSTEM'),
('SUPER', 'ADM0704', 'Y', 'Y', 'Y', 'Y', 'SYSTEM');

-- 약관
INSERT INTO TB_TERMS (TERMS_NO, TERMS_TYPE_CD, TERMS_VER, TERMS_CN, REQUIRED_YN, APPLY_DE, REG_ID) VALUES
(1, 'SERVICE', '1.0', '서비스 이용약관 본문 (임시)', 'Y', '20261101', 'SYSTEM'),
(2, 'PRIVACY', '1.0', '개인정보 처리방침 본문 (임시)', 'Y', '20261101', 'SYSTEM'),
(3, 'MARKETING', '1.0', '마케팅 정보 수신 동의 본문 (임시)', 'N', '20261101', 'SYSTEM');

-- API 설정 (API 키는 비워둠. URL은 각 API 문서에서 한 번 더 확인할 것)
INSERT INTO TB_API_CONFIG (API_SRC_CD, API_NM, BASE_URL, API_KEY_ENC, DAILY_LIMIT, TIMEOUT_MS, USE_YN, REG_ID) VALUES
('FSC_GOLD', '금융위원회 일반상품시세정보(금시세)', 'https://apis.data.go.kr/1160100/service/GetGeneralProductInfoService', NULL, 10000, 5000, 'Y', 'SYSTEM'),
('EXIM_FX', '한국수출입은행 환율', 'https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON', NULL, 1000, 5000, 'Y', 'SYSTEM'),
('KIS', '한국투자증권 KIS Developers (모의투자)', 'https://openapivts.koreainvestment.com:29443', NULL, NULL, 5000, 'Y', 'SYSTEM');

-- 자산 마스터
INSERT INTO TB_ASSET (ASSET_NO, ASSET_TYPE_CD, ASSET_CD, ASSET_NM, MARKET_CD, CURRENCY_CD, TRADE_UNIT, API_SRC_CD, TRADABLE_YN, USE_YN, REG_ID) VALUES
(1, 'GOLD', 'KRX_GOLD', '금 99.99 (1g)', 'KRX_GOLD', 'KRW', 1, 'FSC_GOLD', 'Y', 'Y', 'SYSTEM'),
(2, 'FX', 'USD', '미국 달러', 'FX', 'KRW', 1, 'EXIM_FX', 'Y', 'Y', 'SYSTEM'),
(3, 'FX', 'JPY100', '일본 엔 (100엔)', 'FX', 'KRW', 1, 'EXIM_FX', 'Y', 'Y', 'SYSTEM'),
(4, 'FX', 'EUR', '유로', 'FX', 'KRW', 1, 'EXIM_FX', 'Y', 'Y', 'SYSTEM'),
(5, 'FX', 'CNH', '중국 위안', 'FX', 'KRW', 1, 'EXIM_FX', 'Y', 'Y', 'SYSTEM'),
(6, 'FX', 'GBP', '영국 파운드', 'FX', 'KRW', 1, 'EXIM_FX', 'Y', 'Y', 'SYSTEM'),
(7, 'STOCK', '005930', '삼성전자', 'KOSPI', 'KRW', 1, 'KIS', 'Y', 'Y', 'SYSTEM'),
(8, 'STOCK', '000660', 'SK하이닉스', 'KOSPI', 'KRW', 1, 'KIS', 'Y', 'Y', 'SYSTEM'),
(9, 'STOCK', '035420', 'NAVER', 'KOSPI', 'KRW', 1, 'KIS', 'Y', 'Y', 'SYSTEM'),
(10, 'STOCK', '005380', '현대차', 'KOSPI', 'KRW', 1, 'KIS', 'Y', 'Y', 'SYSTEM'),
(11, 'STOCK', '035720', '카카오', 'KOSPI', 'KRW', 1, 'KIS', 'Y', 'Y', 'SYSTEM');

-- 배치 작업
INSERT INTO TB_BATCH_JOB (JOB_ID, JOB_NM, CRON_EXPR, USE_YN, REG_ID) VALUES
('GOLD_DAILY_COLLECT', '금 일별 시세 수집', '0 30 13 * * MON-FRI', 'Y', 'SYSTEM'),
('FX_DAILY_COLLECT', '환율 일별 수집', '0 30 11 * * MON-FRI', 'Y', 'SYSTEM'),
('STOCK_DAILY_COLLECT', '주식 일별 시세 수집', '0 0 16 * * MON-FRI', 'Y', 'SYSTEM'),
('KIS_TOKEN_REFRESH', 'KIS 토큰 갱신', '0 0 7 * * *', 'Y', 'SYSTEM');

-- 등록일시 일괄 세팅
UPDATE TB_CODE_GROUP SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_AUTH_GROUP SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_MENU SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_AUTH_MENU SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_TERMS SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_API_CONFIG SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_ASSET SET REG_DT = NOW() WHERE REG_DT IS NULL;
UPDATE TB_BATCH_JOB SET REG_DT = NOW() WHERE REG_DT IS NULL;

-- 명시적으로 번호를 넣은 테이블은 다음 자동번호가 겹치지 않게 시작값을 올려둠
ALTER TABLE TB_TERMS AUTO_INCREMENT = 100;
ALTER TABLE TB_ASSET AUTO_INCREMENT = 100;
