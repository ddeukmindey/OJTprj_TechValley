# Hướng dẫn xem Database qua pgAdmin (Web UI)
### Dự án: TechValley — Cloud Instance Monitoring System (MSA)

Tài liệu này hướng dẫn từng bước xem dữ liệu thật trong 4 database Postgres của hệ thống (mỗi service 1 database riêng), bắt đầu từ lúc chưa bật container nào.

---

## Kiến trúc database (để hiểu vì sao có 4 database riêng)

| Service | Database | Bảng chính | Port map ra host |
|---|---|---|---|
| auth-gateway-service | `techvalley_auth` | `members` | `5433` |
| instance-service | `techvalley_instance` | `instances` | `5434` |
| client-service | `techvalley_client` | `clients`, `cost_snapshots` | `5435` |
| alert-service | `techvalley_alert` | `alerts` | `5436` |

> `monitoring-service` và `llm-service` **không có database riêng** — chúng chỉ gọi API sang các service khác qua WebClient nội bộ. Đây chính là điểm chứng minh kiến trúc MSA thật sự (mỗi service sở hữu dữ liệu riêng), không phải "distributed monolith" (nhiều service dùng chung 1 DB).

---

## Bước 1 — Khởi động toàn bộ hệ thống

Mở terminal, `cd` vào thư mục gốc của repo (nơi có file `docker-compose.yml`), chạy:

```bash
docker-compose up -d
```

Đợi khoảng 10–30 giây để Postgres và các service khởi động xong. Kiểm tra tất cả container đã chạy bằng:

```bash
docker ps
```

Bạn cần thấy các container sau đang ở trạng thái `Up`:

```
techvalley-postgres-auth
techvalley-postgres-instance
techvalley-postgres-client
techvalley-postgres-alert
techvalley-pgadmin
auth-gateway-service
instance-service
client-service
monitoring-service
alert-service
llm-service
techvalley-nginx
```

Nếu container nào bị `Exit` hoặc `Restarting`, xem log bằng:

```bash
docker logs <tên-container>
```

---

## Bước 2 — Mở pgAdmin trên trình duyệt

1. Mở Chrome (hoặc trình duyệt bất kỳ), truy cập:
   ```
   http://localhost:5050
   ```
2. Màn hình đăng nhập pgAdmin hiện ra. Đăng nhập bằng:
   - **Email:** `admin@techvalley.com`
   - **Password:** `admin`

   (2 giá trị này lấy từ biến `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD` trong `docker-compose.yml`.)

---

## Bước 3 — Đăng ký 4 server database vào pgAdmin

Bạn cần làm thao tác này **4 lần**, mỗi lần cho 1 database. Cách làm giống hệt nhau, chỉ khác **Host name** và **tên Server**.

### 3.1. Mở form đăng ký server

- Ở panel bên trái, chuột phải vào mục **Servers** → chọn **Register** → **Server...**

### 3.2. Tab "General"

- **Name:** đặt tên gợi nhớ, ví dụ `Auth DB` (server đầu tiên), `Instance DB` (server thứ 2), `Client DB` (server thứ 3), `Alert DB` (server thứ 4).

### 3.3. Tab "Connection"

Điền đúng theo bảng dưới (ví dụ đang đăng ký server **Auth DB**):

| Trường | Giá trị |
|---|---|
| Host name/address | `postgres-auth` |
| Port | `5432` |
| Maintenance database | `techvalley_auth` |
| Username | `techvalley_admin` |
| Password | `password123` |
| Save password? | Bật (tick chọn) |

> ⚠️ **Lưu ý quan trọng:** Host phải là **tên container** (`postgres-auth`, `postgres-instance`, `postgres-client`, `postgres-alert`), **không phải** `localhost`. Vì pgAdmin và các Postgres cùng chạy trong network nội bộ `techvalley-net` của Docker, nên chúng gọi nhau bằng tên container. Port cũng luôn là `5432` (port nội bộ trong network) — không phải `5433/5434/5435/5436` (đó là port map ra máy host, chỉ dùng khi bạn kết nối từ psql/DBeaver ngoài Docker).

- Bấm **Save**. Server đầu tiên xuất hiện trong panel trái.

### 3.4. Lặp lại cho 3 server còn lại

Làm lại bước 3.1–3.3 với thông tin sau:

**Server 2 — Instance DB**
| Trường | Giá trị |
|---|---|
| Host name/address | `postgres-instance` |
| Port | `5432` |
| Maintenance database | `techvalley_instance` |
| Username | `techvalley_admin` |
| Password | `password123` |

**Server 3 — Client DB**
| Trường | Giá trị |
|---|---|
| Host name/address | `postgres-client` |
| Port | `5432` |
| Maintenance database | `techvalley_client` |
| Username | `techvalley_admin` |
| Password | `password123` |

**Server 4 — Alert DB**
| Trường | Giá trị |
|---|---|
| Host name/address | `postgres-alert` |
| Port | `5432` |
| Maintenance database | `techvalley_alert` |
| Username | `techvalley_admin` |
| Password | `password123` |

Sau khi xong, panel trái sẽ có dạng cây như sau:

```
Servers
├── Auth DB
├── Instance DB
├── Client DB
└── Alert DB
```

---

## Bước 4 — Xem danh sách bảng

Với mỗi server, mở rộng theo đường dẫn:

```
Tên Server → Databases → techvalley_xxx → Schemas → public → Tables
```

Ví dụ với **Auth DB**: `Auth DB → Databases → techvalley_auth → Schemas → public → Tables → members`

Bạn sẽ thấy đúng số bảng đã thiết kế trong ERD:

- `techvalley_auth` → bảng **members**
- `techvalley_instance` → bảng **instances**
- `techvalley_client` → bảng **clients**, **cost_snapshots**
- `techvalley_alert` → bảng **alerts**

---

## Bước 5 — Xem dữ liệu bên trong bảng

Có 2 cách:

### Cách A — Xem nhanh qua giao diện (không cần viết SQL)

Chuột phải vào tên bảng (ví dụ `instances`) → **View/Edit Data** → **All Rows**.

Dữ liệu hiện ra dạng bảng (spreadsheet), có thể sửa trực tiếp nếu cần.

### Cách B — Dùng Query Tool (linh hoạt hơn, xem được nhiều bảng cùng lúc)

1. Chuột phải vào tên database (ví dụ `techvalley_instance`) → **Query Tool**.
2. Gõ câu lệnh SQL, ví dụ:
   ```sql
   SELECT * FROM instances ORDER BY updated_at DESC;
   ```
3. Bấm nút ▶ (hoặc phím tắt `F5`) để chạy.

**Mẹo:** để xem nhanh tất cả bảng + số dòng dữ liệu của cả database mà không cần mở từng bảng, chạy câu lệnh sau trong Query Tool:

```sql
SELECT relname AS table_name, n_live_tup AS row_count
FROM pg_stat_user_tables
ORDER BY relname;
```

---

## Bước 6 — Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân | Cách khắc phục |
|---|---|---|
| Không mở được `localhost:5050` | Container `pgadmin` chưa chạy hoặc chưa khởi động xong | Chạy `docker ps` kiểm tra, đợi thêm hoặc `docker logs techvalley-pgadmin` |
| pgAdmin báo lỗi "could not translate host name postgres-auth" | Điền nhầm Host thành `localhost` thay vì tên container | Sửa lại Host đúng tên container như bảng ở Bước 3 |
| pgAdmin báo lỗi "password authentication failed" | Gõ sai username/password | Dùng đúng `techvalley_admin` / `password123` |
| Mở bảng nhưng không có dữ liệu | Database mới khởi tạo, service chưa từng nhận request nào để ghi dữ liệu | Gọi thử vài API qua Swagger/nginx (`POST /api/instances`, `POST /api/clients`...) rồi refresh lại bảng |
| Muốn kết nối bằng DBeaver/TablePlus thay vì pgAdmin | — | Dùng Host `localhost`, Port tương ứng ở cột "Port map ra host" trong bảng đầu tài liệu (`5433`–`5436`), vì lúc đó bạn kết nối từ ngoài Docker network |

---

## Tóm tắt nhanh (checklist)

- [ ] `docker-compose up -d`
- [ ] `docker ps` → tất cả container `Up`
- [ ] Mở `http://localhost:5050`, đăng nhập `admin@techvalley.com` / `admin`
- [ ] Đăng ký 4 server: Host = tên container (`postgres-auth/instance/client/alert`), Port = `5432`, User = `techvalley_admin`, Pass = `password123`
- [ ] Expand từng server → Databases → Schemas → public → Tables
- [ ] Chuột phải bảng → View/Edit Data → All Rows để xem dữ liệu thật
