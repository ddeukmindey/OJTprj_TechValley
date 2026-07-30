# Testing Auth Service

## Chuẩn bị
- PostgreSQL chạy.
- Spring Boot chạy.
- Có tài khoản admin.

## Test Login
build docker từ đầu, nếu không được thì docker compose down -v, sau đó docker compose up --build -d

mở postman:

POST http://localhost:8080/api/auth/login

```json
{
  "email":"admin@techvalley.com",
  "password":"123456"
}
```

Kỳ vọng:
- HTTP 200
- Có accessToken.

## Test sai mật khẩu
Kỳ vọng:
- HTTP 401.

## Test thiếu token

Gọi API bảo vệ.

Kỳ vọng:
- HTTP 401.

## Test role

ADMIN:
- truy cập API ADMIN được.

CLIENT_MANAGER:
- bị 403 với API ADMIN.

## Checklist

- Login OK
- JWT sinh đúng
- JWT xác thực đúng
- Role hoạt động
- Exception trả đúng định dạng
