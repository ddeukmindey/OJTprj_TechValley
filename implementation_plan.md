# Master Implementation Plan: Báo Cáo Xử Lý 20 Vấn Đề Hệ Thống

Tài liệu này tổng hợp chi tiết **Bối Cảnh/Nguyên Nhân, Cách Xử Lý & Trạng Thái Cho 20 Vấn Đề (15 Lỗi Kỹ Thuật Nghiệp Vụ + 5 Tiêu Chuẩn RESTful/Microservices)** của dự án **Cloud Instance Monitoring System - TechValley**.

---

## 🔐 Chiến Lược Phân Quyền Hybrid (Endpoint & Method RBAC)

| Tầng Phân Quyền | Cơ Chế Triển Khai | Phạm Vi & Mục Tiêu |
| :--- | :--- | :--- |
| **1. Endpoint & HTTP Method (Gateway / SecurityConfig)** | Phân quyền thô dựa trên URL Matchers + HTTP Verb (`GET`, `POST`, `DELETE`) | `POST/DELETE /api/instances` $\rightarrow$ Chỉ `ADMIN`<br>`GET /api/instances`, `GET /api/alerts` $\rightarrow$ `ADMIN` & `CLIENT_MANAGER` |
| **2. Method & Data Ownership (Service Layer)** | Phân quyền hạt mịn kiểm tra `managerId == currentUserId` | `CLIENT_MANAGER` chỉ truy cập dữ liệu thuộc Client mình phụ trách. Ném `403 Forbidden` nếu can thiệp dữ liệu khác. |

---

## 📊 Bảng Tổng Hợp 20 Vấn Đề & Hướng Xử Lý Chi Tiết

### 🏛️ NHÓM I: KIẾN TRÚC MICROSERVICES & DOCKER NETWORK (Lỗi 1, 2, 3, 4, 5, 18, 19)

| Mã Lỗi | Tên Vấn Đề | Bối Cảnh / Nguyên Nhân | Hướng Xử Lý & Kết Quả | Trạng Thái |
| :---: | :--- | :--- | :--- | :---: |
| **Lỗi 1 & 19** | DB Query chéo & Vòng lặp N+1 Request | Microservice tự chọc vào DB của service khác; gửi N HTTP request liên tiếp gây trễ 1000ms. | Xóa 100% DB query chéo, tạo RestClients inter-service và thêm API `POST /api/alerts/batch`. | ✅ **100%** |
| **Lỗi 2** | Hardcode `localhost` trong Docker Network | URL các service bị hardcode `localhost`, làm container trỏ vào chính nó thay vì trỏ sang container khác. | Đổi URL trong `application.yml` & `docker-compose.yml` sang tên Docker DNS nội bộ (`http://instance-service:8081`). | ✅ **100%** |
| **Lỗi 3** | Thiếu Token Relay giữa các Microservices | Request REST inter-service không đính kèm `Authorization: Bearer <token>`, bị ngắt lỗi 401. | Tạo `JwtTokenRelayInterceptor` tự đính kèm JWT Token vào Header của mọi HTTP request outbound. | ✅ **100%** |
| **Lỗi 4** | Mở công khai Port nội bộ ra bên ngoài Docker | File `docker-compose.yml` publish tất cả port `8081`-`8085` ra ngoài host, bỏ qua cửa Gateway. | Đóng toàn bộ port nội bộ `8081`-`8085` trong mạng `techvalley-net`, chỉ mở độc nhất port Gateway `8080`. | ✅ **100%** |
| **Lỗi 5** | Tồn tại module Monolith trùng lặp | Thư mục gốc chứa module monolith cũ `cloud-monitor-service` gây xung đột khi build. | Xóa hoàn toàn thư mục monolith cũ `cloud-monitor-service` khỏi repository dự án. | ✅ **100%** |
| **Lỗi 18** | Xung đột Cron Job khi Scale-out *(Scalability)* | Khi scale-out `monitoring-service` đa container, tất cả đồng loạt chạy Cron Job sinh Alert rác. | Thêm `@ConditionalOnProperty` cho `MonitoringScheduler`: chỉ container PRIMARY cấu hình `true` mới khởi tạo Bean. | ✅ **100%** |

---

### ⚖️ NHÓM II: CONSISTENCY & LOGIC NGHIỆP VỤ RBAC (Lỗi 6, 7, 8, 9, 10, 15, 16, 17)

| Mã Lỗi | Tên Vấn Đề | Bối Cảnh / Nguyên Nhân | Hướng Xử Lý & Kết Quả | Trạng Thái |
| :---: | :--- | :--- | :--- | :---: |
| **Lỗi 6** | Vi phạm cách ly dữ liệu RBAC Tenant | `CLIENT_MANAGER` A đọc và sửa được máy chủ/cảnh báo của `CLIENT_MANAGER` B. | Áp dụng Hybrid RBAC: Service layer tự lọc `WHERE manager_id = currentUserId` khi role là `CLIENT_MANAGER`. | ✅ **100%** |
| **Lỗi 7** | Lỗi 500 FK Violation khi Xóa Instance | Xóa Instance đang có bản ghi Alert lịch sử bị PostgreSQL chặn bởi khóa ngoại. | Trước khi xóa Instance, gọi API `deleteAlertsByInstanceId` của `alert-service` để dọn dẹp Alert liên quan. | ✅ **100%** |
| **Lỗi 8** | Lệch kiểu dữ liệu `is_resolve` *(Consistency)* | CSDL lưu kiểu `INT` (0/1) nhưng Java Entity / DTOs dùng `Boolean` (true/false). | Thêm `@Convert(converter = NumericBooleanConverter.class)` trên `Boolean isResolved` trong `Alert.java`. | ✅ **100%** |
| **Lỗi 9** | Thiếu logic cảnh báo máy dừng kéo dài | Hệ thống không tự động phát hiện các máy chủ dừng hoạt động trên 48 giờ. | Cập nhật `MonitoringScheduler`: quét máy `STOPPED` có mốc thời gian $\ge 48$h để tự động chèn Alert `LONG_STOPPED`. | ✅ **100%** |
| **Lỗi 10** | Công thức SLA Uptime & Cost Forecast sai | Tính SLA dựa trên đếm máy `RUNNING` thay vì tính tổng số phút Downtime từ danh sách Alert. | Sửa công thức trong `ClientServiceImpl`: tính SLA % dựa trên tổng số phút Downtime thực tế của các Alert sự cố. | ✅ **100%** |
| **Lỗi 15** | Thiếu tài khoản mẫu `CLIENT_MANAGER` | `DataInitializer` chỉ tạo sẵn 1 tài khoản `admin@techvalley.com`, thiếu tài khoản Manager mẫu. | Cập nhật `DataInitializer`: tự động sinh tài khoản mẫu `manager@techvalley.com` (pass: `password123`). | ✅ **100%** |
| **Lỗi 16** | Lệch trạng thái giữa Instance & Alert *(Consistency)* | Resolve Alert xong nhưng status máy chủ bên `instance-service` vẫn bị kẹt ở chữ `ERROR`. | Trong `resolveAlert()`, gọi API `PATCH /api/instances/{id}/status` tự động khôi phục status về `RUNNING`. | ✅ **100%** |
| **Lỗi 17** | Thiếu Optimistic Locking *(Consistency)* | 2 request cùng UPDATE 1 bản ghi làm ghi đè mất dữ liệu của nhau (Lost Update). | Thêm `@Version private Long version;` vào Entity `Alert`, `Instance`, `Client` và bắt ngoại lệ trả về `409 Conflict`. | ✅ **100%** |

---

### 🎨 NHÓM III: FRONTEND INTEGRATION, CACHE & QUALITY (Lỗi 11, 12, 13, 14, 15_Cache, 20)

| Mã Lỗi | Tên Vấn Đề | Bối Cảnh / Nguyên Nhân | Hướng Xử Lý & Kết Quả | Trạng Thái |
| :---: | :--- | :--- | :--- | :---: |
| **Lỗi 11** | Thiếu CORS cho Web Frontend | Web Frontend (React/Vite `localhost:3000`/`5173`) bị trình duyệt chặn do vi phạm SOP. | Cấu hình `CorsConfigurationSource` ở `SecurityConfig` và `addCorsMappings` ở 4 `WebConfig` microservices. | ✅ **100%** |
| **Lỗi 12** | Thiếu Validation cho `cpuUsage` | Request DTO thiếu `@DecimalMax("100.0")`, người dùng có thể gửi CPU âm hoặc > 100%. | Thêm `@DecimalMax(value = "100.0")` và `@Min(0)` vào `InstanceRequest` và `InstanceStatusUpdateRequest`. | ✅ **100%** |
| **Lỗi 13** | Crash 500 khi truyền sai `sortBy` | Truyền tên cột không có trong Entity làm JPA ném `PropertyReferenceException` crash 500. | Thêm bộ lọc `allowedSortFields`: tự động fallback về cột mặc định `launcheAt` nếu client truyền tên biến sai. | ✅ **100%** |
| **Lỗi 14** | Trùng lặp class `ApiResponse` | 6 microservices tự tạo class `ApiResponse` riêng lẻ gây trùng lặp và tốn công bảo trì. | Đưa `ApiResponse` chuẩn 5 trường vào `common-lib` dùng làm Single Source of Truth cho 5 microservices nghiệp vụ. | ✅ **100%** |
| **Lỗi 15_Cache** | Thiếu Caching cho Danh mục *(Cacheable)* | Mỗi lần reload trang microservices lại truy vấn SQL làm chậm thời gian phản hồi. | Thêm `spring-boot-starter-cache`, `@EnableCaching`, `@Cacheable` cho các API đọc tĩnh (`clientCost`, `clientSla`, `instances`). | ✅ **100%** |
| **Lỗi 20** | Thiếu Swagger API Documentation | Thiếu OpenAPI 3.0 annotations khiến đối tác/Frontend khó tra cứu Hợp đồng API. | Khai báo đầy đủ OpenAPI 3.0 annotations (`@Tag`, `@Operation`) cho toàn bộ 5 Controllers hệ thống. | ✅ **100%** |

---

## 🚀 Tóm Tắt Tiến Độ Theo Giai Đoạn

- **GIAI ĐOẠN 1**: Core Architecture, Dynamic Docker Network & Scalability (Lỗi 1, 2, 3, 4, 5, 18, 19) $\rightarrow$ ✅ **ĐÃ HOÀN THÀNH 100%**
- **GIAI ĐOẠN 2**: CONSISTENCY & Logic Nghiệp Vụ RBAC (Lỗi 6, 7, 8, 9, 10, 15, 16, 17) $\rightarrow$ ✅ **ĐÃ HOÀN THÀNH 100%**
- **GIAI ĐOẠN 3**: CACHEABLE, INTEROPERABILITY & Quality (Lỗi 11, 12, 13, 14, 15_Cache, 20) $\rightarrow$ ✅ **ĐÃ HOÀN THÀNH 100%**
- **GIAI ĐOẠN 4**: Kiểm Thử Hệ Thống & Verification (Build 8/8 modules SUCCESS) $\rightarrow$ ✅ **ĐÃ HOÀN THÀNH 100%**
