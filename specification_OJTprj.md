# TÀI LIỆU ĐẶC TẢ KỸ THUẬT (TECHNICAL SPECIFICATION)
## Dự án: Cloud Instance Monitoring System - TechValley (OJT Developer Track)

---

## 1. TỔNG QUAN HỆ THỐNG

### 1.1. Mục tiêu
Hệ thống **Cloud Instance Monitoring System** của TechValley được xây dựng dưới dạng **RESTful API**, đóng vai trò là nền tảng quản lý, giám sát hạ tầng máy chủ ảo (Cloud Instances), tự động phát hiện và cảnh báo các bất thường kỹ thuật, tính toán chỉ số chất lượng dịch vụ (SLA), dự báo chi phí vận hành cho khách hàng và tích hợp tính năng Trí tuệ nhân tạo (LLM Feature) hỗ trợ phân tích/tối ưu vận hành.

### 1.2. Công nghệ & Mô hình Triển khai Đề xuất
- **Mô hình triển khai ứng dụng:** **Đơn bản Server tập trung (Centralized Single Backend REST API Service)**.
  - *Giải đáp kiến trúc:* **KHÔNG** cần tách thành 2 bản app riêng biệt (Server riêng / Client riêng). Tất cả người dùng (`ADMIN` lẫn `CLIENT_MANAGER`) đều kết nối chung tới **1 REST API Backend Server duy nhất**. Sự phân biệt quyền hạn và truy cập dữ liệu được xử lý tập trung ở tầng Backend qua **JWT Token & Role-Based Access Control (RBAC)**.
- **Kiến trúc hệ thống:** RESTful Web API (Layered Architecture: Controller - Service - Repository - Entity).
- **Xác thực & Phân quyền:** JSON Web Token (JWT) Stateless Authentication + Role-Based Access Control (RBAC).
- **Cơ sở dữ liệu & Công cụ:** **Microsoft SQL Server (MS SQL Server)**.
  - *Công cụ quản trị:* **SQL Server Management Studio (SSMS)** được sử dụng làm GUI tool chính để thiết kế Schema, xem sơ đồ quan hệ ERD (Database Diagram), quản lý chỉ mục và truy vấn T-SQL.
- **Tài liệu API:** OpenAPI 3.0 / Swagger Documentation.
- **Tích hợp LLM:** OpenAI API / Anthropic Claude API / Gemini API wrapper service.

---

## 2. QUẢN LÝ VAI TRÒ VÀ PHÂN QUYỀN (RBAC)

Hệ thống định nghĩa 2 vai trò chính với các mức truy cập dữ liệu như sau:

| Vai trò (Role) | Mô tả quyền hạn |
| :--- | :--- |
| **`ADMIN`** | Quản trị toàn bộ hệ thống. Có toàn quyền CRUD trên tất cả Resource (Members, Clients, Instances, Alerts, Monitoring, Reports, LLM Features). |
| **`CLIENT_MANAGER`** | Quản lý phụ trách khách hàng. Chỉ có quyền truy cập và thao tác với các `Clients` mà mình được phân công (`managerId == currentUserId`), cùng các `Instances`, `Alerts`, `Cost`, `SLA` liên quan đến các Client đó. |

### Cơ chế Xác thực
- Tất cả request (ngoại trừ `POST /api/auth/login`) đều yêu cầu HTTP Header:
  ```http
  Authorization: Bearer <JWT_TOKEN>
  ```
- Token chứa thông tin `memberId`, `username`, `role` và thời gian hết hạn (`exp`).

---

## 3. THIẾT KẾ CƠ SỞ DỮ LIỆU (DATABASE SCHEMA SPECIFICATION FOR MS SQL SERVER / SSMS)

> **Ghi chú SSMS:** Toàn bộ bảng, chỉ mục (Index) và quan hệ khóa ngoại (Foreign Keys) dưới đây được quản lý và trực quan hóa trực tiếp trên **SQL Server Management Studio (SSMS)** qua tính năng *Database Diagrams*.

### 3.1. Bảng `members` (Tài khoản người dùng hệ thống)
Lưu trữ thông tin tài khoản nhân sự TechValley (Admin & Client Manager).

| Tên trường (Column) | Kiểu dữ liệu (MS SQL Server) | Ràng buộc (Constraints) | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `IDENTITY(1,1)` | Định danh duy nhất người dùng |
| `username` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | Tên đăng nhập |
| `password` | `VARCHAR(255)` | `NOT NULL` | Mật khẩu đã được băm (BCrypt/Argon2) |
| `fullName` | `NVARCHAR(100)` | `NOT NULL` | Họ và tên người dùng (hỗ trợ Unicode) |
| `role` | `VARCHAR(20)` | `NOT NULL` | Vai trò: `'ADMIN'`, `'CLIENT_MANAGER'` |
| `createdAt` | `DATETIME2` | `DEFAULT GETDATE()` | Thời gian tạo tài khoản |

### 3.2. Bảng `clients` (Thông tin Khách hàng)
Lưu trữ thông tin doanh nghiệp/khách hàng thuê hạ tầng Cloud.

| Tên trường (Column) | Kiểu dữ liệu (MS SQL Server) | Ràng buộc (Constraints) | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `IDENTITY(1,1)` | Định danh duy nhất khách hàng |
| `name` | `NVARCHAR(100)` | `NOT NULL` | Tên khách hàng / Công ty |
| `email` | `VARCHAR(100)` | `NOT NULL`, `UNIQUE` | Email liên hệ chính |
| `company` | `NVARCHAR(100)` | `NULLABLE` | Tên doanh nghiệp đại diện |
| `contractPlan` | `VARCHAR(20)` | `NOT NULL` | Gói hợp đồng: `'BASIC'`, `'STANDARD'`, `'ENTERPRISE'` |
| `managerId` | `BIGINT` | `NOT NULL`, `FK -> members(id)` | Quản lý phụ trách khách hàng |
| `createdAt` | `DATETIME2` | `DEFAULT GETDATE()` | Thời gian khởi tạo dữ liệu |

### 3.3. Bảng `instances` (Thông tin Máy chủ ảo / Cloud Instance)
Lưu trữ hạ tầng máy chủ của từng khách hàng.

| Tên trường (Column) | Kiểu dữ liệu (MS SQL Server) | Ràng buộc (Constraints) | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `IDENTITY(1,1)` | Định danh duy nhất instance |
| `clientId` | `BIGINT` | `NOT NULL`, `FK -> clients(id)` | Khách hàng sở hữu instance |
| `name` | `VARCHAR(100)` | `NOT NULL` | Tên gợi nhớ của instance |
| `region` | `VARCHAR(50)` | `NOT NULL` | Vùng máy chủ (e.g. `'ap-southeast-1'`, `'us-east-1'`) |
| `type` | `VARCHAR(50)` | `NOT NULL` | Cấu hình máy chủ (e.g. `'t3.medium'`, `'c5.xlarge'`) |
| `status` | `VARCHAR(20)` | `NOT NULL` | Trạng thái: `'RUNNING'`, `'STOPPED'`, `'ERROR'` |
| `cpuUsage` | `FLOAT` | `NOT NULL`, `DEFAULT 0.0` | Tỷ lệ sử dụng CPU hiện tại (0.0% - 100.0%) |
| `monthlyCost` | `DECIMAL(10,2)` | `NOT NULL`, `DEFAULT 0.00` | Chi phí định mức hàng tháng ($ USD) |
| `lastUpdated` | `DATETIME2` | `DEFAULT GETDATE()` | Thời điểm cập nhật chỉ số gần nhất |
| `createdAt` | `DATETIME2` | `DEFAULT GETDATE()` | Thời điểm khởi tạo instance |

### 3.4. Bảng `alerts` (Nhật ký Cảnh báo Hệ thống)
Lưu lịch sử các cảnh báo phát sinh từ máy chủ.

| Tên trường (Column) | Kiểu dữ liệu (MS SQL Server) | Ràng buộc (Constraints) | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `IDENTITY(1,1)` | Định danh cảnh báo |
| `instanceId` | `BIGINT` | `NOT NULL`, `FK -> instances(id)` | Instance phát sinh cảnh báo |
| `type` | `VARCHAR(30)` | `NOT NULL` | Loại cảnh báo: `'HIGH_CPU'`, `'SYSTEM_ERROR'`, `'LONG_STOPPED'` |
| `severity` | `VARCHAR(20)` | `NOT NULL` | Mức độ nghiêm trọng: `'INFO'`, `'WARNING'`, `'CRITICAL'` |
| `message` | `NVARCHAR(MAX)` | `NOT NULL` | Nội dung mô tả chi tiết sự cố |
| `isResolved` | `BIT` | `NOT NULL`, `DEFAULT 0` | Trạng thái xử lý (`1`: Đã xử lý, `0`: Chưa xử lý) |
| `createdAt` | `DATETIME2` | `DEFAULT GETDATE()` | Thời gian phát sinh cảnh báo |
| `resolvedAt` | `DATETIME2` | `NULLABLE` | Thời gian đánh dấu hoàn tất xử lý |

### 3.5. Bảng `cost_snapshots` (Lịch sử Đóng băng Chi phí)
Lưu tổng chi phí hàng tháng của từng client phục vụ thống kê & dự báo.

| Tên trường (Column) | Kiểu dữ liệu (MS SQL Server) | Ràng buộc (Constraints) | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `IDENTITY(1,1)` | Định danh snapshot |
| `clientId` | `BIGINT` | `NOT NULL`, `FK -> clients(id)` | Mã khách hàng |
| `yearMonth` | `VARCHAR(7)` | `NOT NULL` | Tháng ghi nhận dạng `'YYYY-MM'` (e.g. `'2026-07'`) |
| `totalCost` | `DECIMAL(10,2)` | `NOT NULL` | Tổng chi phí trong tháng ($ USD) |
| `recordedAt` | `DATETIME2` | `DEFAULT GETDATE()` | Thời điểm chốt dữ liệu |

---

## 4. CHUẨN ĐỊNH DẠNG REQUEST / RESPONSE (API ENVELOPE)

Tất cả API trong hệ thống đều tuân thủ cấu trúc JSON đồng nhất:

### Response Thành công (HTTP 200 / 201)
```json
{
  "success": true,
  "code": 200,
  "message": "Thao tác thành công",
  "data": { ... },
  "timestamp": "2026-07-21T15:00:00Z"
}
```

### Response Thất bại / Lỗi (HTTP 4xx / 5xx)
```json
{
  "success": false,
  "code": 400,
  "message": "Thông tin đầu vào không hợp lệ",
  "errors": [
    {
      "field": "cpuUsage",
      "message": "Giá trị cpuUsage phải nằm trong khoảng từ 0 đến 100"
    }
  ],
  "timestamp": "2026-07-21T15:00:00Z"
}
```

---

## 5. CHI TIẾT ĐẶC TẢ API (API ENDPOINTS SPECIFICATION)

### 5.1. Nhóm Auth API

#### `POST /api/auth/login`
- **Mô tả:** Đăng nhập hệ thống lấy JWT Access Token.
- **Phân quyền:** Public (Không yêu cầu Token).
- **Request Body:**
  ```json
  {
    "username": "admin_techvalley",
    "password": "SecretPassword123!"
  }
  ```
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Đăng nhập thành công",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 86400,
      "user": {
        "id": 1,
        "username": "admin_techvalley",
        "fullName": "Nguyen Van A",
        "role": "ADMIN"
      }
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```
- **Response Error (401 Unauthorized):** Tên đăng nhập hoặc mật khẩu không đúng.

---

### 5.2. Nhóm Client API

#### `POST /api/clients`
- **Mô tả:** Tạo mới khách hàng.
- **Phân quyền:** `ADMIN`.
- **Request Body:**
  ```json
  {
    "name": "Công ty TNHH Giải Pháp Việt",
    "email": "contact@viet solution.vn",
    "company": "VietSolution Corp",
    "contractPlan": "ENTERPRISE",
    "managerId": 2
  }
  ```
- **Response Success (201 Created):** Trả về đối tượng Client vừa tạo.

#### `GET /api/clients`
- **Mô tả:** Lấy danh sách khách hàng (có phân trang & lọc).
- **Phân quyền:** `ADMIN` (xem tất cả), `CLIENT_MANAGER` (chỉ xem các client có `managerId == currentUserId`).
- **Query Parameters:** `page` (default: 1), `size` (default: 10), `search` (tùy chọn tên/email), `managerId` (tùy chọn).
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Lấy danh sách khách hàng thành công",
    "data": {
      "items": [
        {
          "id": 10,
          "name": "Công ty TNHH Giải Pháp Việt",
          "email": "contact@vietsolution.vn",
          "company": "VietSolution Corp",
          "contractPlan": "ENTERPRISE",
          "managerId": 2,
          "createdAt": "2026-07-21T10:00:00Z"
        }
      ],
      "pagination": {
        "currentPage": 1,
        "pageSize": 10,
        "totalElements": 1,
        "totalPages": 1
      }
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```

#### `GET /api/clients/{id}/instances`
- **Mô tả:** Lấy tất cả instance thuộc sở hữu của 1 Client.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý Client đó.
- **Response Success (200 OK):** Danh sách các instance thuộc Client ID.

#### `GET /api/clients/{id}/cost`
- **Mô tả:** Lấy tổng chi phí hạ tầng tháng hiện tại của Client.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý Client đó.
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Lấy dữ liệu chi phí thành công",
    "data": {
      "clientId": 10,
      "currentMonth": "2026-07",
      "totalInstances": 5,
      "runningCost": 1250.00,
      "stoppedCost": 150.00,
      "totalMonthlyCost": 1400.00
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```

#### `GET /api/clients/{id}/cost-forecast`
- **Mô tả:** Dự báo chi phí phát sinh cuối tháng của Client dựa trên trạng thái instance hiện tại.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý Client đó.
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Tính toán dự báo chi phí thành công",
    "data": {
      "clientId": 10,
      "currentSpent": 850.00,
      "projectedSpentEndOfMonth": 1450.00,
      "activeRunningInstances": 4,
      "recommendation": "Dự báo chi phí trong tầm kiểm soát."
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```

#### `GET /api/clients/{id}/sla`
- **Mô tả:** Tính toán chỉ số cam kết chất lượng dịch vụ (SLA %) trong tháng của Client.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý Client đó.
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Tính toán chỉ số SLA thành công",
    "data": {
      "clientId": 10,
      "month": "2026-07",
      "slaPercentage": 99.85,
      "targetSla": 99.90,
      "status": "WARNING",
      "totalDowntimeHours": 1.1
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```

---

### 5.3. Nhóm Instance API

#### `POST /api/instances`
- **Mô tả:** Đăng ký / Tạo mới instance.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` (chỉ tạo cho client thuộc quyền quản lý).
- **Request Body:**
  ```json
  {
    "clientId": 10,
    "name": "prod-web-server-01",
    "region": "ap-southeast-1",
    "type": "c5.xlarge",
    "status": "RUNNING",
    "cpuUsage": 45.5,
    "monthlyCost": 350.00
  }
  ```
- **Response Success (201 Created):** Thông tin Instance đã được khởi tạo.

#### `GET /api/instances`
- **Mô tả:** Xem danh sách instance với phân trang, lọc và sắp xếp.
- **Phân quyền:** `ADMIN` (xem toàn bộ), `CLIENT_MANAGER` (chỉ xem instance thuộc Client quản lý).
- **Query Parameters:** `page`, `size`, `clientId`, `status` (`RUNNING`/`STOPPED`/`ERROR`), `region`, `sortBy` (`cpuUsage`/`monthlyCost`/`createdAt`), `sortOrder` (`asc`/`desc`).
- **Response Success (200 OK):** Trả về danh sách instance thỏa mãn điều kiện.

#### `GET /api/instances/{id}`
- **Mô tả:** Xem thông tin chi tiết của 1 instance.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý client sở hữu instance.
- **Response Success (200 OK):** Trả về thông tin chi tiết instance.

#### `PATCH /api/instances/{id}/status`
- **Mô tả:** Cập nhật trạng thái và chỉ số CPU của instance.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý instance đó.
- **Request Body:**
  ```json
  {
    "status": "ERROR",
    "cpuUsage": 92.4
  }
  ```
- **Response Success (200 OK):** Cập nhật trạng thái thành công.

#### `DELETE /api/instances/{id}`
- **Mô tả:** Xóa instance khỏi hệ thống.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` quản lý instance đó.
- **Quy tắc kiểm tra nghiệp vụ (Business Rule):**
  - **KHÔNG ĐƯỢC XÓA** instance có trạng thái `RUNNING`. Trả về `HTTP 400 Bad Request`.
  - Chỉ cho phép xóa khi instance ở trạng thái `STOPPED` hoặc `ERROR`.
- **Response Error (400 Bad Request):**
  ```json
  {
    "success": false,
    "code": 400,
    "message": "Không thể xóa máy chủ đang ở trạng thái RUNNING. Vui lòng tắt máy chủ (STOPPED) trước khi xóa.",
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```
- **Response Success (200 OK):** Xóa thành công.

---

### 5.4. Nhóm Monitoring API

#### `GET /api/monitor/warnings`
- **Mô tả:** Quét hệ thống và trả về danh sách các instance có cảnh báo CPU cao (`cpuUsage >= 80.0%`).
- **Tác dụng phụ (Side-effect logic):** Tự động khởi tạo 1 Alert loại `HIGH_CPU` với mức độ `WARNING` vào bảng `alerts` nếu instance chưa có Alert chưa xử lý (`isResolved == false`).

#### `GET /api/monitor/errors`
- **Mô tả:** Quét hệ thống và trả về danh sách các instance đang bị lỗi (`status == 'ERROR'`).
- **Tác dụng phụ (Side-effect logic):** Tự động khởi tạo 1 Alert loại `SYSTEM_ERROR` với mức độ `CRITICAL` vào bảng `alerts` nếu instance chưa có Alert chưa xử lý (`isResolved == false`).

#### `GET /api/monitor/long-stopped`
- **Mô tả:** Lấy danh sách các instance bị tạm dừng (`status == 'STOPPED'`) liên tục hơn 7 ngày mà chưa bật lại.

#### `GET /api/monitor/report`
- **Mô tả:** Báo cáo tổng quan tình trạng hạ tầng (Tổng số instance, số lượng RUNNING/STOPPED/ERROR, CPU trung bình toàn hệ thống, tổng số alert chưa xử lý).

---

### 5.5. Nhóm Alert API

#### `GET /api/alerts`
- **Mô tả:** Lấy lịch sử cảnh báo hệ thống.
- **Phân quyền:** `ADMIN` (xem tất cả), `CLIENT_MANAGER` (chỉ xem alert từ các instance thuộc client quản lý).
- **Query Parameters:** `isResolved` (`true`/`false`), `severity`, `instanceId`, `page`, `size`.

#### `PATCH /api/alerts/{id}/resolve`
- **Mô tả:** Đánh dấu cảnh báo đã được quản trị viên/kỹ thuật viên xử lý xong.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER` phụ trách.
- **Cập nhật dữ liệu:** Chuyển `isResolved = true` và ghi nhận `resolvedAt = CURRENT_TIMESTAMP`.

---

### 5.6. Nhóm LLM Feature API (Chọn 1 trong 3 tính năng AI)

#### Lựa chọn triển khai chính: `GET /api/instances/{id}/diagnosis` (Tự động Chẩn đoán Sự cố bằng AI)
- **Mô tả:** Gửi dữ liệu chỉ số (Status, CPU usage, lịch sử Alert) của Instance sang Mô hình ngôn ngữ lớn (LLM) để phân tích nguyên nhân gốc rễ và đề xuất giải pháp xử lý kỹ thuật.
- **Phân quyền:** `ADMIN` hoặc `CLIENT_MANAGER`.
- **Response Success (200 OK):**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Phân tích chẩn đoán từ AI hoàn tất",
    "data": {
      "instanceId": 15,
      "instanceName": "payment-service-prod",
      "healthScore": 45,
      "diagnosis": "Máy chủ đang gặp tình trạng nghẽn CPU (98.5%) kết hợp với lỗi 5xx kéo dài.",
      "rootCause": "Nhiều khả năng xảy ra hiện tượng Memory Leak hoặc bị tấn công DDoS lớp ứng dụng.",
      "actionableSteps": [
        "1. Khởi động lại dịch vụ payment-service trên container.",
        "2. Nâng cấp cấu hình từ c5.xlarge lên c5.2xlarge.",
        "3. Kích hoạt Rate Limiting trên API Gateway."
      ]
    },
    "timestamp": "2026-07-21T15:00:00Z"
  }
  ```

---

## 6. QUY TẮC NGHIỆP VỤ (BUSINESS LOGIC RULES)

1. **Cơ chế chống trùng lặp Alert (Alert Deduplication Check):**
   - Trước khi tạo tự động một Alert mới (`HIGH_CPU` hoặc `SYSTEM_ERROR`), hệ thống phải kiểm tra xem Instance đó đã có Alert cùng loại đang ở trạng thái chưa giải quyết (`isResolved = false`) hay chưa.
   - Nếu đã tồn tại Alert chưa giải quyết, hệ thống **KHÔNG được tạo thêm** Alert mới để tránh trôi ngập nhật ký (alert flooding).

2. **Công thức tính Dự báo Chi phí (Cost Forecast Formula):**
   $$\text{ForecastCost} = \text{SpentSoFar} + \left( \sum_{\text{running\_instances}} \frac{\text{monthlyCost}}{\text{totalDaysInMonth}} \times \text{remainingDays} \right)$$
   - Chỉ tính toán trên các Instance có trạng thái `RUNNING`. Instance `STOPPED` sẽ không phát sinh phí hạ tầng chạy theo giờ.

3. **Công thức tính Chỉ số SLA (%):**
   $$\text{SLA (\%)} = \left( \frac{\text{Tổng số giờ instance duy trì RUNNING}}{\text{Tổng số instance} \times \text{Tổng số giờ trong tháng}} \right) \times 100\%$$
   - Khi `status == 'ERROR'` hoặc `'STOPPED'` (ngoài lịch bảo trì), khoảng thời gian đó được tính vào Downtime.

4. **Ràng buộc Xóa Instance (Instance Deletion Policy):**
   - Không cho phép xóa instance `RUNNING`. Hệ thống sẽ chặn ở tầng Service và ném ngoại lệ `InvalidOperationException` -> HTTP 400 Bad Request.

---

## 7. PHÂN CHIA CÔNG VIỆC THEO CHỨC NĂNG (TASK ALLOCATION BY FEATURE MODULES)

> **Ghi chú phân công tài liệu & demo:** 
> - **Tài liệu Đặc tả Kỹ thuật (Specification):** Trưởng nhóm chịu trách nhiệm biên soạn chính và tổng hợp.
> - **Slide PPT & Báo cáo:** Cả nhóm cùng tham gia làm (mỗi thành viên phụ trách trình bày nội dung cho mô-đun chức năng mình đảm nhận, Member E chịu trách nhiệm tổng hợp slide PPT và kịch bản Demo).

| Thành viên | Mô-đun Chức năng Đảm nhận | Chi tiết Công việc & Các API Phụ trách |
| :--- | :--- | :--- |
| **Member A** | **Chức năng 1: Xác thực & Phân quyền (Auth & Security)** | • **DB Lead:** Thiết kế bảng `members` & sơ đồ ERD tổng thể.<br>• **Xác thực:** Triển khai API `POST /api/auth/login` (JWT Token generation & verification).<br>• **Bảo mật:** Triển khai JWT Authentication Filter & Phân quyền Role-Based Access Control (`ADMIN`, `CLIENT_MANAGER`).<br>• **Tài liệu:** Đóng góp báo cáo/slide phần ERD & Security. |
| **Member B** | **Chức năng 2: Quản lý Khách hàng (Client Management)** | • **Quản lý dữ liệu Client:** Triển khai API Tạo mới (`POST /api/clients`), Xem danh sách với Phân trang, Tìm kiếm & Lọc (`GET /api/clients`).<br>• **Quản lý quan hệ:** Triển khai API Lấy danh sách instance thuộc client (`GET /api/clients/{id}/instances`).<br>• **Phân quyền:** Cấu hình logic giới hạn dữ liệu theo `managerId`.<br>• **Tài liệu:** Đóng góp báo cáo/slide phần Client Management. |
| **Member C** | **Chức năng 3: Quản lý Máy chủ ảo (Instance Management)** | • **CRUD Instance:** Triển khai API Tạo mới (`POST /api/instances`), Danh sách (`GET /api/instances`), Chi tiết (`GET /api/instances/{id}`).<br>• **Điều khiển & Validate:** Triển khai Cập nhật trạng thái/CPU (`PATCH /api/instances/{id}/status`) và Xóa instance (`DELETE /api/instances/{id}`).<br>• **Business Rule:** Logic chặn xóa instance đang `RUNNING` (HTTP 400).<br>• **Tài liệu:** Đóng góp báo cáo/slide phần Instance Management. |
| **Member D** | **Chức năng 4: Giám sát Hạ tầng & Quản lý Cảnh báo (Monitoring & Alert System)** | • **Giám sát tự động:** Triển khai Monitoring API (`GET /api/monitor/warnings`, `/errors`, `/long-stopped`, `/report`).<br>• **Quản lý Cảnh báo:** Triển khai Alert API (`GET /api/alerts`, `PATCH /api/alerts/{id}/resolve`).<br>• **Business Rule:** Logic tự động phát sinh Alert (`HIGH_CPU`, `SYSTEM_ERROR`) & Logic chống trùng lặp Alert (Alert Deduplication Check).<br>• **Tài liệu:** Đóng góp báo cáo/slide phần Monitoring & Alerts. |
| **Member E** | **Chức năng 5: Phân tích Chi phí, SLA & Chẩn đoán AI (Cost, SLA & LLM Feature)** | • **Phân tích Tài chính & Dịch vụ:** Triển khai API `GET /api/clients/{id}/cost`, Dự báo chi phí (`cost-forecast`) và Tính tỷ lệ `SLA`.<br>• **AI Integration:** Triển khai Chẩn đoán sự cố tự động bằng LLM (`GET /api/instances/{id}/diagnosis`).<br>• **Hệ thống & Demo:** Cấu hình Swagger/OpenAPI UI, Quản lý Git Repository (Merge/PR) và Tổng hợp Slide PPT & Kịch bản Demo. |

---

## 8. QUY TRÌNH QUẢN LÝ NGUỒN MÃ NGUỒN (GIT FLOW STANDARD)

### 8.1. Cấu trúc Nhánh (Branching Strategy)
- **`main` (Production / Final Demo Branch):** 
  - Nhánh chứa mã nguồn **ổn định nhất 100%**, sẵn sàng đem đi báo cáo, nộp bài hoặc Demo.
  - **Tuyệt đối KHÔNG push code trực tiếp vào `main`**. Chỉ gộp (merge) từ nhánh `develop` sang `main` sau khi đã review và kiểm thử hệ thống chạy ổn định.
- **`develop` / `dev` (Integration Branch):**
  - Nhánh tích hợp chung của cả nhóm. Tất cả tính năng sau khi thành viên làm xong sẽ được tạo Pull Request (PR) để gộp vào nhánh này.
  - Nhóm sẽ chạy test API, kiểm tra xung đột (conflict) và kiểm thử liên mô-đun trên nhánh `develop`.
- **`feature/{member-task}` (Feature Branches):**
  - Nhánh cá nhân riêng cho từng nhiệm vụ (VD: `feature/auth-jwt`, `feature/instance-crud`, `feature/llm-diagnosis`).
  - Thành viên làm việc độc lập trên nhánh này, tránh gây ảnh hưởng tới người khác.

### 8.2. Quy trình làm việc & Duyệt Code (Workflow & Pull Request)
1. **Làm việc cá nhân:** Thành viên checkout từ `develop` ra nhánh `feature/...` của mình để viết code.
2. **Push & Tạo PR:** Khi hoàn thành, push nhánh `feature/...` lên Remote Repository và mở **Pull Request (PR) vào nhánh `develop`**.
3. **Review & Merge vào `develop`:** Trưởng nhóm (hoặc ít nhất 1 Teammate) thực hiện Review Code, kiểm tra logic. Nếu đạt yêu cầu thì Approve và Merge vào `develop`.
4. **Kiểm thử hệ thống (Testing trên `develop`):** Cả nhóm test lại các luồng API trên nhánh `develop` (Swagger, Postman, kết nối CSDL SQL Server).
5. **Release lên `main`:** Khi toàn bộ các tính năng trên `develop` đã ổn định và OK, Trưởng nhóm mở PR gộp từ `develop` -> `main` để chốt phiên bản Demo chính thức.

### 8.3. Chuẩn Commit Message (Conventional Commits)
- `feat: add jwt login api`
- `fix: prevent deleting running instance`
- `docs: update swagger endpoints`
- `refactor: optimize cost calculation service`

---

## 9. CHECKLIST KIỂM THỬ VÀ NỘP BÀI (ACCEPTANCE CRITERIA)

- [x] Tài liệu Đặc tả Specification đầy đủ schema, API, logic và phân công.
- [ ] Database ERD khớp 100% với định nghĩa các Entity trong code.
- [ ] API Đăng nhập thành công trả về JWT Bearer Token hợp lệ.
- [ ] Phân quyền RBAC chính xác (`ADMIN` xem được tất cả, `CLIENT_MANAGER` chỉ thấy client mình quản lý).
- [ ] API `GET /api/monitor/warnings` và `errors` tự động tạo Alert đúng logic chống trùng lặp.
- [ ] Không thể xóa Instance khi ở trạng thái `RUNNING` (Trả về lỗi HTTP 400 rõ ràng).
- [ ] Công thức tính SLA và Cost Forecast hoạt động khớp với dữ liệu giả lập.
- [ ] Endpoint LLM Diagnosis hoạt động tốt và trả về gợi ý khắc phục sự cố hợp lý.
- [ ] Swagger UI hiển thị đầy đủ thông tin tất cả endpoint và có thể test trực tiếp.
- [ ] Git commit history rõ ràng, không có commit rác, đúng flow nhánh `develop` & `main`.
