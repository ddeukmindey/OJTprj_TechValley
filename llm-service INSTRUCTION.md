# Hướng dẫn Chuẩn bị Công cụ & Thực hiện chi tiết — `llm-service`

Áp dụng cho tính năng `GET /api/instances/{id}/diagnosis` (TechValley OJT). File này gồm 2 phần:
**(A)** công cụ cần chuẩn bị trước khi bắt đầu, **(B)** các bước thực hiện chi tiết kèm xử lý lỗi
thường gặp — đúc kết từ quá trình triển khai thực tế.

## A. Chuẩn bị công cụ

| # | Công cụ | Mục đích | Cách kiểm tra đã cài |
|---|---|---|---|
| 1 | Postman **hoặc** trình duyệt (dùng Swagger UI có sẵn) | Test API thủ công | — |
| 2 | Tài khoản Google (miễn phí, không cần thẻ tín dụng) | Lấy Gemini API Key | — |


> **Windows**: dùng PowerShell mặc định là được, không cần cài Git Bash/WSL riêng cho các lệnh
> trong tài liệu này — mục B.5 có ghi chú tương đương PowerShell cho từng lệnh Linux/Mac.

---

## B. Các bước thực hiện chi tiết

### B.1. Tạo branch riêng

```bash
git checkout final
git pull origin final
git checkout -b feature/llm-<ten>-diagnosis
```

### B.2. Lấy Gemini API Key miễn phí

1. Vào **https://aistudio.google.com/apikey**, đăng nhập bằng tài khoản Google bất kỳ.
2. Bấm **"Create API key"** → chọn project có sẵn hoặc để Google tự tạo mới.
3. Copy key. **Lưu ý quan trọng**: kể từ giữa 2026, Google phát hành key theo 2 định dạng:
   - `AIzaSy...` (Standard key, kiểu cũ)
   - `AQ.Ab...` (Auth key, kiểu mới — phần lớn tài khoản mới hiện chỉ nhận được loại này)

   Cả 2 loại đều dùng được, nhưng **phải gửi key qua HTTP header `x-goog-api-key`**, không dùng
   query param `?key=...` (cách cũ chỉ ổn định với key `AIzaSy`, còn key `AQ.` dùng query param
   dễ bị lỗi 401/404 khó hiểu). Code mẫu trong repo (`GeminiLlmProviderServiceImpl.java`) đã
   xử lý đúng theo header, không cần sửa gì thêm.

### B.3. Kiểm tra model nào thực sự dùng được với key của bạn

Google liên tục đổi tên và khai tử model (ví dụ `gemini-1.5-flash`, `gemini-2.0-flash` đã bị
khai tử; `gemini-2.5-flash` với 1 số tài khoản mới báo *"no longer available to new users"* dù
vẫn hiện trong danh sách). Vì vậy **trước khi chạy, hãy tự kiểm tra**:

**Linux/Mac/Git Bash:**
```bash
curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=YOUR_API_KEY" \
  | grep -o '"name": "models/[^"]*"' | sort -u
```

**Windows PowerShell:**
```powershell
(Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models?key=YOUR_API_KEY").models |
  Select-Object name, supportedGenerationMethods | Format-Table -AutoSize
```

→ Tìm model có `generateContent` trong `supportedGenerationMethods`.

**Khuyến nghị**: dùng alias **`gemini-flash-latest`** thay vì ghim cứng 1 version cụ thể
(`gemini-2.5-flash`, `gemini-3.5-flash`...). Alias này do Google duy trì, luôn tự trỏ tới bản
Flash mới nhất mà tài khoản của bạn được phép dùng — tránh việc phải sửa code mỗi khi Google
khai tử model.

### B.4. Cấu hình `.env`

```bash
cp .env.example .env
```
Sửa nội dung (file `.env` nằm cùng cấp thư mục với `docker-compose.yml`):
```
LLM_PROVIDER=gemini
LLM_API_KEY=<dán key thật vào đây>
LLM_MODEL=gemini-flash-latest
```

> `LLM_PROVIDER=mock` nếu muốn chạy offline hoàn toàn, không cần API Key — hữu ích khi demo ở nơi
> mạng không ổn định.

### B.5. Build & chạy hệ thống bằng Docker (không dùng Maven local)

**Chạy toàn bộ hệ thống lần đầu (hoặc sau khi pull code mới):**
```bash
docker compose up -d --build
docker compose logs -f llm-service
```
Lệnh `--build` sẽ tự chạy Maven **bên trong container** theo `llm-service/Dockerfile` — không
cần máy bạn cài JDK/Maven.

**Chỉ build/khởi động lại riêng `llm-service`** (khi các service khác đã chạy sẵn, không cần
build lại toàn bộ hệ thống cho nhanh):
```bash
docker compose up -d --build llm-service
```

**Xem log riêng của `llm-service` khi debug:**
```bash
docker compose logs -f llm-service
```

**Kiểm tra biến môi trường container thực sự nhận được** (rất hay bị sai do `.env` không nằm
đúng thư mục gốc cạnh `docker-compose.yml`, hoặc do quên rebuild sau khi sửa code):

- Linux/Mac: `docker exec llm-service env | grep LLM_`
- Windows PowerShell: `docker exec llm-service env | Select-String LLM_`

**Sau khi sửa code Java trong `llm-service/`, luôn phải rebuild lại image** — chỉ `docker compose
up -d` (không kèm `--build`) sẽ dùng lại image cache cũ, KHÔNG áp dụng thay đổi code mới:
```bash
docker compose stop llm-service
docker compose build --no-cache llm-service
docker compose up -d llm-service
```

> **Mẹo**: nếu chỉ sửa `.env` (đổi `LLM_MODEL`, `LLM_API_KEY`, `LLM_PROVIDER`...) mà KHÔNG sửa
> code Java, không cần `--build`, chỉ cần `docker compose up -d llm-service` là container đọc
> lại `.env` mới.

### B.6. Test API

**Lấy JWT token** (đăng nhập qua `auth-gateway-service`):
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@techvalley.com","password":"..."}'
```

**Gọi thử endpoint:**
```bash
curl -s http://localhost:8085/api/instances/1/diagnosis \
  -H "Authorization: Bearer <TOKEN>"
```

**Hoặc test qua Swagger UI/Postman**: `http://localhost:8085/swagger-ui.html` → bấm "Authorize" →
dán `Bearer <token>` → thử endpoint trực tiếp trên UI, không cần gõ curl.

**Kết quả mong đợi** (khi Gemini hoạt động tốt):
```json
{
  "success": true,
  "code": 200,
  "data": {
    "instanceId": 1,
    "healthScore": 45,
    "diagnosis": "...",
    "rootCause": "...",
    "actionableSteps": ["...", "..."],
    "source": "AI"
  }
}
```
Nếu `source` là `"MOCK"` thay vì `"AI"` → Gemini gọi thất bại và hệ thống đã tự fallback (xem
mục B.7 để tra log tìm nguyên nhân), request vẫn thành công bình thường, không phải bug.

### B.7. Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `source` luôn là `MOCK`, log có `404 Not Found` | Model bị khai tử/giới hạn cho tài khoản bạn | Đổi `LLM_MODEL=gemini-flash-latest`, kiểm tra lại bằng ListModels (mục B.3) |
| `404 ... no longer available to new users` | Model tuy còn trong danh sách nhưng bị khóa quyền dùng cho account mới | Dùng alias `gemini-flash-latest` thay vì version cụ thể |
| Log `401 UNAUTHENTICATED` | Key dạng `AQ.` bị gửi qua query param thay vì header | Đảm bảo dùng đúng code mẫu (header `x-goog-api-key`), không tự sửa lại thành `?key=` |
| `Connection refused: instance-service:8081` lúc mới `docker compose up` | `depends_on` chỉ đợi container start, không đợi Spring Boot bên trong sẵn sàng (mất 20-35s) | Đợi log `instance-service` hiện dòng `Started ... Application in ...` rồi mới gọi; code đã có sẵn cơ chế tự retry ~40s nên thường tự phục hồi, không cần gọi lại tay |
| Sửa code Java xong gọi API vẫn thấy hành vi cũ | Quên rebuild Docker image | `docker compose build --no-cache llm-service` rồi `up -d` lại (xem B.5) |
| `.env` sửa xong không có tác dụng | File `.env` không nằm cùng cấp thư mục với `docker-compose.yml`, hoặc quên `docker compose up -d llm-service` lại | Kiểm tra bằng lệnh B.5 phần "kiểm tra biến môi trường", đảm bảo đúng thư mục gốc repo |
| PowerShell báo `An empty pipe element is not allowed` khi copy lệnh Linux | Lệnh dùng cú pháp `\` xuống dòng và `grep` không tồn tại trên PowerShell | Dùng bản lệnh PowerShell tương ứng (đã ghi kèm ở B.3, B.5) thay vì copy nguyên lệnh Linux |
| `403 Forbidden` khi gọi diagnosis | Đúng hành vi mong muốn — tài khoản `CLIENT_MANAGER` đang gọi instance không thuộc client mình quản lý | Không phải lỗi, dùng chính case này để demo RBAC trong video |
| `docker compose build` báo lỗi tải dependency Maven | Máy đang mất mạng, hoặc lần đầu build nên tải nhiều package (bình thường, chỉ chậm ở lần build đầu) | Kiểm tra mạng, chờ build xong; các lần build sau sẽ nhanh hơn nhờ cache layer của Docker |

---

## C. Trước khi mở Pull Request

- [ ] `docker compose build --no-cache llm-service` chạy pass, không lỗi (đồng nghĩa Maven build
      bên trong container cũng pass, không cần chạy Maven trên máy để kiểm tra riêng)
- [ ] Test cả 2 trường hợp: `source: "AI"` (Gemini hoạt động) và `source: "MOCK"` (giả lập lỗi để
      kiểm tra fallback) — cả 2 đều đã thử thành công
- [ ] Test case RBAC: `CLIENT_MANAGER` gọi instance của client khác → nhận `403`
- [ ] `.env` chứa key thật **không** được commit lên Git (kiểm tra `.gitignore` đã chặn `.env`)