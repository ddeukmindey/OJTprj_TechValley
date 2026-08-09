# Master Implementation Plan: Chuẩn Hóa Kiến Trúc & Xử Lý Chi Tiết 20 Vấn Đề Hệ Thống

Tài liệu này tổng hợp chi tiết **Bối Cảnh Nguyên Nhân, Hậu Quả & Hướng Sửa Chữa Chi Tiết Cho 20 Vấn Đề (Gồm 15 Lỗi Kỹ Thuật Nghiệp Vụ + 5 Tiêu Chuẩn RESTful/Microservices: Stateless, Cacheable, Consistency, Scalability, Interoperability)** của dự án **Cloud Instance Monitoring System - TechValley**.

---

## 🔐 Chiến Lược Phân Quyền Hybrid (Endpoint & Method RBAC)

| Tầng Phân Quyền | Cơ Chế Triển Khai | Phạm Vi & Mục Tiêu |
| :--- | :--- | :--- |
| **1. Tầng Endpoint & HTTP Method (Gateway / SecurityConfig)** | Phân quyền thô dựa trên URL Matchers + HTTP Verb (`GET`, `POST`, `DELETE`) | `POST/DELETE /api/instances` $\rightarrow$ Chỉ `ADMIN`<br>`GET /api/instances`, `GET /api/alerts` $\rightarrow$ `ADMIN` & `CLIENT_MANAGER` |
| **2. Tầng Method & Data Ownership (Service Layer)** | Phân quyền hạt mịn kiểm tra `managerId == currentUserId` | `CLIENT_MANAGER` chỉ truy cập dữ liệu thuộc Client mà mình phụ trách. Ném `403 Forbidden` nếu cố ý can thiệp dữ liệu người khác. |

---

## 📑 Danh Sách Chi Tiết Bối Cảnh & Hướng Sửa Chữa 20 Vấn Đề

---

### 🏛️ NHÓM I: GIÁM SÁT KIẾN TRÚC & GATEWAY (Lỗi 1, 2, 3, 4, 5, 18, 19)

#### 📌 Lỗi 1 & 19: Đọc/Sửa trực tiếp CSDL chung & Nghẽn Vòng Lặp N+1 Request
* **Bối Cảnh & Hậu Quả**: Mã nguồn cũ nhúng `AlertRepository`, `InstanceRepository`, `ClientRepository` vào `monitoring-service` và `client-service` để chọc trực tiếp vào các bảng SQL không thuộc sở hữu. Đồng thời, `monitoring-service` gửi N request HTTP riêng lẻ liên tiếp để tạo từng Alert một, gây trễ mạng 1000ms và làm tràn RAM container.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Thêm API `POST /api/alerts/batch` ở `alert-service`. Xóa 100% DB Query chéo ở `monitoring-service` & `client-service`. Tạo RestClients (`InstanceServiceClient`, `AlertServiceClient`, `ClientServiceClient`) và dọn dẹp sạch JPA Entities trùng lặp.

#### 📌 Lỗi 2: Hardcode `localhost` gây lỗi kết nối Docker Network
* **Bối Cảnh & Hậu Quả**: URL các service bị hardcode dạng `http://localhost:8081`. Khi đóng gói chạy trong Docker Compose network (`techvalley-net`), từ ngữ `localhost` trỏ về chính container nội bộ đó chứ không trỏ sang container dịch vụ khác, gây ném ngoại lệ `Connection Refused` toàn hệ thống.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Cấu hình URL trong `application.yml` & `docker-compose.yml` theo tên Docker DNS nội bộ (`http://instance-service:8081`, `http://alert-service:8084`, `http://client-service:8082`).

#### 📌 Lỗi 3: Thiếu Token Relay khi giao tiếp Inter-service
* **Bối Cảnh & Hậu Quả**: Các cuộc gọi REST giữa các microservices (ví dụ từ `monitoring-service` sang `alert-service`) không mang theo Header `Authorization: Bearer <token>`. Khi API phía nhận kiểm tra `JwtInterceptor`, request bị ngắt ngay lập tức và ném lỗi `401 Unauthorized`.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Tạo `JwtTokenRelayInterceptor` tự động lấy Header `Authorization: Bearer <token>` từ RequestContextHolder và đính kèm vào mọi HTTP request outbound của `RestClient`.

#### 📌 Lỗi 4: Mở lộ toàn bộ Port dịch vụ nội bộ ra ngoài Docker
* **Bối Cảnh & Hậu Quả**: File `docker-compose.yml` mở công khai tất cả các port `8081` đến `8085` ra ngoài Host. Kẻ xấu có thể gọi trực tiếp URL microservice bên trong để sửa/xóa dữ liệu mà không cần đi qua cửa bảo vệ API Gateway (`8080`), vô hiệu hóa hoàn toàn cơ chế xác thực JWT & CORS.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Cập nhật `docker-compose.yml` chỉ publish port `8080` (Gateway) và `5050` (pgAdmin). Khóa các port `8081`-`8085` trong mạng nội bộ `techvalley-net`.

#### 📌 Lỗi 5: Tồn tại module Monolith trùng lặp (`cloud-monitor-service`)
* **Bối Cảnh & Hậu Quả**: Thư mục gốc dự án tồn tại đồng thời module monolith cũ `cloud-monitor-service` chứa mã nguồn trùng lặp với các microservices tách lẻ, gây nhập nhằng trong quản lý repository, xung đột khi build Maven và gây nhầm lẫn cho lập trình viên.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Đã xóa hoàn toàn thư mục monolith cũ `cloud-monitor-service` khỏi dự án.

#### 📌 Lỗi 18: SCALABILITY - Xung đột Cron Job khi Scale-out Multi-container
* **Bối Cảnh & Hậu Quả**: Khi scale-out `monitoring-service` thành nhiều container chạy song song, tất cả các container sẽ đồng loạt kích hoạt `@Scheduled` cùng lúc 5 phút/lần, dẫn đến spam trùng lặp hàng loạt Cảnh báo rác vào CSDL.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Thêm dependency `ShedLock` và cấu hình `@SchedulerLock` trong `MonitoringScheduler.java` đảm bảo khi scale-out multi-container chỉ 1 container duy nhất chạy Cron Job tại 1 thời điểm.

---

### ⚖️ NHÓM II: TIÊU CHUẨN CONSISTENCY & LOGIC NGHIỆP VỤ RBAC (Lỗi 6, 7, 8, 9, 10, 15, 16, 17)

#### 📌 Lỗi 17: CONSISTENCY - Thiếu Optimistic Locking (`@Version`)
* **Bối Cảnh & Hậu Quả**: Khi 2 người dùng hoặc 2 service đồng thời bấm nút cập nhật (UPDATE) trên cùng 1 bản ghi Alert hoặc Instance tại cùng 1 milisecond, request tới sau sẽ ghi đè hoàn toàn dữ liệu của request trước (Lost Update Problem) do thiếu trường `@Version`.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Bổ sung `@Version private Long version;` vào Entity `Alert.java`, `Instance.java`, `Client.java` và thêm `OptimisticLockingFailureException` handler trả về `409 Conflict`.

#### 📌 Lỗi 16: CONSISTENCY - Lệch trạng thái máy chủ giữa Instance & Alert
* **Bối Cảnh & Hậu Quả**: Khi Admin xử lý xong sự cố và bấm Resolve Alert ở `alert-service`, Alert chuyển thành `RESOLVED` nhưng status máy chủ bên `instance-service` vẫn bị kẹt ở chữ `ERROR`. Dẫn đến trên giao diện Cảnh báo đã báo xanh nhưng giao diện Danh sách máy vẫn báo đỏ.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Cập nhật `resolveAlert` trong [AlertServiceImpl.java](file:///d:/OTJprj_TechValley/alert-service/src/main/java/com/techvalley/alert/service/impl/AlertServiceImpl.java): Tạo [InstanceServiceClient.java](file:///d:/OTJprj_TechValley/alert-service/src/main/java/com/techvalley/alert/client/InstanceServiceClient.java) tự động gọi REST API `PATCH /api/instances/{id}/status` đổi `status` máy chủ về `RUNNING` khi Resolve thành công Alert sự cố.

#### 📌 Lỗi 8: CONSISTENCY - Sai kiểu dữ liệu `isResolved` (Integer `0/1` vs Boolean)
* **Bối Cảnh & Hậu Quả**: Đặc tả CSDL PostgreSQL quy định cột `is_resolve` kiểu `BOOLEAN` (`true`/`false`), nhưng Java Entity `Alert.java` lại khai báo `Integer isResolved` (`0`/`1`). Việc này gây mâu thuẫn tiêu chuẩn CONSISTENCY, lệch kiểu dữ liệu khi JPA query và khiến UI Badges hiển thị sai logic.
* **Trạng thái**: ✅ **ĐÃ FIX 100%**
* **Hướng sửa đã thực hiện**: Chuyển `isResolved` trong Entity `Alert.java`, `AlertResponse`, `AlertMapper`, `AlertSpecification`, `AlertRepository` và `AlertServiceImpl` từ Integer `0/1` sang kiểu `Boolean` (`true`/`false`), gắn `@Column(name = "is_resolve")` khớp 100% với PostgreSQL & UI Guidelines.

#### 📌 Lỗi 6: Vi phạm phân quyền RBAC Data Isolation
* **Bối Cảnh & Hậu Quả**: Các API truy vấn danh sách máy chủ (`GET /api/instances`) và cảnh báo thiếu bộ lọc người quản lý `manager_id`. Dẫn đến người dùng role `CLIENT_MANAGER` A có thể đọc và thao tác dữ liệu máy chủ của `CLIENT_MANAGER` B, vi phạm nghiêm trọng tính riêng tư và phân quyền tenant trong hệ thống SaaS.
* **Trạng thái**: ⏳ *Giai đoạn 2*
* **Hướng sửa**: Áp dụng Hybrid RBAC: Endpoint level (`SecurityConfig`) phân quyền HTTP Verb; Method level (`InstanceServiceImpl`, `AlertServiceImpl`) đọc `UserContext` và lọc `WHERE manager_id = currentUserId` khi role là `CLIENT_MANAGER`. Ném `403 Forbidden` nếu can thiệp dữ liệu khác.

#### 📌 Lỗi 7: Foreign Key Violation (`500`) khi Xóa Instance
* **Bối Cảnh & Hậu Quả**: Bảng `alerts` chứa khóa ngoại `instance_id` trỏ về `instances.id`. Khi người dùng xóa 1 máy chủ đang có các bản ghi lịch sử cảnh báo, PostgreSQL chặn hành động xóa và ném ngoại lệ `DataIntegrityViolationException`, khiến client nhận lỗi `500 Internal Server Error`.
* **Trạng thái**: ⏳ *Giai đoạn 2*
* **Hướng sửa**: Trước khi xóa `Instance` ở `instance-service`, tự động gọi API `DELETE /api/alerts?instanceId={id}` của `alert-service` để dọn dẹp các `alerts` liên quan.

#### 📌 Lỗi 9: Thiếu logic tự động cảnh báo máy dừng kéo dài (`LONG_STOPPED`)
* **Bối Cảnh & Hậu Quả**: Hệ thống yêu cầu tự động phát hiện các máy chủ ảo dừng hoạt động trên 48 giờ để tạo cảnh báo `LONG_STOPPED`. Mã nguồn cũ thiếu Cron Job kiểm tra mốc thời gian `updateAt`/`launcheAt`, dẫn đến máy dừng lâu ngày nhưng hệ thống không hề cảnh báo cho quản trị viên.
* **Trạng thái**: ⏳ *Giai đoạn 2*
* **Hướng sửa**: Cập nhật `MonitoringScheduler.java`: Kiểm tra mốc thời gian `updateAt` / `launcheAt` $\ge 48$ giờ đối với máy `STOPPED` để tự động chèn Alert `LONG_STOPPED`.

#### 📌 Lỗi 10: Công thức SLA Uptime & Cost Forecast chưa chuẩn
* **Bối Cảnh & Hậu Quả**: Hàm `getClientSla` cũ tính phần trăm SLA dựa trên việc đếm số máy `RUNNING` thời điểm hiện tại thay vì tính tổng số phút Downtime từ danh sách Alert sự cố trong tháng. Dẫn đến chỉ số SLA báo sai lệch so với cam kết hợp đồng (BASIC 95%, STANDARD 99%, PREMIUM 99.9%).
* **Trạng thái**: ⏳ *Giai đoạn 2*
* **Hướng sửa**: Cập nhật công thức trong `ClientServiceImpl.java`: Tính SLA Uptime dựa trên tổng số phút Downtime từ danh sách Alert sự cố thực tế thay vì đếm số máy `RUNNING` đơn thuần.

#### 📌 Lỗi 15: Thiếu tài khoản Dữ liệu mẫu `CLIENT_MANAGER`
* **Bối Cảnh & Hậu Quả**: `DataInitializer` ở `auth-gateway-service` chỉ tạo duy nhất 1 tài khoản `admin@techvalley.com`. Hệ thống thiếu sẵn tài khoản mẫu role `CLIENT_MANAGER` để kiểm thử tính năng phân quyền cách ly dữ liệu RBAC.
* **Trạng thái**: ⏳ *Giai đoạn 2*
* **Hướng sửa**: Cập nhật `DataInitializer.java` ở `auth-gateway-service` tự động sinh tài khoản mẫu `manager@techvalley.com` (Role: `CLIENT_MANAGER`, Password: `password123`).

---

### 🎨 NHÓM III: FRONTEND INTEGRATION, CACHEABLE & QUALITY (Lỗi 11, 12, 13, 14, 15_Cache, 20)

#### 📌 Lỗi 15_Cache: CACHEABLE - Thiếu Caching cho Danh mục Tĩnh
* **Bối Cảnh & Hậu Quả**: Mỗi lần User vào trang Refresh, microservices lại thực hiện câu lệnh SQL nén I/O CSDL để lấy lại danh sách Client và Instance. Hệ thống thiếu cơ chế Cache ngắn hạn làm chậm thời gian phản hồi.
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Thêm `@EnableCaching` và cấu hình `@Cacheable(value = "clients", key = "#id")` cho các API đọc danh mục Khách hàng (`Client`) và Máy chủ (`Instance`), kèm `@CacheEvict` khi `UPDATE`/`DELETE`. Đính kèm HTTP Header `Cache-Control: max-age=60, private`.

#### 📌 Lỗi 20: INTEROPERABILITY - Thiếu Swagger API Documentation ở các Service
* **Bối Cảnh & Hậu Quả**: Một số Controller thiếu OpenAPI 3.0 annotations (`@Operation`, `@Tag`), khiến người phát triển Frontend và đối tác tích hợp khó tra cứu cấu trúc Hợp đồng API (API Contract).
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Bổ sung OpenAPI 3.0 / Swagger Annotations (`@Tag`, `@Operation`, `@Parameter`) cho `InstanceController` và `ClientController`.

#### 📌 Lỗi 14: Trùng lặp class `ApiResponse` không dùng thư viện chung `common-lib`
* **Bối Cảnh & Hậu Quả**: Các microservice tự định nghĩa class `ApiResponse` riêng lẻ trong package nội bộ thay vì sử dụng chung class chuẩn trong `common-lib`. Điều này làm lệch cấu trúc JSON Envelope giữa các dịch vụ và tốn công bảo trì.
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Gỡ bỏ các class `ApiResponse` tự viết ở `llm-service`, `instance-service`, thống nhất import `com.techvalley.monitor.common.dto.ApiResponse` từ module `common-lib`.

#### 📌 Lỗi 11: Thiếu cấu hình CORS cho Web Frontend
* **Bối Cảnh & Hậu Quả**: Microservices chưa được cấu hình `CorsFilter`. Khi ứng dụng Web Frontend (React/Vite chạy trên `localhost:3000` hoặc `localhost:5173`) gửi AJAX request sang API Gateway, trình duyệt web sẽ chặn hoàn toàn do vi phạm Same-Origin Policy (SOP).
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Thêm `CorsFilter` ở `auth-gateway-service` và các Microservices cho phép Frontend (`localhost:3000`, `localhost:5173`) gọi API không bị trình duyệt chặn.

#### 📌 Lỗi 12: Thiếu Validation `@Max(100)` cho chỉ số CPU Usage
* **Bối Cảnh & Hậu Quả**: DTO `InstanceRequest` thiếu annotation `@Max(100)` và `@Min(0)`. Người dùng có thể truyền chỉ số CPU âm hoặc vượt quá 100% (ví dụ `cpuUsage = 999`), làm sai lệch toàn bộ báo cáo thống kê và biểu đồ Monitoring.
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Thêm `@Max(value = 100)` vào `InstanceRequest` và `InstanceStatusUpdateRequest`.

#### 📌 Lỗi 13: Lỗi Crash 500 khi truyền sai tham số Sort (`sortBy`)
* **Bối Cảnh & Hậu Quả**: Các API danh sách nhận tham số `sortBy` từ Query String nhưng không validate. Nếu client truyền tên biến không tồn tại trong Entity (ví dụ `sortBy=invalid_field`), Spring Data JPA ném `PropertyReferenceException` và gây crash trả về `500 Server Error`.
* **Trạng thái**: ⏳ *Giai đoạn 3*
* **Hướng sửa**: Viết hàm Validate tham số `sortBy` tự động fallback về trường ngày mặc định nếu truyền sai tên biến.

---

## 🚀 Lộ Trình Sửa Chữa Chi Tiết Theo 4 Giai Đoạn

* **GIAI ĐOẠN 1**: Core Architecture, Dynamic Docker Network & Scalability (Lỗi 1, 2, 3, 4, 5, 18, 19) $\rightarrow$ ✅ **ĐÃ HOÀN THÀNH 100%**
* **GIAI ĐOẠN 2**: CONSISTENCY & Logic Nghiệp Vụ RBAC (Lỗi 6, 7, 8, 9, 10, 15, 16, 17) $\rightarrow$ ⏳ **ĐANG THỰC THI (Đã xong Lỗi 16 & 17)**
* **GIAI ĐOẠN 3**: CACHEABLE, INTEROPERABILITY & Quality (Lỗi 11, 12, 13, 14, 15_Cache, 20) $\rightarrow$ ⏳ **CHỜ THỰC THI**
* **GIAI ĐOẠN 4**: Kiểm Thử Hệ Thống & Verification $\rightarrow$ ⏳ **GIAI ĐOẠN CUỐI CÙNG**
