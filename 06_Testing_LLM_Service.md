# Testing LLM Service

## 1. Tổng quan Module LLM (llm-service)
- **Port dịch vụ**: `8085`
- **Chức năng chính**: Tự động thu thập dữ liệu hạ tầng (Telemetry) từ `instance-service` và danh sách cảnh báo gần nhất từ `alert-service` để Google Gemini AI (hoặc Mock Service) phân tích nguyên nhân gốc rễ (Root Cause), chấm điểm sức khỏe (Health Score) và đề xuất các bước xử lý sự cố (Actionable Steps).
- **Cơ chế dự phòng (Fallback)**: Tự động chuyển hướng từ `GeminiLlmServiceImpl` sang `MockLlmServiceImpl` khi Gemini API hết quota, lỗi mạng, hoặc chưa cấu hình `llm.api-key`.
- **Tích hợp RestClient**: `InstanceServiceClient` và `AlertServiceClient` chịu trách nhiệm kết nối trực tiếp đến REST API của `instance-service` (Port 8081) và `alert-service` (Port 8084). 
- **Cấu hình chính trong `application.yml`**:
  - `llm.provider`: `gemini` / `mock`
  - `llm.model`: `gemini-1.5-flash`
  - `llm.mock-enabled`: `true`

---

## 2. Kế hoạch đã Thực thi & Luồng xử lý Chi tiết (Execution Plan & Sequence Flow)

### 2.1. Các hạng mục kiến trúc đã hoàn thành
1. **Kiến trúc Layered & RestClient**:
   - Khởi tạo [InstanceServiceClient](file:///d:/OTJprj_TechValley/llm-service/src/main/java/com/techvalley/llm/client/InstanceServiceClient.java) và [AlertServiceClient](file:///d:/OTJprj_TechValley/llm-service/src/main/java/com/techvalley/llm/client/AlertServiceClient.java) kết nối HTTP REST tới các dịch vụ hạ tầng (`instance-service` & `alert-service`).
2. **Temporary Test Fallback Generator (Lưới an toàn hỗ trợ Test)**:
   - Xây dựng thuật toán sinh dữ liệu giả lập tạm thời theo `instanceId % 4` trong khối `catch` ở cả 2 Client để đảm bảo quá trình test AI diễn ra thuận lợi khi 2 service hạ tầng chưa khởi động.
3. **Phân tích AI kép (Dual LLM Service Provider)**:
   - [GeminiLlmServiceImpl](file:///d:/OTJprj_TechValley/llm-service/src/main/java/com/techvalley/llm/service/impl/GeminiLlmServiceImpl.java): Xây dựng System Prompt chuẩn DevOps/SRE, gửi request tới REST API của Google Gemini 1.5 Flash và parse JSON linh hoạt.
   - [MockLlmServiceImpl](file:///d:/OTJprj_TechValley/llm-service/src/main/java/com/techvalley/llm/service/impl/MockLlmServiceImpl.java): Xử lý phản hồi AI giả lập tức thì khi dịch vụ Gemini gặp sự cố hoặc cờ `mock-enabled=true`.
4. **Chuẩn hóa API Envelope & Exception Handling**:
   - Sử dụng `ApiResponse<InstanceDiagnosisResponse>` cho mọi kết quả trả về.
   - [GlobalExceptionHandler](file:///d:/OTJprj_TechValley/llm-service/src/main/java/com/techvalley/llm/exception/GlobalExceptionHandler.java) bắt các lỗi validation, ép kiểu tham số, lỗi kết nối API.

---

### 2.2. Sơ đồ Luồng xử lý Tuần tự

1. Gửi Yêu cầu: User gửi HTTP GET /api/instances/{id}/diagnosis ➔  LlmController tiếp nhận và chuyển tiếp cho Service.
2. Thu thập dữ liệu: Service gọi HTTP sang instance-service (lấy CPU, status) và alert-service (lấy danh sách lỗi). (Nếu 2 service sập, tự kích hoạt sinh dữ liệu giả lập theo id % 4).
3. Phân tích AI: Ghép dữ liệu gom được vào Prompt gửi sang Google Gemini API (hoặc tự động dùng Mock nếu Gemini lỗi/tắt key) để luận ra Nguyên nhân gốc rễ & Hướng khắc phục.
4. Trả Kết quả: Đóng gói đáp án vào chuẩn JSON ApiResponse (HTTP 200 OK) trả về cho Client.
---

### 2.3. Chi tiết 4 Bước Luồng thực thi

1. **Bước 1: Tiếp nhận Yêu cầu (Request Ingestion)**
   - API Client (Web Dashboard / Postman) gửi HTTP Request `GET /api/instances/{id}/diagnosis`.
   - `LlmController` đón nhận, log thông tin `instanceId` và chuyển giao xử lý cho `LlmServiceImpl.diagnoseInstance(id)`.

2. **Bước 2: Thu thập Dữ liệu Hạ tầng (Telemetry Data Aggregation)**
   - `LlmServiceImpl` lần lượt kích hoạt `InstanceServiceClient` và `AlertServiceClient`.
   - **Luồng Live Data (Chạy chính)**: Thực hiện HTTP REST Client tới `instance-service` (`/api/instances/{id}`) và `alert-service` (`/api/alerts?instanceId={id}`) để đọc thông số thực từ Database.
   - **Luồng Fallback Giả lập Tạm thời (Chỉ dùng khi Test)**: Nếu 2 service trên chưa bật, khối `catch` chủ động sinh dữ liệu telemetry giả lập tạm thời theo thuật toán `id % 4` để không làm đứt đoạn quá trình kiểm thử AI. *(Khi đưa vào vận hành chính thức, khối code giả lập tạm thời này có thể được gỡ bỏ)*.

3. **Bước 3: Xử lý Phân tích Chẩn đoán AI (AI Diagnosis & Fallback Logic)**
   - Dữ liệu `InstanceDto` và `List<AlertDto>` thu thập được truyền làm ngữ cảnh (Context) cho Provider AI:
     - Nếu `llm.provider = gemini`: Dựng System Prompt DevOps, gửi request tới Google Gemini 1.5 Flash API. Nếu Gemini gặp sự cố mà cờ `llm.mock-enabled = true`, hệ thống tự động Fallback sang `MockLlmServiceImpl`.
     - Nếu `llm.provider = mock`: Chạy trực tiếp qua `MockLlmServiceImpl`.

4. **Bước 4: Chuẩn hóa Response trả về (API Envelope Normalization)**
   - Đóng gói DTO kết quả chẩn đoán vào `ApiResponse<InstanceDiagnosisResponse>` tiêu chuẩn (`success`, `code`, `message`, `data`, `timestamp`) và phản hồi HTTP `200 OK` về Client.

---

## 3. Cơ chế Giả lập Dữ liệu Test Tạm thời (Temporary Test Fallback Data)

> [!NOTE]
> **Mục đích chính**: Đây **không phải** là các option tính năng chính thức của sản phẩm. Đây chỉ là **kịch bản giả lập tạm thời khi kiểm thử (Test Fallback)**.
> Trong trường hợp 2 service hạ tầng (`instance-service` và `alert-service`) chưa hoạt động hoặc chưa khởi động, `llm-service` sẽ tự động sinh ra dữ liệu test linh hoạt theo `instanceId % 4` nhằm **đảm bảo quá trình kiểm thử AI diễn ra thuận lợi**.
> Khi quá trình test đã hoàn tất và kết nối các service thực tế đã hoạt động ổn định, phần code sinh dữ liệu giả lập tạm thời này trong khối `catch` **có thể xóa bỏ hoàn toàn**.

Khi 2 service hạ tầng chưa khởi động, thuật toán `instanceId % 4` trong khối `catch` tự động kích hoạt 4 dạng dữ liệu test giả lập tạm thời như sau:

### 🔴 Dạng 0: Máy chủ Quá tải CPU (ID mod 4 = 0, Ví dụ ID: 4, 8, 12, 16, 20...)
- **Dữ liệu Telemetry sinh ra trong khối catch**:
  - `instanceName`: `"db-postgres-primary"` | `instanceType`: `"db.r6g.xlarge"` | `status`: `"RUNNING"` | `cpuUsage`: `88.5%`
  - `alerts`: 2 Cảnh báo chưa xử lý (`HIGH_CPU` - Slow queries, `HIGH_MEMORY` - Connection pool 95%).
- **Kết quả AI trả về (`data` JSON)**:
  ```json
  {
    "instanceId": 4,
    "instanceName": "db-postgres-primary",
    "instanceStatus": "RUNNING",
    "issueType": "HIGH_CPU",
    "healthScore": 60,
    "diagnosis": "[Cảnh báo Tải cao] Máy chủ 'db-postgres-primary' (ID: 4) có tỷ lệ CPU 88.5% tiệm cận ngưỡng quá tải.",
    "rootCause": "Có các truy vấn CSDL chạy chậm (Slow Queries) thiếu index hoặc tiến trình xử lý nền (background worker) chưa tối ưu.",
    "actionableSteps": [
      "1. Phân tích slow query log trong PostgreSQL để tối ưu chỉ mục Index.",
      "2. Mở rộng kích thước connection pool cho CSDL.",
      "3. Theo dõi sát biểu đồ CPU trong 30 phút tới."
    ],
    "isMockResponse": true
  }
  ```

---

### 🟡 Dạng 1: Máy chủ Ngừng hoạt động (ID mod 4 = 1, Ví dụ ID: 1, 5, 9, 13, 17...)
- **Dữ liệu Telemetry sinh ra trong khối catch**:
  - `instanceName`: `"auth-gateway-service"` | `instanceType`: `"t3.medium"` | `status`: `"STOPPED"` | `cpuUsage`: `0.0%`
  - `alerts`: 1 Cảnh báo (`SYSTEM_ERROR` - Container stopped unexpectedly Exit code 137 OOMKilled).
- **Kết quả AI trả về (`data` JSON)**:
  ```json
  {
    "instanceId": 1,
    "instanceName": "auth-gateway-service",
    "instanceStatus": "STOPPED",
    "issueType": "STOPPED",
    "healthScore": 0,
    "diagnosis": "[Cảnh báo Ngừng hoạt động] Máy chủ 'auth-gateway-service' (ID: 1) đang ở trạng thái STOPPED. Dịch vụ không thể tiếp nhận yêu cầu.",
    "rootCause": "Tiến trình container bị ngừng đột ngột do lỗi gạt cầu chì (OOMKilled) hoặc lệnh dừng chủ động từ quản trị viên.",
    "actionableSteps": [
      "1. Kiểm tra log hệ thống (`docker logs` / `journalctl`) để tìm nguyên nhân dừng đột ngột.",
      "2. Thực hiện khởi động lại máy chủ (Start Instance) từ màn hình quản trị Dashboard.",
      "3. Kiểm tra hạn mức bộ nhớ RAM allocation cho tiến trình ứng dụng."
    ],
    "isMockResponse": true
  }
  ```

---

### 🟢 Dạng 2: Máy chủ Vận hành An toàn / Khỏe mạnh (ID mod 4 = 2, Ví dụ ID: 2, 6, 10, 14, 18...)
- **Dữ liệu Telemetry sinh ra trong khối catch**:
  - `instanceName`: `"client-portal-worker"` | `instanceType`: `"t3.small"` | `status`: `"RUNNING"` | `cpuUsage`: `24.5%`
  - `alerts`: `[]` (Không có cảnh báo nào).
- **Kết quả AI trả về (`data` JSON)**:
  ```json
  {
    "instanceId": 2,
    "instanceName": "client-portal-worker",
    "instanceStatus": "RUNNING",
    "issueType": "NONE",
    "healthScore": 95,
    "diagnosis": "[Hoạt động Tốt] Máy chủ 'client-portal-worker' (ID: 2) vận hành ổn định. Tỷ lệ CPU 24.5%, không có cảnh báo nào chưa xử lý.",
    "rootCause": "Tất cả chỉ số telemetry hạ tầng và ứng dụng đều nằm trong khoảng an toàn cho phép.",
    "actionableSteps": [
      "1. Duy trì cơ chế kiểm tra sức khỏe (Healthcheck) định kỳ.",
      "2. Thực hiện sao lưu dữ liệu (Backup) định kỳ theo SLA."
    ],
    "isMockResponse": true
  }
  ```

---

### 🚨 Dạng 3: Máy chủ Lỗi Nghiêm trọng / Quá tải Nguy hiểm (ID mod 4 = 3, Ví dụ ID: 3, 7, 11, 15, 19...)
- **Dữ liệu Telemetry sinh ra trong khối catch**:
  - `instanceName`: `"payment-service-prod"` | `instanceType`: `"c5.xlarge"` | `status`: `"ERROR"` | `cpuUsage`: `96.8%`
  - `alerts`: 2 Cảnh báo nguy cấp (`SYSTEM_ERROR` - HTTP 504 Gateway Timeout, `HIGH_CPU` - CPU critical 96.8%).
- **Kết quả AI trả về (`data` JSON)**:
  ```json
  {
    "instanceId": 3,
    "instanceName": "payment-service-prod",
    "instanceStatus": "ERROR",
    "issueType": "CRITICAL",
    "healthScore": 25,
    "diagnosis": "[Sự cố Nghiêm trọng] Máy chủ 'payment-service-prod' (ID: 3) gặp lỗi nguy hiểm với CPU 96.8% và 2 cảnh báo chưa xử lý.",
    "rootCause": "Ứng dụng quá tải ngưng trệ thread (Thread Lockup) hoặc gặp hiện tượng HTTP 504 Gateway Timeout do lưu lượng truy cập đột biến.",
    "actionableSteps": [
      "1. Kích hoạt tính năng Auto-scaling hoặc Scale UP cấu hình máy chủ từ c5.xlarge lên dòng cao hơn.",
      "2. Giới hạn lưu lượng truy cập đầu vào (Rate Limiting / Throttling) để giảm tải cho CPU.",
      "3. Khởi động lại các worker node bị treo thread."
    ],
    "isMockResponse": true
  }
  ```

---

## 4. Chuẩn bị Môi trường Test
- `llm-service` chạy ở port `8085` (`mvn spring-boot:run` hoặc Docker Compose).
- `instance-service` (port `8081`) và `alert-service` (port `8084`) chạy song song. *(Lưu ý: Trên thực tế LLM Service gọi tới `/api/instances/{id}` và `/api/alerts?instanceId={id}`. Nếu 2 service này chưa bật, `llm-service` tự động kích hoạt 4 dạng dữ liệu test giả lập tạm thời theo `instanceId` ở Mục 3)*.
- Công cụ test: Postman / cURL / Swagger UI (`http://localhost:8085/swagger-ui.html`).

---

## 5. Các kịch bản Test (Test Cases)

### 5.1. Test Chẩn đoán AI Thành công (Ideal State)
**Yêu cầu API**:
- **Method**: `GET`
- **URL**: `http://localhost:8085/api/instances/15/diagnosis`
- **Headers**: 
  - `Authorization`: `Bearer <JWT_TOKEN>` (Nếu đi qua API Gateway)
  - `Accept`: `application/json`

**Kỳ vọng**:
- **HTTP Status**: `200 OK`
- `data.instanceId` đúng bằng ID truyền vào (15).
- `data.issueType` thuộc các giá trị chuẩn: `HIGH_CPU`, `SYSTEM_ERROR`, `STOPPED`, `CRITICAL`, `NONE`.
- `data.healthScore` là số nguyên trong khoảng `0` - `100`.
- `data.actionableSteps` là một danh sách (Array) các bước hướng dẫn cụ thể.
- `data.isMockResponse` nhận giá trị `true` (nếu chạy Mock Fallback) hoặc `false` (nếu Gemini API phản hồi trực tiếp).

---

### 5.2. Test Lỗi Tham số Path Variable (Bad Request)
- **Method**: `GET`
- **URL**: `http://localhost:8085/api/instances/invalid_id/diagnosis`
- **Kỳ vọng**:
  - **HTTP Status**: `400 Bad Request`
  - `success`: `false`
  - `code`: `400`
  - `message`: Chứa thông báo lỗi ép kiểu tham số `id`.

---

### 5.3. Test Lỗi Hệ thống Nội bộ (Internal Server Error)
- **Kịch bản**: Gọi API khi Gemini API lỗi mà `mock-enabled=false`, hoặc lỗi server không xác định.
- **Kỳ vọng**:
  - **HTTP Status**: `500 Internal Server Error`
  - `success`: `false`
  - `code`: `500`
  - `message`: *"Xảy ra lỗi hệ thống khi phân tích AI: ..."*

---

## 6. Chi tiết Cấu trúc JSON Response (API Envelope)

### 6.1. Response Thành công (HTTP 200 OK)
```json
{
  "success": true,
  "code": 200,
  "message": "Phân tích chẩn đoán từ AI hoàn tất",
  "data": {
    "instanceId": 15,
    "instanceName": "payment-service-prod",
    "instanceStatus": "ERROR",
    "issueType": "CRITICAL",
    "healthScore": 25,
    "diagnosis": "[Sự cố Nghiêm trọng] Máy chủ 'payment-service-prod' (ID: 15) gặp lỗi nguy hiểm với CPU 96.8% và 2 cảnh báo chưa xử lý.",
    "rootCause": "Ứng dụng quá tải ngưng trệ thread (Thread Lockup) hoặc gặp hiện tượng HTTP 504 Gateway Timeout do lưu lượng truy cập đột biến.",
    "actionableSteps": [
      "1. Kích hoạt tính năng Auto-scaling hoặc Scale UP cấu hình máy chủ từ c5.xlarge lên dòng cao hơn.",
      "2. Giới hạn lưu lượng truy cập đầu vào (Rate Limiting / Throttling) để giảm tải cho CPU.",
      "3. Khởi động lại các worker node bị treo thread."
    ],
    "isMockResponse": true
  },
  "timestamp": "2026-08-08T23:15:47.123456"
}
```

#### Chi tiết các trường trong `data` (`InstanceDiagnosisResponse`):
| Tên trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `instanceId` | `Long` | ID định danh của Instance được phân tích. |
| `instanceName` | `String` | Tên tên máy chủ ảo (VD: `db-postgres-primary`, `payment-service-prod`). |
| `instanceStatus` | `String` | Trạng thái hiện tại (`RUNNING`, `STOPPED`, `ERROR`). |
| `issueType` | `String` | Phân loại sự cố chính (`HIGH_CPU`, `SYSTEM_ERROR`, `STOPPED`, `CRITICAL`, `NONE`). |
| `healthScore` | `Integer` | Điểm sức khỏe máy chủ đánh giá bởi AI (thang điểm 0 - 100). |
| `diagnosis` | `String` | Tóm tắt tổng quan về tình trạng sức khỏe máy chủ. |
| `rootCause` | `String` | Phân tích nguyên nhân kỹ thuật chi tiết gây ra sự cố. |
| `actionableSteps` | `List<String>` | Danh sách các bước đề xuất để kỹ sư DevOps/SRE khắc phục. |
| `isMockResponse` | `Boolean` | `true` nếu phản hồi từ Mock Fallback, `false` nếu từ Gemini AI live. |

---

### 6.2. Response Lỗi Tham số Đầu vào (HTTP 400 Bad Request)
```json
{
  "success": false,
  "code": 400,
  "message": "Tham số đường dẫn 'id' không đúng định dạng số nguyên (Long)",
  "data": null,
  "errors": [
    {
      "field": "id",
      "message": "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'"
    }
  ],
  "timestamp": "2026-08-08T23:15:47.123456"
}
```

---

### 6.3. Response Lỗi Chưa Xác thực (HTTP 401 Unauthorized)
```json
{
  "success": false,
  "code": 401,
  "message": "Yêu cầu không hợp lệ. Vui lòng đăng nhập và cung cấp JWT Token hợp lệ.",
  "data": null,
  "timestamp": "2026-08-08T23:15:47.123456"
}
```

---

### 6.4. Response Lỗi Không có Quyền (HTTP 403 Forbidden)
```json
{
  "success": false,
  "code": 403,
  "message": "Tài khoản của bạn không có quyền truy cập vào tài nguyên chẩn đoán LLM này.",
  "data": null,
  "timestamp": "2026-08-08T23:15:47.123456"
}
```

---

### 6.5. Response Lỗi Server Nội bộ (HTTP 500 Internal Server Error)
```json
{
  "success": false,
  "code": 500,
  "message": "Xảy ra lỗi hệ thống khi phân tích AI: Gemini API connection timeout after 8000ms",
  "data": null,
  "timestamp": "2026-08-08T23:15:47.123456"
}
```

---

## 7. Checklist Kiểm thử Module LLM

- [x] **Khởi động Service OK**: App khởi chạy trên port `8085` không có lỗi.
- [x] **Swagger UI / OpenAPI OK**: Truy cập `http://localhost:8085/swagger-ui.html` hiển thị đầy đủ tài liệu API.
- [x] **API Endpoint Chẩn đoán OK**: `GET /api/instances/{id}/diagnosis` phản hồi HTTP 200.
- [x] **Tích hợp RestClient Telemetry OK**: Đọc được thông tin từ `instance-service` và `alert-service` (hoặc fallback generator).
- [x] **Tích hợp Gemini AI API OK**: Gửi prompt đúng định dạng và parse JSON kết quả trả về từ Gemini 1.5 Flash.
- [x] **Cơ chế Fallback (Mock) OK**: Tự động chuyển sang `MockLlmServiceImpl` khi Gemini lỗi/hết quota mà không làm sập request.
- [x] **Chuẩn hóa API Envelope OK**: Mọi response (thành công lẫn thất bại) đều tuân thủ `ApiResponse<T>` (`success`, `code`, `message`, `data`, `errors`, `timestamp`).
- [x] **Xử lý Exception tập trung OK**: `GlobalExceptionHandler` bắt và format lỗi đúng chuẩn JSON.
