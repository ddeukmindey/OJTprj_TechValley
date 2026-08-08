# TechValley — Thứ tự Test Toàn Hệ Thống

Tài liệu này là **điểm bắt đầu**. Đọc file này trước, sau đó vào từng file test riêng của mỗi service theo đúng thứ tự dưới đây — vì các service phụ thuộc dữ liệu lẫn nhau (Member → Client → Instance → Alert).

## 1. Danh sách file test

| # | File | Service | Port |
|---|---|---|---|
| 1 | `01_TEST_auth-gateway-service.md` | auth-gateway-service | 8080 |
| 2 | `02_TEST_client-service.md` | client-service | 8082 |
| 3 | `03_TEST_instance-service.md` | instance-service | 8081 |
| 4 | `04_TEST_monitoring-service.md` | monitoring-service | 8083 |
| 5 | `05_TEST_alert-service.md` | alert-service | 8084 |

## 2. Vì sao phải test theo đúng thứ tự này

```
auth-gateway-service (login, lấy JWT)
        │
        ▼
client-service (tạo Client — cần managerId là id member ADMIN/CLIENT_MANAGER)
        │
        ▼
instance-service (tạo Instance — cần clientId vừa tạo)
        │
        ▼
monitoring-service (quét CPU cao/lỗi/ngưng lâu — cần Instance có sẵn, tự tạo Alert qua alert-service)
        │
        ▼
alert-service (xem lại Alert vừa được monitoring-service tạo, đánh dấu resolved)
```

Nếu bạn test `instance-service` trước khi có `Client` nào, request tạo Instance sẽ lỗi vì `clientId` không tồn tại. Nếu test `monitoring-service` trước khi có `Instance` nào, kết quả trả về rỗng (không sai, nhưng không có gì để kiểm tra).

## 3. Chuẩn bị trước khi test bất kỳ service nào

```bash
# Build lại toàn bộ (bắt buộc sau mọi lần sửa code)
cd /path/to/OJTprj_TechValley
mvn clean install -DskipTests

# Dọn sạch container/volume cũ nếu đổi cấu trúc DB
docker compose down -v --remove-orphans

# Chạy toàn bộ hệ thống
docker compose up --build -d

# Xác nhận tất cả container đang chạy
docker ps -a
```

Kỳ vọng thấy các container sau ở trạng thái `Up`:
- `techvalley-postgres-auth`, `techvalley-postgres-instance`, `techvalley-postgres-client`, `techvalley-postgres-alert`
- `auth-gateway-service`, `instance-service`, `client-service`, `monitoring-service`, `alert-service`, `llm-service`
- `techvalley-pgadmin`

Nếu container nào `Exited`/`Restarting`:
```bash
docker logs <tên-container> --tail 100
```

## 4. Công cụ test

Tất cả ví dụ trong 5 file test dùng `curl`. Bạn có thể thay bằng Postman/Swagger UI nếu muốn — chỉ cần map đúng method + endpoint + body.

Swagger UI của từng service (nếu đã cấu hình springdoc):
```
http://localhost:8080/swagger-ui.html   (auth-gateway-service)
http://localhost:8081/swagger-ui.html   (instance-service)
http://localhost:8082/swagger-ui.html   (client-service)
http://localhost:8083/swagger-ui.html   (monitoring-service)
http://localhost:8084/swagger-ui.html   (alert-service)
```

## 5. Biến dùng chung khi test (lưu lại khi làm theo các file)

Khi làm theo 5 file test, bạn sẽ cần lưu lại các giá trị sau để dùng ở bước sau:

| Biến | Lấy từ đâu | Dùng ở |
|---|---|---|
| `$TOKEN` | Response của `POST /api/auth/login` | Header `Authorization: Bearer $TOKEN` cho MỌI request sau đó |
| `$CLIENT_ID` | Response của `POST /api/clients` | Tạo Instance, test cost/SLA |
| `$INSTANCE_ID` | Response của `POST /api/instances` | Test update status, xem theo instance |
| `$ALERT_ID` | Response của `GET /api/alerts` | Test resolve alert |

## 6. Lưu ý quan trọng — tài khoản mặc định

Hệ thống **tự động seed sẵn 1 tài khoản ADMIN** khi `auth-gateway-service` khởi động lần đầu (xem `DataInitializer.java`):
```
email:    admin@techvalley.com
password: 123456
```
**Không có endpoint đăng ký (`POST /api/members`)** trong code hiện tại — nếu cần thêm tài khoản `CLIENT_MANAGER` để test phân quyền, phải insert thẳng vào database `techvalley_auth` (hướng dẫn chi tiết ở file `01_TEST_auth-gateway-service.md`, mục 4).
