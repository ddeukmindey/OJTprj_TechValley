# Testing Alert Service

## Chuẩn bị
- PostgreSQL chạy.
- cloud-monitor-service chạy (port 8081).
- Có sẵn vài dòng dữ liệu trong bảng `alerts` (insert tay để test).

## Test xem tất cả

GET http://localhost:8081/api/alerts

Kỳ vọng:
- HTTP 200
- `data.items` là mảng alert, `data.pagination` có currentPage/pageSize/totalElements/totalPages.

## Test lọc theo loại

GET http://localhost:8081/api/alerts?alertType=CPU_HIGH

Kỳ vọng:
- HTTP 200
- Tất cả item trong `data.items` có `alertType = CPU_HIGH`.

## Test lọc theo trạng thái xử lý

GET http://localhost:8081/api/alerts?isResolved=false

Kỳ vọng:
- HTTP 200
- Tất cả item có `resolved = false`.

## Test lọc theo 1 ngày

GET http://localhost:8081/api/alerts?date=2026-07-15

Kỳ vọng:
- HTTP 200
- Chỉ trả alert có `detectedAt` trong ngày 2026-07-15.

## Test lọc theo tháng

GET http://localhost:8081/api/alerts?month=2026-07

Kỳ vọng:
- HTTP 200
- Chỉ trả alert có `detectedAt` trong tháng 07/2026.

## Test lọc theo khoảng ngày

GET http://localhost:8081/api/alerts?fromDate=2026-07-01&toDate=2026-07-20

Kỳ vọng:
- HTTP 200
- Chỉ trả alert có `detectedAt` nằm trong khoảng 2 ngày trên.

## Test kết hợp nhiều filter + phân trang

GET http://localhost:8081/api/alerts?isResolved=false&month=2026-07&page=1&size=5

Kỳ vọng:
- HTTP 200
- Kết quả thoả đồng thời tất cả điều kiện, `data.items` tối đa 5 phần tử.

## Test đánh dấu đã xử lý

PATCH http://localhost:8081/api/alerts/1/resolve

(không cần body)

Kỳ vọng:
- HTTP 200
- `data.resolved = true`, `data.resolvedAt` có giá trị.

## Test resolve alert đã xử lý rồi

Gọi lại đúng API trên với cùng id vừa resolve ở bước trước.

Kỳ vọng:
- HTTP 400
- message: "Cảnh báo ID {id} đã được xử lý trước đó."

## Test resolve alert không tồn tại

PATCH http://localhost:8081/api/alerts/9999/resolve

Kỳ vọng:
- HTTP 404
- message: "Không tìm thấy cảnh báo với ID: 9999"

## Test tham số filter sai định dạng

GET http://localhost:8081/api/alerts?date=15-07-2026

Kỳ vọng:
- HTTP 400
- `errors` chứa field `date` không hợp lệ.

## Checklist

- Xem tất cả OK
- Filter theo loại OK
- Filter theo ngày/tháng/khoảng ngày OK
- Filter theo trạng thái xử lý OK
- Phân trang OK
- Resolve alert thành công OK
- Resolve alert đã xử lý bị chặn (400) OK
- Resolve alert không tồn tại trả 404 OK
- Exception trả đúng định dạng ApiResponse
