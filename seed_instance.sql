-- ============================================
-- SEED DATA — techvalley_instance
-- Chạy trên container: techvalley-postgres-instance
-- ============================================

-- ⚠️ client_id dưới đây phải khớp với id thật trong bảng clients (database techvalley_client).
-- Chạy seed_client.sql trước, xem kết quả SELECT cuối file đó, thay số nếu khác 1/2/3.

DELETE FROM instances WHERE instance_name IN (
    'web-server-01', 'web-server-02', 'db-server-primary',
    'db-server-high-cpu', 'app-server-error', 'cache-server-stopped',
    'backup-server-longstop', 'api-gateway-01'
);

INSERT INTO instances (instance_name, region, instance_type, status, cpu_usage, monthly_cost, client_id, launche_at, update_at) VALUES
-- Client 1 (ABC Technology, PREMIUM) — trạng thái đa dạng để demo đủ tính năng
('web-server-01',          'ap-southeast-1', 'MEDIUM', 'RUNNING', 45.5, 120.0, 4, NOW() - INTERVAL '30 days', NOW()),
('db-server-primary',      'ap-southeast-1', 'LARGE',  'RUNNING', 62.0, 250.0, 4, NOW() - INTERVAL '30 days', NOW()),
('db-server-high-cpu',     'ap-southeast-1', 'LARGE',  'RUNNING', 92.0, 250.0, 4, NOW() - INTERVAL '15 days', NOW()),
('app-server-error',       'ap-northeast-1', 'SMALL',  'ERROR',   10.0, 50.0,  4, NOW() - INTERVAL '10 days', NOW()),

-- Client 2 (XYZ Solutions, STANDARD)
('web-server-02',          'ap-southeast-1', 'SMALL',  'RUNNING', 38.0, 50.0,  5, NOW() - INTERVAL '20 days', NOW()),
('cache-server-stopped',   'ap-southeast-1', 'SMALL',  'STOPPED', 0.0,  50.0,  5, NOW() - INTERVAL '20 days', NOW() - INTERVAL '2 hours'),

-- Client 3 (DEF Innovations, BASIC) — có 1 instance dừng lâu để demo /long-stopped
('backup-server-longstop', 'ap-southeast-1', 'SMALL',  'STOPPED', 0.0,  50.0,  6, NOW() - INTERVAL '60 days', NOW() - INTERVAL '50 hours'),
('api-gateway-01',         'ap-southeast-1', 'MEDIUM', 'RUNNING', 55.0, 120.0, 6, NOW() - INTERVAL '5 days',  NOW());

-- Xem lại kết quả
SELECT id, instance_name, status, cpu_usage, client_id, update_at FROM instances ORDER BY id;
