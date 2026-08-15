# Hướng dẫn Setup & Test API — TechValley Cloud Instance Monitoring System

Tài liệu này dành cho người mới join dự án lần đầu, hoặc máy đã có sẵn code nhưng muốn dựng lại từ đầu (sạch 100%, không dính rác từ lần chạy trước). Làm đúng thứ tự từ trên xuống, không nhảy cóc.

---

## Phần 1 — Công cụ cần cài đặt

Dự án đã Docker hóa toàn bộ (6 service Spring Boot + 4 PostgreSQL + pgAdmin + nginx), nên **không cần cài JDK, Maven, PostgreSQL lên máy thật**. Chỉ cần:

| Công cụ | Bắt buộc | Ghi chú |
|---|---|---|
| [Git](https://git-scm.com/downloads) | ✅ | Để clone/pull code |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | ✅ | Bật lên, đợi biểu tượng cá voi ở khay hệ thống chuyển màu **xanh (Running)** trước khi làm bất cứ điều gì |
| Trình duyệt (Chrome/Edge/Firefox) | ✅ | Để mở Swagger UI |
| [Postman](https://www.postman.com/downloads/) hoặc `curl` | Tùy chọn | Swagger UI đã đủ dùng để test toàn bộ API, Postman chỉ cần nếu muốn lưu lại collection |
| DBeaver / pgAdmin | Tùy chọn | Dự án đã có sẵn pgAdmin chạy trong Docker (`http://localhost:5050`), không cần cài thêm |

**Kiểm tra Docker đã cài đúng chưa** — mở terminal (Git Bash / PowerShell / Terminal) gõ:
```bash
docker --version
docker compose version
```
Nếu cả 2 lệnh đều ra số phiên bản (không phải lỗi "command not found") là ổn.

⚠️ Trên Windows: đảm bảo Docker Desktop đang chạy ở chế độ **WSL2 backend** (mặc định khi cài mới), nếu không container Postgres có thể chậm bất thường.

---

## Phần 2 — Lấy code về máy

### Trường hợp A — Máy mới, chưa có code lần nào
```bash
git clone https://github.com/ddeukmindey/OJTprj_TechValley.git
cd OJTprj_TechValley
git checkout final_swagger
```

### Trường hợp B — Máy đã có sẵn code, muốn cập nhật bản mới nhất
```bash
cd OJTprj_TechValley
git checkout final_swagger
git fetch origin
git pull origin final_swagger
```

Kiểm tra đang đứng đúng nhánh và đã lấy code mới nhất:
```bash
git status
git log --oneline -5
```
`git status` phải báo `Your branch is up to date with 'origin/final_swagger'` và không còn file nào ở trạng thái `modified` (nếu có, dùng `git stash` hoặc `git reset --hard origin/final_swagger` để bỏ hết thay đổi cục bộ trước khi pull).

---

## Phần 3 — Dọn sạch Docker và build lại từ đầu (no-cache)

Đây là bước quan trọng nhất nếu bạn từng chạy dự án trước đó — đảm bảo không bị dính code cũ, image cũ, hay dữ liệu database cũ gây lệch ID (xem giải thích ở Phần 4).

### Bước 3.1 — Dừng và xoá sạch mọi thứ liên quan tới project cũ
```bash
docker compose down -v --remove-orphans
```
Giải thích từng flag:
- `down` — dừng và xoá toàn bộ container của project
- `-v` — xoá luôn **volume** (tức là xoá sạch dữ liệu 4 database Postgres — bắt buộc phải xoá nếu muốn seed data từ đầu cho ID sạch sẽ, dễ đoán)
- `--remove-orphans` — dọn luôn container "mồ côi" còn sót lại từ lần chạy có cấu trúc `docker-compose.yml` cũ hơn

### Bước 3.2 — Xoá image cũ của 6 service (bắt buộc để build no-cache thật sự)
```bash
docker images | grep ojtprj
docker rmi $(docker images "ojtprj*" -q) 2>/dev/null
```
(Nếu không ra dòng nào ở lệnh `docker images | grep ojtprj` nghĩa là chưa từng build — bỏ qua bước này.)

### Bước 3.3 — Build lại toàn bộ, không dùng cache
```bash
docker compose build --no-cache
```
Lệnh này sẽ build lại từ đầu cả 6 service (mỗi service tự `mvn clean install` bên trong container theo `Dockerfile` riêng) — **không** dùng lại bất kỳ layer Docker nào đã cache trước đó, đảm bảo code mới nhất được compile thật sự. Bước này khá lâu (5–15 phút tùy máy) vì phải tải lại toàn bộ dependency Maven.

### Bước 3.4 — Khởi động toàn bộ hệ thống
```bash
docker compose up -d
```

### Bước 3.5 — Kiểm tra tất cả container đã lên đúng và khỏe mạnh
```bash
docker ps -a
```
Phải thấy đủ **12 container**: `techvalley-postgres-auth`, `techvalley-postgres-instance`, `techvalley-postgres-client`, `techvalley-postgres-alert`, `techvalley-pgadmin`, `auth-gateway-service`, `instance-service`, `client-service`, `monitoring-service`, `alert-service`, `llm-service`, `techvalley-nginx` — tất cả đều ở trạng thái `Up`, **không** có cái nào `Exited` hay `Restarting`.

Nếu có service nào bị `Exited`, xem log để tìm nguyên nhân:
```bash
docker logs auth-gateway-service --tail 100
```
(thay tên container tương ứng)

⏱️ Lưu ý: 6 service Spring Boot cần khoảng 15–30 giây sau khi container lên để app thật sự sẵn sàng nhận request (Spring Boot boot time + Hibernate tạo schema). Đợi thêm ~30s sau khi `docker ps -a` báo `Up` rồi mới sang Phần 4.

### Bước 3.6 — Xác nhận hệ thống truy cập được qua nginx
Mở trình duyệt: **`http://localhost/`**

Phải thấy trang landing page tối màu, tiêu đề "TechValley Cloud Monitor — API Docs", với 6 card link tới Swagger UI của từng service. Nếu trang này không load được → nginx hoặc 1 trong 6 service chưa sẵn sàng, quay lại kiểm tra `docker logs`.

---

## Phần 4 — Seed dữ liệu mẫu vào database

Có 3 file SQL trong thư mục gốc, **phải chạy đúng thứ tự** vì dữ liệu phụ thuộc lẫn nhau qua 3 database tách biệt (kiến trúc MSA — mỗi service 1 DB riêng, không có foreign key giữa các DB khác nhau):

```
seed_auth.sql  →  seed_client.sql  →  seed_instance.sql
```

### Bước 4.1 — Seed bảng `members` (tài khoản đăng nhập)

```bash
docker exec -i techvalley-postgres-auth psql -U techvalley_admin -d techvalley_auth < seed_auth.sql
```

Lệnh này sẽ in ra bảng kết quả cuối cùng dạng:
```
 id | email                     | name                 | role
----+---------------------------+----------------------+-----------------
  2 | admin@techvalley.com      | Nguyen Van Admin      | ADMIN
  3 | manager1@techvalley.com   | Tran Thi Manager 1     | CLIENT_MANAGER
  4 | manager2@techvalley.com   | Le Van Manager 2       | CLIENT_MANAGER
```

⚠️ **QUAN TRỌNG — đọc kỹ trước khi sang bước tiếp theo:**
`auth-gateway-service` có `DataInitializer` tự động tạo sẵn tài khoản `admin@techvalley.com` **ngay khi service khởi động lần đầu** (trước cả khi bạn kịp chạy file seed này). Vì vậy giá trị `id` thật của `manager1@techvalley.com` và `manager2@techvalley.com` **có thể không phải lúc nào cũng giống nhau giữa các lần chạy** — phụ thuộc vào việc bạn xoá volume (`-v`) sạch hay không ở Bước 3.1.

**Ghi lại chính xác 2 số `id` của `manager1` và `manager2`** từ bảng kết quả vừa in ra. Mật khẩu của cả 3 tài khoản đều là **`123456`**.

### Bước 4.2 — Kiểm tra & sửa `seed_client.sql` nếu ID lệch

Mở file `seed_client.sql`, tìm đoạn:
```sql
INSERT INTO clients (client_name, contract_plan, manager_id, create_at) VALUES
('Cong ty TNHH ABC Technology',   'PREMIUM',  2, NOW()),  -- manager_id = id của manager1@techvalley.com
('Cong ty Co phan XYZ Solutions', 'STANDARD', 2, NOW()),
('Startup DEF Innovations',       'BASIC',    3, NOW());  -- manager_id = id của manager2@techvalley.com
```
File hiện đang giả định `manager1 = id 2`, `manager2 = id 3`. **So với kết quả thật bạn vừa xem ở Bước 4.1** — nếu khác, sửa 3 số `2` và `3` trong đoạn trên cho khớp đúng ID thật rồi lưu lại trước khi chạy tiếp.

### Bước 4.3 — Seed bảng `clients`

```bash
docker exec -i techvalley-postgres-client psql -U techvalley_admin -d techvalley_client < seed_client.sql
```

Kết quả in ra sẽ có dạng 3 client với `id` 1, 2, 3 — **ghi lại 3 id này** (dùng ở bước sau nếu ID khác 1/2/3).

### Bước 4.4 — Kiểm tra & sửa `seed_instance.sql` nếu cần

Tương tự Bước 4.2, mở `seed_instance.sql`, đối chiếu cột `client_id` trong các dòng `INSERT INTO instances` với 3 `id` thật vừa lấy ở Bước 4.3. Nếu đã seed đúng thứ tự trên volume sạch, mặc định thường khớp sẵn `1, 2, 3` — không cần sửa.

### Bước 4.5 — Seed bảng `instances`

```bash
docker exec -i techvalley-postgres-instance psql -U techvalley_admin -d techvalley_instance < seed_instance.sql
```

Sau bước này, dữ liệu mẫu đã sẵn sàng: 8 instance trải đều 3 client, có đủ trạng thái `RUNNING` / `STOPPED` / `ERROR`, có 1 instance CPU cao (92%) để test cảnh báo, có 1 instance dừng >48 giờ để test `/long-stopped`.

### Bước 4.6 (tuỳ chọn) — Xem lại dữ liệu bằng pgAdmin

1. Mở `http://localhost:5050`
2. Đăng nhập: `admin@techvalley.com` / `admin`
3. Chuột phải **Servers → Register → Server...**
   - Tab **General**: Name tuỳ ý, ví dụ `techvalley`
   - Tab **Connection**: `Host` lần lượt là `postgres-auth` / `postgres-client` / `postgres-instance` / `postgres-alert` tuỳ DB muốn xem (phải đăng ký riêng từng cái vì đây là 4 Postgres container khác nhau), Port `5432`, Username `techvalley_admin`, Password `password123`
4. Cây bên trái → **Databases → (tên DB) → Schemas → public → Tables** để xem dữ liệu vừa seed.

---

## Phần 5 — Test API trên Swagger UI

Toàn bộ traffic đi qua **nginx ở cổng 80** — 6 service Spring Boot **không** expose port ra ngoài trực tiếp (xem `docker-compose.yml`, các dòng `ports` của service đều bị comment), nên **chỉ có một cách test đúng là qua các đường dẫn `/docs/...` dưới đây**, không gọi thẳng `localhost:8080/8081/...`.

| Service | Swagger UI |
|---|---|
| Auth | `http://localhost/docs/auth/` |
| Instance | `http://localhost/docs/instance/` |
| Client | `http://localhost/docs/client/` |
| Monitoring | `http://localhost/docs/monitor/` |
| Alert | `http://localhost/docs/alert/` |
| LLM | `http://localhost/docs/llm/` |

### Bước 5.1 — Lấy JWT token

Mở **`http://localhost/docs/auth/`** → tìm `POST /api/auth/login` → **Try it out** → dán body:
```json
{
  "email": "admin@techvalley.com",
  "password": "123456"
}
```
→ **Execute** → copy chuỗi `token` trong response (không copy dấu ngoặc kép).

Muốn test theo góc nhìn quản lý (phân quyền CLIENT_MANAGER) thì login lại bằng:
```json
{
  "email": "manager1@techvalley.com",
  "password": "123456"
}
```

*(Lưu ý: ở màn hình Swagger của Auth, nút Authorize vẫn hiển thị nhưng **không bắt buộc** phải bấm — `POST /api/auth/login` là API public, không cần token, cứ Try it out thẳng.)*

### Bước 5.2 — Gắn token vào 5 Swagger UI còn lại

Token phải gắn **riêng ở từng tab Swagger** (5 service không share phiên đăng nhập với nhau):

1. Mở tab Swagger của service muốn test (VD: `http://localhost/docs/instance/`)
2. Bấm nút **Authorize** (góc trên bên phải, biểu tượng ổ khoá)
3. Dán token vào ô `Value` — **không cần** gõ chữ `Bearer` phía trước, Swagger tự thêm
4. Bấm **Authorize** → **Close**
5. Lặp lại cho `/docs/client/`, `/docs/monitor/`, `/docs/alert/`, `/docs/llm/`

Từ giờ mọi request Try it out ở 5 tab này đều tự động gửi kèm header `Authorization: Bearer <token>`.

### Bước 5.3 — Test Client API (`/docs/client/`)

Test theo thứ tự phụ thuộc dữ liệu — **Client có trước, Instance dựa vào Client**:

| Endpoint | Cách test |
|---|---|
| `POST /api/clients` | Đăng nhập **admin** → tạo client mới. Đăng nhập **manager1** → thử tạo → phải bị từ chối (chỉ ADMIN được tạo client) |
| `GET /api/clients` | Đăng nhập **admin** → thấy đủ cả 3 client seed sẵn. Đăng nhập **manager1** → chỉ thấy client có `managerId` khớp mình |
| `GET /api/clients/{id}/instances` | Dùng `id` client vừa seed (1/2/3) → trả về đúng instance thuộc client đó |
| `GET /api/clients/{id}/cost` | Đối chiếu tay: tổng `monthlyCost` các instance đang **RUNNING** thuộc client |
| `GET /api/clients/{id}/cost-forecast` | Đối chiếu bằng đơn giá đề bài: SMALL $50 / MEDIUM $120 / LARGE $250 |
| `GET /api/clients/{id}/sla` | Test với client PREMIUM (id=1, seed sẵn) và BASIC (id=3) — so ngưỡng SLA: PREMIUM 99.9% / STANDARD 99% / BASIC 95% |

### Bước 5.4 — Test Instance API (`/docs/instance/`)

| Endpoint | Cách test |
|---|---|
| `POST /api/instances` | Đăng nhập **manager1** → tạo instance cho client **không** thuộc quyền quản lý mình → phải bị chặn |
| `GET /api/instances` | Test đủ 3 tổ hợp: `page`+`size` (phân trang), `status=RUNNING` (lọc), `sortBy=cpuUsage&sortOrder=desc` (sắp xếp) |
| `GET /api/instances/{id}` | Test với `id` không tồn tại (VD 9999) → phải trả `404`, không phải lỗi `500` |
| `PATCH /api/instances/{id}/status` | Đổi 1 instance từ `STOPPED` sang `RUNNING`, kiểm tra field cập nhật thời gian có đổi |
| `DELETE /api/instances/{id}` | **Test quan trọng nhất**: xoá instance `web-server-01` (đang RUNNING, seed sẵn id=1) → phải báo lỗi (deletion blocked). Sau đó xoá `cache-server-stopped` (STOPPED) → phải thành công |

### Bước 5.5 — Test Monitoring API (`/docs/monitor/`)

Đây là phần giám khảo chấm kỹ nhất — logic auto-record + chống trùng cảnh báo.

| Endpoint | Cách test |
|---|---|
| `GET /api/monitor/warnings` | Instance `db-server-high-cpu` (cpuUsage=92%, seed sẵn) sẽ xuất hiện. Gọi **lần 1** → sang tab Alert kiểm tra 1 Alert `CPU_HIGH` mới được tạo. Gọi **lần 2 ngay sau đó** → Alert **không** được tạo trùng |
| `GET /api/monitor/errors` | Instance `app-server-error` (status=ERROR, seed sẵn) → gọi, kiểm tra Alert nghiêm trọng được tạo, gọi lại lần 2 không trùng |
| `GET /api/monitor/long-stopped` | `backup-server-longstop` (STOPPED từ 50 giờ trước, seed sẵn) → phải xuất hiện. `cache-server-stopped` (mới STOPPED ~2 giờ) → **không** được xuất hiện |
| `GET /api/monitor/report` | Đối chiếu tay: đếm theo status, tổng số cảnh báo, tổng chi phí, số alert chưa xử lý |

### Bước 5.6 — Test Alert API (`/docs/alert/`)

| Endpoint | Cách test |
|---|---|
| `GET /api/alerts` | Sau Bước 5.5 đã có Alert → test lọc `alertType=CPU_HIGH`, `isResolved=false` |
| `PATCH /api/alerts/{id}/resolve` | Resolve 1 alert vừa tạo → gọi lại `GET /api/monitor/warnings` cho cùng instance → Alert `CPU_HIGH` **phải được tạo lại** (vì alert cũ đã resolved, hết điều kiện dedup) — chứng minh rule chống trùng hoạt động đúng cả 2 chiều |

### Bước 5.7 — Test LLM API (`/docs/llm/`)

Tính năng đã chọn: **`GET /api/instances/{id}/diagnosis`**.

- Gọi với `id` của `app-server-error` (status ERROR, seed sẵn) → xem response có `cause` (nguyên nhân) + `recommendedAction` (hành động đề xuất) phản ánh đúng ngữ cảnh instance đó.
- Kiểm tra field `source` trong response:
  - `"MOCK"` → đang chạy chế độ giả lập (không gọi Gemini thật)
  - `"GEMINI_AI"` → đã gọi Gemini thành công

⚠️ **Lưu ý cấu hình hiện tại**: trong `docker-compose.yml`, `llm-service` đang đặt `LLM_USE_REAL_API=true` nhưng `GEMINI_API_KEY={GEMINI_API_KEY}` — đây **không phải** một API key thật (thiếu dấu `$` để nội suy biến môi trường, nên giá trị thật truyền vào container là chuỗi chữ `{GEMINI_API_KEY}`). Vì code có cơ chế tự fallback khi gọi Gemini thất bại (hết quota/sai key/mất mạng), API **sẽ không lỗi 500** — chỉ tự động trả kết quả `source: "MOCK"` thay vì `"GEMINI_AI"`.

Nếu muốn demo bằng AI thật, cần:
1. Lấy API key thật tại [Google AI Studio](https://aistudio.google.com/apikey)
2. Sửa trong `docker-compose.yml`, dòng của `llm-service`:
   ```yaml
   - GEMINI_API_KEY=<dán_key_thật_vào_đây>
   ```
3. `docker compose up -d --build llm-service` để build lại riêng service này rồi test lại — response sẽ có `source: "GEMINI_AI"`.

---

## Phần 6 — Checklist đối chiếu nhanh với tiêu chí chấm điểm đề bài

| Tiêu chí đề bài | Test ở bước nào |
|---|---|
| JWT Auth — phân quyền theo role thực sự hoạt động | 5.1 (2 role khác nhau) + 5.3, 5.4 (kiểm tra ownership) |
| Monitoring Logic — dedup + auto-record đúng | 5.5 + 5.6 (gọi lại lần 2, resolve rồi gọi lại) |
| Cost & SLA — tính đúng, có căn cứ đơn giá | 5.3 (đối chiếu tay $50/$120/$250 và ngưỡng SLA) |
| Instance Deletion Rules — RUNNING bị chặn xoá | 5.4 (`DELETE /api/instances/{id}`) |
| LLM Prompt — phản ánh domain context | 5.7 |

---

## Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `docker compose up` báo lỗi port `80` đã bị chiếm | Có ứng dụng khác (Skype, IIS, XAMPP...) đang dùng port 80 | Tắt ứng dụng đó, hoặc đổi `"80:80"` thành `"8000:80"` trong `docker-compose.yml` rồi truy cập `http://localhost:8000/` |
| Trang `http://localhost/` không load | 1 trong 6 service chưa khởi động xong, hoặc lỗi compile | `docker ps -a` xem service nào `Exited`, rồi `docker logs <tên_container>` |
| Login trả về `401` dù đúng email/password | Chưa chạy `seed_auth.sql`, hoặc database bị dính dữ liệu cũ từ lần chạy trước không xoá `-v` | Chạy lại từ Bước 3.1 (`down -v`) → Bước 4.1 |
| `POST /api/clients` báo lỗi `foreign key`/`managerId không tồn tại` | ID của `manager1`/`manager2` trong `seed_client.sql` không khớp ID thật trong bảng `members` | Xem lại Bước 4.2 |
| Gọi API ở tab Swagger khác báo `401`/`403` dù đã login | Chưa bấm Authorize **ở đúng tab** đó — token không share giữa các Swagger UI | Lặp lại Bước 5.2 cho từng tab |
| `GET /api/instances/{id}/diagnosis` trả `source: "MOCK"` dù muốn AI thật | `GEMINI_API_KEY` chưa phải key thật (xem Bước 5.7) | Làm theo hướng dẫn cuối Bước 5.7 |
