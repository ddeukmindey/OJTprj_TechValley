# Hướng dẫn cấu trúc các module

Mọi module đều theo cùng cấu trúc. (tức là với thư mục monitoring, alert, client, cost, instance)

```
module
├── client 
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity (cái này thì không cần vì t đã tạo sẵn file mà không thuộc thư mục entity rồi (vd: Alert.java, Client.java, ...))
├── repository
├── enums
├── service
│   └── impl
├── exception
├── mapper
└── specification
```
Theo chuẩn Spring Boot:

client gọi đến API truy vấn tới csdl của các module khác 
controller chỉ chứa REST API
service xử lý nghiệp vụ
repository truy cập DB
entity chứa các bảng trong database (ở mấy module còn lại thì không cần thư mục 
entity do đã tạo sẵn các file .java ở ngoài)
enums định nghĩa các type 
dto chứa request/response
config chứa cấu hình

## Thứ tự phát triển

1. Entity
2. Repository
3. DTO
4. Service
5. Controller
6. Exception
7. Security
8. Test

## Vai trò

### Controller
Nhận request.

### Service
Business logic.

### Repository
Làm việc với database.

### DTO
Trao đổi dữ liệu.

### Mapper
Chuyển Entity ↔ DTO.

### Specification
Filter, search, sort.

## Áp dụng

- Instance
- Client
- Alert
- Monitoring
- LLM

đều theo đúng cấu trúc trên để toàn bộ project thống nhất.
