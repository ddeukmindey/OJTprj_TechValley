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

## 3. Thiết kế dữ liệu
### Bảng chính
- `members`: thông tin tài khoản, role, createdAt.
- `clients`: thông tin khách hàng, contractPlan, managerId.
- `instances`: thông tin instance, region, type, status, cpuUsage, monthlyCost.
- `alerts`: lưu cảnh báo, loại cảnh báo, trạng thái xử lý.
- `cost_snapshots`: lưu tổng chi phí theo tháng cho từng client.

### Lưu ý thiết kế
- `clients.managerId` tham chiếu `members.id`.
- `instances.clientId` tham chiếu `clients.id`.
- `alerts.instanceId` tham chiếu `instances.id`.
- Cần mô tả rõ PK/FK trên ERD để giải thích được quan hệ.

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

## 6. Phân chia công việc đề xuất
### Member A
- Lead ERD.
- Auth API.
- Client API.

### Member B
- Instance register/retrieve/delete.
- Filter, pagination, sort.

### Member C
- Status change API.
- Monitoring API.

### Member D
- Alert API.
- Cost forecast.
- SLA calculation.

### Member E
- LLM feature.
- Swagger.
- Git management.
- PPT lead.

## 7. Kế hoạch thực hiện theo thời gian
### Giai đoạn 1: Thống nhất thiết kế
- Chốt ERD.
- Chốt luồng auth và role.
- Chốt format response chung.
- Chốt Git flow.

### Giai đoạn 2: Coding
- Làm entity, repository, service, controller.
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
- Trình bày ERD và API.
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
- [ ] ERD có quan hệ rõ ràng.
- [ ] Auth hoạt động với JWT.
- [ ] Role-based access control đúng.
- [ ] Monitoring tự tạo alert đúng.
- [ ] SLA và cost forecast tính hợp lý.
- [ ] LLM feature chạy được.
- [ ] Swagger đầy đủ endpoint.
- [ ] Git history sạch và đúng flow.
- [ ] PPT có giải thích thiết kế, không chỉ liệt kê chức năng.