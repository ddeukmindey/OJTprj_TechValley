-- ============================================
-- SEED DATA — techvalley_auth
-- Chạy trên container: techvalley-postgres-auth
-- ============================================

-- Mật khẩu của TẤT CẢ tài khoản dưới đây: 123456
-- Hash BCrypt đã tạo sẵn cho "123456":
-- $2b$10$gcC6vUzXWLDgPhuQLiRD2uGW1DV/hnjIAcnm9WHW3d4ABGkgPGNdW

-- Xoá dữ liệu cũ nếu chạy lại (tránh trùng email — email có UNIQUE constraint)
DELETE FROM members WHERE email IN (
    'admin@techvalley.com',
    'manager1@techvalley.com',
    'manager2@techvalley.com'
);

INSERT INTO members (email, password, name, role, create_at) VALUES
('admin@techvalley.com',    '$2b$10$gcC6vUzXWLDgPhuQLiRD2uGW1DV/hnjIAcnm9WHW3d4ABGkgPGNdW', 'Nguyen Van Admin',   'ADMIN',          NOW()),
('manager1@techvalley.com', '$2b$10$gcC6vUzXWLDgPhuQLiRD2uGW1DV/hnjIAcnm9WHW3d4ABGkgPGNdW', 'Tran Thi Manager 1', 'CLIENT_MANAGER', NOW()),
('manager2@techvalley.com', '$2b$10$gcC6vUzXWLDgPhuQLiRD2uGW1DV/hnjIAcnm9WHW3d4ABGkgPGNdW', 'Le Van Manager 2',   'CLIENT_MANAGER', NOW());

-- Xem lại kết quả + lấy ID để dùng cho các bước seed sau (client-service cần managerId)
SELECT id, email, name, role FROM members ORDER BY id;
