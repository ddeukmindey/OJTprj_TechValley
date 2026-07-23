# Kế hoạch OJT Developer Track - TechValley

## 1. Mục tiêu
Xây dựng Cloud Instance Monitoring System REST API cho TechValley, tập trung vào giám sát instance, cảnh báo tự động, dự báo chi phí, SLA, và 1 LLM feature.

## 2. Phạm vi chức năng
- Auth API: login bằng JWT.
- Client API: đăng ký và quản lý client.
- Instance API: đăng ký, xem danh sách, xem chi tiết, đổi trạng thái, xóa instance.
- Monitoring API: cảnh báo CPU cao, lỗi, stopped lâu.
- Alert API: xem lịch sử cảnh báo, đánh dấu đã xử lý.
- Report API: báo cáo tổng quan, cost forecast, SLA.
- LLM API: chọn 1 chức năng hỗ trợ tự động.

## 3. Thiết kế dữ liệu (MongoDB Collections)
### Collections chính
- `members`: thông tin tài khoản, role, createdAt.
- `clients`: thông tin khách hàng, contractPlan, managerId.
- `instances`: thông tin instance, region, type, status, cpuUsage, monthlyCost.
- `alerts`: lưu cảnh báo, loại cảnh báo, trạng thái xử lý (`isResolved`).
- `cost_snapshots`: lưu tổng chi phí theo tháng cho từng client.

### Lưu ý thiết kế MongoDB
- `clients.managerId` tham chiếu tới `members._id` (ObjectId).
- `instances.clientId` tham chiếu tới `clients._id` (ObjectId).
- `alerts.instanceId` tham chiếu tới `instances._id` (ObjectId).
- Cần mô tả rõ cấu trúc Document & liên kết ObjectId trên MongoDB Compass để giải thích được quan hệ.

## 4. API cần triển khai
### Auth
- `POST /api/auth/login`

### Client
- `POST /api/clients`
- `GET /api/clients`
- `GET /api/clients/{id}/instances`
- `GET /api/clients/{id}/cost`
- `GET /api/clients/{id}/cost-forecast`
- `GET /api/clients/{id}/sla`

### Instance
- `POST /api/instances`
- `GET /api/instances`
- `GET /api/instances/{id}`
- `PATCH /api/instances/{id}/status`
- `DELETE /api/instances/{id}`

### Monitoring
- `GET /api/monitor/warnings`
- `GET /api/monitor/errors`
- `GET /api/monitor/long-stopped`
- `GET /api/monitor/report`

### Alert
- `GET /api/alerts`
- `PATCH /api/alerts/{id}/resolve`

### LLM
- Chọn 1 trong 3:
  - `GET /api/instances/{id}/diagnosis`
  - `GET /api/monitor/report/summary`
  - `GET /api/clients/{id}/cost-optimization`

## 5. Business logic quan trọng
- JWT xác thực cho các API cần phân quyền.
- `ADMIN` quản lý toàn bộ dữ liệu.
- `CLIENT_MANAGER` chỉ quản lý client được giao.
- `GET /api/monitor/warnings` tự tạo alert nếu `cpuUsage >= 80`.
- `GET /api/monitor/errors` tự tạo alert critical nếu instance ở trạng thái `ERROR`.
- Không tạo alert trùng nếu đã có alert chưa resolved cho cùng instance.
- Forecast chi phí dựa trên instance `RUNNING`.
- SLA tính theo tỷ lệ thời gian `RUNNING` so với tổng giờ trong tháng.
- `RUNNING` không được xóa; `STOPPED` và `ERROR` được xóa.

## 6. Phân chia công việc theo từng mô-đun chức năng (Feature Allocation)
### Member A - Chức năng 1: Xác thực & Phân quyền (Auth & Security)
- Lead thiết kế Database & Sơ đồ ERD.
- Module Auth API: `POST /api/auth/login`.
- JWT Token Filter & Cơ chế Phân quyền Role-Based Access Control (RBAC).

### Member B - Chức năng 2: Quản lý Khách hàng (Client Management)
- Module Client API: `POST /api/clients`, `GET /api/clients` (Phân trang, Lọc, Search).
- API Quan hệ Client-Instance: `GET /api/clients/{id}/instances`.
- Phân quyền giới hạn dữ liệu theo `managerId`.

### Member C - Chức năng 3: Quản lý Máy chủ ảo (Instance Management)
- Module Instance CRUD & Query: `POST /api/instances`, `GET /api/instances`, `GET /api/instances/{id}`.
- Module Instance Control: `PATCH /api/instances/{id}/status`, `DELETE /api/instances/{id}`.
- Validation Business Rule: Chặn xóa instance đang `RUNNING`.

### Member D - Chức năng 4: Giám sát Hạ tầng & Cảnh báo (Monitoring & Alert System)
- Module Monitoring API: `GET /api/monitor/warnings`, `/errors`, `/long-stopped`, `/report`.
- Module Alert API: `GET /api/alerts`, `PATCH /api/alerts/{id}/resolve`.
- Logic tự động tạo Alert & Chống trùng lặp Alert (Alert Deduplication Check).

### Member E - Chức năng 5: Phân tích Chi phí, SLA & AI (Cost, SLA & LLM Feature)
- Module Cost & SLA: `GET /api/clients/{id}/cost`, `cost-forecast`, `sla`.
- Module AI Integration: `GET /api/instances/{id}/diagnosis` (LLM Feature).
- Swagger Documentation, Git Management (Review PR/Merge), Lead tổng hợp PPT & Demo.

---
### Giai đoạn 1: Thống nhất thiết kế
- Chốt MongoDB Collections & Schema.
- Chốt luồng auth và role.
- Chốt format response chung.
- Chốt Git flow.

### Giai đoạn 2: Coding
- Làm entity/document, repository, service, controller.
- Viết logic monitoring, alert, cost, SLA.
- Bổ sung validation và exception handling.

### Giai đoạn 3: Kiểm thử và tích hợp
- Merge từng nhánh vào `develop`.
- Test API bằng Swagger/Postman.
- Sửa lỗi logic và conflict.

### Giai đoạn 4: Hoàn thiện demo
- Chọn 1 LLM feature.
- Chụp màn hình Swagger.
- Chuẩn bị dữ liệu demo.

### Giai đoạn 5: Làm PPT
- Viết nội dung problem definition.
- Trình bày MongoDB Schema và API.
- Giải thích business logic.
- Chuẩn bị phần Q&A.

## 8. Git flow
- `main`: bản final.
- `develop`: nhánh tích hợp.
- `feature/{name}`: nhánh làm việc cá nhân.
- Mỗi người làm trên feature branch riêng.
- Tạo PR vào `develop`.
- Có ít nhất 1 teammate review.
- Commit theo chuẩn: `feat`, `fix`, `docs`, `refactor`, `test`.

## 9. Sản phẩm cuối cùng
- File PPT.
- GitHub link có đầy đủ branch, commit, PR.
- Swagger documentation.
- Demo API hoạt động đúng logic.

## 10. Checklist trước khi nộp
- [ ] MongoDB Schema & Document relationships có cấu trúc rõ ràng.
- [ ] Auth hoạt động với JWT.
- [ ] Role-based access control đúng.
- [ ] Monitoring tự tạo alert đúng.
- [ ] SLA và cost forecast tính hợp lý.
- [ ] LLM feature chạy được.
- [ ] Swagger đầy đủ endpoint.
- [ ] Frontend tuân thủ nghiêm ngặt theo [UInUX_guideline.md](file:///d:/OTJprj_TechValley/UInUX_guideline.md).
- [ ] Git history sạch và đúng flow.
- [ ] PPT có giải thích thiết kế, không chỉ liệt kê chức năng.