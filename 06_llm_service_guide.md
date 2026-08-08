# Hướng dẫn chi tiết triển khai `llm-service` (AI Auto-Diagnosis)

Tài liệu này ghi chú lại chi tiết các công cụ đã sử dụng, quy trình chuẩn bị và các bước thực hiện để hoàn thiện module `llm-service` theo chuẩn kiến trúc Microservices (MSA).

---

## 1. Công cụ & Thư viện chuẩn bị
- **Môi trường & Ngôn ngữ:** Java 17, Spring Boot 3.2.x, Maven.
- **Dependency chính (`pom.xml`):**
  - `spring-boot-starter-web`: Xây dựng RESTful API (`@RestController`, `@GetMapping`).
  - `spring-boot-starter-webflux`: Cung cấp `WebClient` để gọi API nội bộ (`instance-service`, `alert-service`) và gọi API bên thứ 3 (Google Gemini).
  - `jackson-databind`: Parse (phân tích) kết quả trả về dạng JSON từ AI.
- **Cơ sở hạ tầng:** Docker & Docker Compose (Quản lý các container mạng nội bộ `techvalley-net`).
- **AI Provider:** Google Gemini 1.5 Flash (Sử dụng API Key miễn phí từ Google AI Studio).

---

## 2. Hướng dẫn lấy Google Gemini API Key (Miễn phí)
1. Truy cập [Google AI Studio](https://aistudio.google.com/).
2. Đăng nhập bằng tài khoản Google.
3. Ở menu trái, chọn **"Get API key"** -> Nhấn **"Create API key in new project"**.
4. Copy đoạn mã bắt đầu bằng `AIzaSy...` (hoặc `AQ...` nếu tạo từ GCP).
5. Mở file `llm-service/src/main/resources/application.yml` và dán vào phần:
   ```yaml
   llm:
     api-key: [DÁN_API_KEY_VÀO_ĐÂY]
   ```

---

## 3. Cách thức triển khai chi tiết từng bước

Dựa vào mô hình `Controller -> Service -> DTO / Client`, mã nguồn được triển khai theo các bước sau:

### Bước 1: Khởi tạo DTO (Data Transfer Object)
Tạo package `com.techvalley.llm.dto` để hứng và trả dữ liệu.
- `ApiResponse.java`: Class bọc kết quả chuẩn của hệ thống (chứa `success`, `code`, `message`, `data`, `timestamp`).
- `InstanceDiagnosisResponse.java`: Định dạng JSON trả về cho người dùng chứa các trường do AI sinh ra (healthScore, diagnosis, rootCause, actionableSteps).

### Bước 2: Thiết lập WebClient giao tiếp nội bộ
Tạo package `com.techvalley.llm.client`.
Vì hệ thống chạy MSA, `llm-service` không truy cập Database trực tiếp mà phải xin dữ liệu từ các service khác.
- `InstanceServiceClient.java`: Gọi tới `http://localhost:8081/api/instances/{id}` để lấy tình trạng CPU, RAM.
- `AlertServiceClient.java`: Gọi tới `http://localhost:8084/api/alerts?instanceId={id}` để lấy lịch sử lỗi.

### Bước 3: Áp dụng Strategy Pattern cho AI Service
Tạo package `com.techvalley.llm.service` và `impl`. 
Nhằm dự phòng khi hết API Key hoặc rớt mạng, ta thiết kế 1 Interface và 2 class kế thừa:
1. `LlmProviderService.java` (Interface định nghĩa hàm `analyze(prompt)`).
2. `MockLlmServiceImpl.java`: Trả về dữ liệu cứng (hardcode JSON) dùng để test khi không có mạng.
3. `GeminiLlmServiceImpl.java`: 
   - Dùng `WebClient` gọi HTTP POST tới `https://generativelanguage.googleapis.com/...`.
   - Gửi theo cấu trúc System Prompt ép AI phải trả về định dạng `application/json` thuần túy.

### Bước 4: Viết Business Logic (Orchestrator)
Tạo `LlmServiceImpl.java`:
- Thu thập dữ liệu từ `InstanceServiceClient` và `AlertServiceClient`.
- Gom tất cả thành chuỗi Prompt: *"Đây là dữ liệu instance... Đây là lỗi... Hãy chẩn đoán"*.
- Gửi Prompt cho AI (thông qua interface).
- Dùng `ObjectMapper` để parse chuỗi JSON do AI trả về thành object `InstanceDiagnosisResponse`.

### Bước 5: Phơi bày REST API Controller
Tạo `LlmController.java`:
- Khai báo `@RestController` và `@RequestMapping("/api/instances")`.
- Viết hàm `@GetMapping("/{id}/diagnosis")` hứng request và trả kết quả đã bọc trong `ApiResponse`.

---

## 4. Cách chạy và Test Hệ Thống
Vì project này dùng Docker Compose định tuyến các service, bạn làm theo các bước sau:
1. Mở Terminal tại thư mục gốc `OJTprj_TechValley`.
2. Chạy lệnh: `docker-compose up -d --build llm-service`
3. Mở **Postman**, tạo Request:
   - **Method:** `GET`
   - **URL:** `http://localhost:8085/api/instances/1/diagnosis`
   - **Body:** `none`
4. Bấm **Send** và xem kết quả AI tư vấn.
