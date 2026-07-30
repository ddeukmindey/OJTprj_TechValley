# Auth Service Architecture

## Mục tiêu
Auth Service chịu trách nhiệm:
- Đăng nhập.
- Xác thực JWT.
- Phân quyền.

## Cấu trúc

```
auth
├── controller
├── service
├── repository
├── entity
├── dto
├── security
├── config
└── exception
```

### entity
Ánh xạ bảng database (`Member`, `Role`).

### repository
Chỉ truy cập database, không chứa business.

### service
Chứa toàn bộ nghiệp vụ:
- kiểm tra email
- kiểm tra mật khẩu BCrypt
- sinh JWT

### controller
Nhận request, gọi service, trả response.

### dto
Request/Response, không trả Entity trực tiếp.

### security
- JwtTokenProvider
- JwtAuthenticationFilter
- SecurityConfig

### config
Bean, PasswordEncoder, Swagger...

### exception
GlobalExceptionHandler và các Exception riêng.

## Luồng Login

```
Frontend
  ↓
POST /api/auth/login
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
  ↓
JWT
  ↓
ApiResponse<LoginResponse>
```

## Quy tắc
- Controller không gọi Repository.
- Repository không chứa business.
- Service là nơi xử lý nghiệp vụ.
- Không trả Entity trực tiếp.
