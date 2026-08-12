-- ============================================
-- SEED DATA — techvalley_client
-- Chạy trên container: techvalley-postgres-client
-- ============================================

-- ⚠️ QUAN TRỌNG: managerId dưới đây phải khớp với id thật của member
-- trong bảng members (database techvalley_auth) — vì 2 database TÁCH RIÊNG,
-- Postgres không thể tự kiểm tra/join giữa 2 database khác nhau (đây chính là
-- đánh đổi của kiến trúc MSA: mất foreign key constraint giữa các service,
-- phải tự đảm bảo tính đúng đắn ở tầng ứng dụng).
--
-- Trước khi chạy file này: chạy seed_auth.sql trước, xem kết quả SELECT cuối file đó,
-- ghi lại đúng "id" của manager1@techvalley.com và manager2@techvalley.com,
-- rồi thay số bên dưới nếu khác 2 và 3.

DELETE FROM clients WHERE client_name IN (
    'Cong ty TNHH ABC Technology',
    'Cong ty Co phan XYZ Solutions',
    'Startup DEF Innovations'
);

INSERT INTO clients (client_name, contract_plan, manager_id, create_at) VALUES
('Cong ty TNHH ABC Technology',   'PREMIUM',  3, NOW()),  -- manager_id = id của manager1@techvalley.com
('Cong ty Co phan XYZ Solutions', 'STANDARD', 3, NOW()),  -- cùng manager1, để test 1 manager quản lý nhiều client
('Startup DEF Innovations',       'BASIC',    4, NOW());  -- manager_id = id của manager2@techvalley.com

-- Xem lại kết quả + lấy ID để dùng cho bước seed instance
SELECT id, client_name, contract_plan, manager_id FROM clients ORDER BY id;
