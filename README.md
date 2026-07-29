# TechValley - Microservices Project

---

## 🛠 Công cụ cần cài đặt sẵn (Prerequisites)

Dự án đã được **Docker hóa hoàn toàn**. Bạn **KHÔNG CẦN** cài đặt JDK, Maven hay PostgreSQL lên máy thật.

Chỉ cần máy tính có sẵn 2 công cụ:
1. **Git**: [Tải Git](https://git-scm.com/downloads)
2. **Docker Desktop**: [Tải Docker Desktop](https://www.docker.com/products/docker-desktop/) *(Bật Docker Desktop lên và đảm bảo trạng thái báo Running - màu xanh)*.

---

##  Hướng dẫn khởi chạy & Xem Database

### Bước 1: Clone dự án về máy
Mở Terminal (Git Bash, Command Prompt hoặc PowerShell) và chạy:
```bash
git clone <URL_REPOSITORY_GIT_CỦA_BẠN>
cd OJTPRJ_TECHVALLEY

### Bước 2: 
docker compose up --build -d
### đợi nó build xong

###bước 3:

# 1. Mở trình duyệt web truy cập: http://localhost:5050
# 2. Đăng nhập pgAdmin bằng tài khoản mặc định:
# * Email: admin@techvalley.com
# * Password: admin


# 3. Đăng ký kết nối tới PostgreSQL Server:
# * Chuột phải vào Servers -> Chọn Register -> Server...
# * Tại tab General: Nhập Name tùy ý (ví dụ: techvalley-db).
# * Tại tab Connection: Nhập chính xác các thông số sau:
# * Host name/address: postgres-db
# * Port: 5432
# * Maintenance database: techvalley_db
# * Username: techvalley_admin
# * Password: password123


# * Bấm Save để lưu.


# 4. Điều hướng cây thư mục bên trái để xem các bảng dữ liệu:
# Servers -> techvalley-db -> Databases -> techvalley_db -> Schemas -> public -> Tables (đủ 5 bảng là thành công)