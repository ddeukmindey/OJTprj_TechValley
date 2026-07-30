# QUY TẮC DỰ ÁN (PROJECT RULES) - CLOUD INSTANCE MONITORING SYSTEM

## 1. QUY CHUẨN XÂY DỰNG FRONTEND (UI/UX GUIDELINE COMPLIANCE)
Khi khởi tạo mã nguồn giao diện (Frontend Code), thiết kế component, hoặc làm việc trên tầng UI/UX cho hệ thống **Cloud Instance Monitoring System**, BẮT BUỘC tuân thủ nghiêm ngặt theo tài liệu hướng dẫn UI/UX:
[UInUX_guideline.md](file:///d:/OTJprj_TechValley/UInUX_guideline.md)

### Các quy tắc UI/UX cốt lõi cần đảm bảo:
1. **Mô hình Layout (Layout Model)**:
   - Sử dụng Admin Dashboard Layout chuẩn với 4 khối:
     - Left Sidebar (Fixed, 240px-280px): Logo TechValley, Badge vai trò (`ADMIN`/`CLIENT_MANAGER`), Menu điều hướng (Dashboard, Clients, Instances, Alerts, Cost & SLA, LLM Reports).
     - Top Header (Fixed): Global Search Bar, Notification Bell (Red Badge đếm số alert chưa xử lý), User Avatar & Role Dropdown.
     - Main Content Area (Scrollable): 12-column Grid Layout với padding 24px/32px cho Widget Cards, Charts, Tables.
     - Footer: Thông tin bản quyền & System Health Status Indicator (Green Dot khi Server Connected).
2. **Phong cách Thiết kế & Bảng màu (Theme & Palette)**:
   - Design Style: Enterprise / SaaS Cloud Management Dashboard.
   - Primary Base: Dark Navy / Slate Blue (`#0F172A` - Slate 900 hoặc `#1E293B` - Slate 800).
   - Background Area: Soft Light Gray (`#F8FAFC` - Slate 50).
   - Surface Card: Pure White (`#FFFFFF`), border `1px solid #E2E8F0`, bo góc `rounded-lg` (8px-12px), `shadow-sm`.
   - Semantic Status Colors:
     - `RUNNING`: Green (`#22C55E` / Tailwind `emerald-500`)
     - `STOPPED`: Amber (`#F59E0B` / Tailwind `amber-500`)
     - `ERROR` / `CRITICAL`: Red (`#EF4444` / Tailwind `red-500`)
     - `HIGH_CPU` / `WARNING`: Orange (`#F97316` / Tailwind `orange-500`)
3. **Thành phần Giao diện & Ràng buộc Nghiệp vụ (Components & Logic Rules)**:
   - Status Badges: Dạng Pill (`rounded-full`), background nhạt (opacity 10-15%) + chữ đậm semantic.
   - Data Tables: Sticky Header, hover row (`hover:bg-slate-50`), CPU Usage Column kèm Mini Progress Bar (Green -> Orange nếu CPU >= 80%).
   - Delete Instance Rule: Nút Delete khi Instance ở trạng thái `RUNNING` phải bị **Disabled** (màu xám, ngắt click) kèm Tooltip *"Không thể xóa Instance đang hoạt động. Vui lòng dừng Instance trước!"*. Chỉ active khi `STOPPED` hoặc `ERROR`.
   - Alerts Indicator: Viền trái đỏ/cam + nhãn `UNRESOLVED` khi `isResolved == false`. Đổi thành `RESOLVED` màu xanh lá sau khi xử lý thành công.
   - AI / LLM Widget: Gradient Border / Sparkles Icon ✨, typography rõ ràng, Skeleton Loader trong lúc chờ API trả về.
4. **5 Core UI States**:
   - Ideal State: Hiển thị đủ dữ liệu, định dạng số chuẩn (tiền tệ `$`, tỷ lệ `%`, ngày giờ `YYYY-MM-DD HH:mm`).
   - Empty State: Không để trang trắng/bảng trống, hiển thị Illustration/Icon xám nhẹ + thông điệp rõ ràng + Call-to-Action chính.
   - Loading State: Bắt buộc dùng Skeleton Loaders (`animate-pulse`) cho Card/Table/Chart; Spinner Button khi click hành động. Cấm dùng màn hình trắng.
   - Error State: Inline Validation Error (viền đỏ + thông báo lỗi dưới ô input), Toast Error góc trên bên phải (`AlertCircleIcon`), Global Error Page (404/500).
   - Partial / Stale State: Badge *"Đang cập nhật..."*, giữ dữ liệu cũ hiển thị mờ (opacity 60%).
5. **Typography**:
   - Font Family: Inter / Plus Jakarta Sans / Roboto (Sans-serif primary); JetBrains Mono / Fira Code (Monospace cho ID, IP, Code, Logs).
   - Hierarchy: H1 Page Title (24px Bold), H2 Section (18px Semibold), H3 Metric Label (14px Semibold), Body Primary (14px Regular), Body Small/Muted (12px Slate 500), Data/Numbers (24px-32px Bold).

---

## 2. QUY CHUẨN KIẾN TRÚC TẦNG BACKEND (LAYERED ARCHITECTURE GUIDELINE)
Tất cả mã nguồn Backend cho **Cloud Instance Monitoring System** phải tuân thủ nghiêm ngặt mô hình kiến trúc phân tầng (Layered Architecture): `Controller -> Service -> Repository -> Entity/Model`, chi tiết tại [specification_OJTprj.md](file:///d:/OTJprj_TechValley/specification_OJTprj.md).

### Ràng buộc trách nhiệm giữa các tầng (Layer Separation Rules):
1. **Controller Layer (REST API Endpoints)**:
   - Chỉ chịu trách nhiệm tiếp nhận HTTP Request, validate tham số đầu vào (`@Valid`, DTOs), kiểm tra JWT Auth/RBAC, và trả về response theo chuẩn **API Envelope (`success`, `code`, `message`, `data`, `timestamp`)**.
   - Tuyệt đối **KHÔNG** chứa logic xử lý nghiệp vụ hay truy vấn CSDL trực tiếp trong Controller.
   - Tích hợp OpenAPI / Swagger Annotations đầy đủ cho mọi Endpoint.
   - Xử lý ngoại lệ tập trung qua Global Exception Handler (`@ControllerAdvice`), chuyển đổi Exception thành Response JSON chuẩn (HTTP 400, 401, 403, 404, 500).
2. **Service Layer (Business Logic & Enforcement)**:
   - Chứa 100% logic nghiệp vụ của hệ thống:
     - **RBAC Data Isolation**: `CLIENT_MANAGER` chỉ truy cập/thao tác dữ liệu thuộc Client mà mình phụ trách (`managerId == currentUserId`).
     - **Instance Deletion Policy**: Kiểm tra trạng thái Instance trước khi xóa. Nếu `status == 'RUNNING'`, ngắt xử lý và ném ngoại lệ `InvalidOperationException` -> HTTP 400 Bad Request. Chỉ cho phép xóa khi `STOPPED` hoặc `ERROR`.
     - **Auto Alert & Deduplication**: Logic tự phát sinh cảnh báo khi `cpuUsage >= 80` (`HIGH_CPU`) hoặc `status == 'ERROR'` (`SYSTEM_ERROR`), đồng thời kiểm tra chống tạo trùng Alert nếu đã có Alert chưa xử lý (`isResolved == false`) cho cùng Instance.
     - **Cost Forecast & SLA Calculation**: Tính toán chi phí dự báo dựa trên các Instance đang `RUNNING` và chỉ số phần trăm SLA theo uptime.
3. **Repository Layer (Data Access & Persistence)**:
   - Trích xuất và thao tác dữ liệu với PostgreSQL Tables.
   - Sử dụng Spring Data JPA Repositories (hoặc Hibernate / JDBC).
   - Quản lý các truy vấn SQL (SQL Aggregations: JOIN, GROUP BY, SUM, AVG) phục vụ thống kê chi phí, dự báo và tính toán SLA.
   - Quản lý tham chiếu Khóa ngoại (Foreign Key / Primary Key) chính xác giữa các Bảng.
4. **Entity / Model Layer (Data Schema Definitions)**:
   - Định nghĩa cấu trúc Entity `@Entity` `@Table` mapping 1:1 với 5 Bảng PostgreSQL (`members`, `clients`, `instances`, `alerts`, `cost_snapshots`).
   - Đảm bảo các thuộc tính bắt buộc, định dạng kiểu dữ liệu (Long/BIGINT, String/VARCHAR, Double/DOUBLE PRECISION, Boolean, Timestamp/Date) và trường audit (`createdAt`, `updatedAt`, `lastUpdated`).

---

## 3. QUY CHUẨN CƠ SỞ DỮ LIỆU POSTGRESQL & PGADMIN 4 (DATABASE GUIDELINE)
Toàn bộ thiết kế dữ liệu tuân thủ mô hình CSDL Quan hệ (Relational RDBMS) trên **PostgreSQL**, quản lý và trực quan hóa qua công cụ GUI **pgAdmin 4**, chi tiết tại [specification_OJTprj.md](file:///d:/OTJprj_TechValley/specification_OJTprj.md#L41-L111).

### Các quy tắc CSDL cốt lõi:
1. **Chuẩn tên Bảng & Cột (Naming Conventions)**:
   - Tên Bảng (Tables) dùng chữ thường snake_case/plural: `members`, `clients`, `instances`, `alerts`, `cost_snapshots`.
   - Tên cột (Columns) chính xác trong DB Schema:
     - `members`: `id`, `name`, `password`, `role`, `created_at`
     - `clients`: `id`, `client_name`, `contract_plan`, `create_at`, `manager_id`
     - `instances`: `id`, `client_id`, `cpu_usage`, `instance_name`, `instance_type`, `launche_at`, `monthly_cost`, `region`, `status`, `update_at`
     - `alerts`: `id`, `instance_id`, `alert_type`, `detected_at`, `is_resolve`, `message`, `resolve_at`
     - `cost_snapshots`: `id`, `client_id`, `year_month`, `total_cost`, `recorded_at`
2. **Mô hình Liên kết Khóa Ngoại (Foreign Key Relationships)**:
   - `clients.manager_id` -> Khóa ngoại tham chiếu tới `members.id` (PRIMARY KEY `id` - BIGINT/BIGSERIAL).
   - `instances.client_id` -> Khóa ngoại tham chiếu tới `clients.id` (PRIMARY KEY `id` - BIGINT/BIGSERIAL).
   - `alerts.instance_id` -> Khóa ngoại tham chiếu tới `instances.id` (PRIMARY KEY `id` - BIGINT/BIGSERIAL).
   - `cost_snapshots.client_id` -> Khóa ngoại tham chiếu tới `clients.id` (PRIMARY KEY `id` - BIGINT/BIGSERIAL).
3. **Quản lý Chỉ mục (Indexing Strategy in pgAdmin 4)**:
   - Đánh Unique Index / Unique Constraint cho `clients.client_name`.
   - Đánh Single/Composite Index cho các cột tần suất truy vấn cao: `clients.manager_id`, `instances.client_id`, `instances.status`, `alerts.instance_id`, `alerts.is_resolve`.
4. **Quy chuẩn Quản trị qua pgAdmin 4**:
   - Đảm bảo các truy vấn SQL (như tính tổng `monthly_cost`, nhóm `alerts` theo `alert_type`) chạy hiệu quả và kiểm thử thành công trên pgAdmin 4 Query Tool trước khi chuyển giao vào Repository layer.
