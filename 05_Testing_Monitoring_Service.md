# Testing Monitoring Service

## Chuẩn bị
- PostgreSQL chạy (database `techvalley_db`).
- `monitoring-service` chạy (port 8083) hoặc `cloud-monitor-service` (port 8081).
- Đã có dữ liệu các `instances` trong CSDL với các kịch bản:
  - Instance có `cpuUsage >= 80%` (để test cảnh báo CPU cao).
  - Instance có trạng thái `status = 'ERROR'` (để test máy chủ gặp sự cố).
  - Instance có trạng thái `status = 'STOPPED'` và thời gian ngưng hoạt động (`launcheAt`/`updateAt`) ít nhất 48 giờ (để test máy chủ tạm dừng lâu ngày).
  - Khách hàng (`clients`) và cảnh báo (`alerts`) phục vụ báo cáo tổng quan.

## Test lấy danh sách cảnh báo CPU cao (Warnings)

Mở Postman / Curl:

GET http://localhost:8083/api/monitor/warnings

Kỳ vọng:
- HTTP 200 OK
- Response định dạng ApiResponse: `success = true`, `code = 200`.
- `data` trả về danh sách các instance có `cpuUsage >= 80.0`.
- Mỗi instance chứa thuộc tính `warningMessage`: `"Cảnh báo: CPU usage >= 80%"`.
- Tự động phát sinh 1 bản ghi trong bảng `alerts` với `alert_type = 'CPU_HIGH'` và `is_resolved = 0` cho instance chưa có cảnh báo chưa xử lý.

## Test lấy danh sách máy chủ bị lỗi (Errors)

GET http://localhost:8083/api/monitor/errors

Kỳ vọng:
- HTTP 200 OK
- `data` trả về danh sách các instance có `status = 'ERROR'`.
- Mỗi instance chứa thuộc tính `warningMessage`: `"Lỗi: Máy chủ đang gặp sự cố (ERROR)"`.
- Tự động tạo bản ghi trong bảng `alerts` với `alert_type = 'ERROR_DETECTED'` và `is_resolved = 0`.

## Test lấy danh sách máy chủ tạm dừng lâu ngày (Long Stopped)

GET http://localhost:8083/api/monitor/long-stopped

Kỳ vọng:
- HTTP 200 OK
- `data` trả về mảng các instance ở trạng thái `STOPPED` ngưng hoạt động từ 48 giờ trở lên.
- Mỗi instance chứa `warningMessage`: `"Cảnh báo: Máy chủ bị tạm dừng ít nhất 48 giờ"`.
- Tự động phát sinh bản ghi cảnh báo `LONG_STOPPED` với `is_resolved = 0`.

## Test chống tạo trùng lặp cảnh báo (Deduplication Check)

Thực hiện gọi lại GET http://localhost:8083/api/monitor/warnings (hoặc `/errors`) lần thứ 2.

Kỳ vọng:
- HTTP 200 OK
- Dữ liệu trả về danh sách instance phù hợp.
- Bảng `alerts` không phát sinh thêm bản ghi trùng lặp (chỉ duy trì 1 alert chưa resolve hiện tại cho từng instance).

## Test lấy báo cáo tổng quan hạ tầng (Overview Report)

GET http://localhost:8083/api/monitor/report

Kỳ vọng:
- HTTP 200 OK
- `data` là đối tượng chứa thông tin tổng hợp:
  - `totalInstances`: Tổng số instance trong hệ thống.
  - `runningInstances`: Số instance đang hoạt động (`RUNNING`).
  - `stoppedInstances`: Số instance đang tạm dừng (`STOPPED`).
  - `errorInstances`: Số instance bị lỗi (`ERROR`).
  - `averageCpuUsage`: % CPU trung bình toàn hệ thống (đã làm tròn 2 chữ số thập phân).
  - `unresolvedAlerts`: Tổng số cảnh báo chưa xử lý (`is_resolved = 0`).
  - `totalClients`: Tổng số lượng khách hàng.

## Test kịch bản hệ thống bình thường / rỗng (Empty / Healthy State)

Khi toàn bộ máy chủ hoạt động bình thường (CPU < 80%, không có máy chủ ERROR hay ngưng hoạt động lâu ngày):

GET http://localhost:8083/api/monitor/warnings

Kỳ vọng:
- HTTP 200 OK
- `data` trả về mảng rỗng `[]`.

## Checklist

- Lấy danh sách cảnh báo CPU cao OK
- Lấy danh sách máy chủ bị lỗi OK
- Lấy danh sách máy chủ ngưng hoạt động >48h OK
- Tự động phát sinh Alert trong CSDL OK
- Chống tạo trùng lặp Alert (Deduplication) OK
- Lấy báo cáo tổng quan hạ tầng (Overview Report) OK
- Thống kê chỉ số CPU trung bình chính xác OK
- Trả về định dạng ApiResponse chuẩn OK
