# Test chi tiết — auth-gateway-service (port 8080)

Đọc `00_TEST_ORDER_TongQuan.md` trước nếu chưa đọc. Service này **phải test đầu tiên** — mọi service khác đều cần JWT token lấy từ đây.

## 1. Danh sách endpoint thực tế trong code

| Method | Endpoint | Auth cần | Mô tả |
|---|---|---|---|
| POST | `/api/auth/login` | Không | Đăng nhập, trả về JWT access token |

*(Đề bài liệt kê thêm `POST /api/auth/logout`, nhưng code hiện tại **chưa implement** — chỉ có `AuthController` với duy nhất method `login`. Nếu bài chấm điểm yêu cầu, cần bổ sung thêm trước khi nộp.)*

## 2. Test Case 1 — Login thành công (tài khoản ADMIN mặc định)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@techvalley.com",
    "password": "123456"
  }'
```

**Kết quả mong đợi:** HTTP 200, body dạng:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    ...
  }
}
```

✅ **Lưu lại giá trị `data.token`** — đây chính là `$TOKEN` dùng cho toàn bộ các bước test sau (mọi service khác).

```bash
export TOKEN="<dán-token-vừa-lấy-vào-đây>"
```

## 3. Test Case 2 — Login sai mật khẩu

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@techvalley.com",
    "password": "sai-mat-khau"
  }'
```
**Kết quả mong đợi:** HTTP 401 hoặc 400, `success: false`, message báo sai thông tin đăng nhập. Kiểm tra hệ thống **không** trả về token khi sai mật khẩu.

## 4. Test Case 3 — Login với email không tồn tại

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "khong-ton-tai@techvalley.com",
    "password": "123456"
  }'
```
**Kết quả mong đợi:** HTTP 401/404, không lộ thông tin kiểu "email không tồn tại" (best practice bảo mật — chỉ nên báo chung "sai email hoặc mật khẩu"). Nếu message hiện tại có phân biệt rõ 2 trường hợp (sai email vs sai password), đây là điểm có thể cải thiện, không bắt buộc sửa gấp.

## 5. Test Case 4 — Validate input rỗng/sai định dạng

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "khong-phai-email",
    "password": ""
  }'
```
**Kết quả mong đợi:** HTTP 400, message validate rõ ràng (`Invalid email format`, `Password is required`) — do `LoginRequest.java` có `@Email` và `@NotBlank`.

## 6. Chuẩn bị tài khoản CLIENT_MANAGER (cần cho test phân quyền ở các service khác)

Vì không có endpoint đăng ký, phải insert thẳng vào DB `techvalley_auth`. Mật khẩu phải mã hoá BCrypt (không insert plain text).

**Cách 1 — dùng pgAdmin** (`http://localhost:5050`, đăng nhập bằng `admin@techvalley.com` / `admin` như đã cấu hình ở `docker-compose.yml`):

Kết nối tới server Postgres (`postgres-auth`, database `techvalley_auth`), chạy:
```sql
INSERT INTO members (email, password, name, role, create_at)
VALUES (
    'manager1@techvalley.com',
    '$2a$10$DowJ1ZR93a5Bt6q4T0m7ZuVYP0V6VvHFfaSY8XZ4Vb.aXjHkP6vHW', -- hash của "123456"
    'Client Manager 1',
    'CLIENT_MANAGER',
    NOW()
);
```

⚠️ Hash trên chỉ là ví dụ minh hoạ — **hash BCrypt sinh ngẫu nhiên mỗi lần dù cùng 1 password**, nên bạn cần tự sinh hash thật bằng 1 trong 2 cách:

**Cách 2 — sinh hash bằng đoạn code Java nhanh** (tạo file tạm `GenHash.java`, chạy 1 lần):
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenHash {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
```

**Cách 3 — dùng trang sinh hash online** (chỉ dùng khi test cục bộ, KHÔNG dùng cho production): tìm "bcrypt generator online", nhập `123456`, copy hash dán vào câu SQL trên.

Sau khi insert xong, test lại:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "manager1@techvalley.com", "password": "123456"}'
```
Lưu token này thành `$TOKEN_MANAGER` — dùng để test các endpoint có phân quyền `CLIENT_MANAGER` ở `client-service`, `instance-service`, `monitoring-service`.

## 7. Checklist hoàn thành

- [ ] Login ADMIN mặc định thành công, lấy được token
- [ ] Login sai password bị từ chối đúng
- [ ] Validate input hoạt động đúng
- [ ] Đã tạo được ít nhất 1 tài khoản `CLIENT_MANAGER` để test phân quyền ở các bước sau
- [ ] Đã lưu `$TOKEN` (ADMIN) và `$TOKEN_MANAGER` (CLIENT_MANAGER) để dùng cho các file test tiếp theo
