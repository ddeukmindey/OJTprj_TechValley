# Test chi tiết — monitoring-service (port 8083)

Yêu cầu trước: đã có `$TOKEN`, `$TOKEN_MANAGER` (file 01), và đặc biệt **đã tạo xong instance CPU cao (`db-server-high-cpu`, cpuUsage=92.0) + instance lỗi (`app-server-error`, status=ERROR)** ở file `03_TEST_instance-service.md`, mục Test Case 2.

⚠️ **Service này là nơi rủi ro lỗi cao nhất** sau đợt refactor — vì toàn bộ `MonitoringServiceImpl.java` đã được viết lại để gọi `WebClient` sang `instance-service`, `alert-service`, `client-service` thay vì đọc thẳng DB. Test kỹ từng bước.

## 1. Danh sách endpoint thực tế trong code

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/monitor/warnings` | Instance CPU ≥ 80%, tự động ghi Alert `CPU_HIGH` |
| GET | `/api/monitor/errors` | Instance đang ERROR, tự động ghi Alert `ERROR_DETECTED` |
| GET | `/api/monitor/long-stopped` | Instance STOPPED quá 48 giờ, tự động ghi Alert `LONG_STOPPED` |
| GET | `/api/monitor/report` | Báo cáo tổng quan (đếm theo status, alert chưa xử lý, tổng client) |

## 2. Chuẩn bị — xác nhận `alert-service` đang chạy trước khi test

Vì mọi endpoint ở đây đều gọi sang `alert-service` để ghi Alert, kiểm tra trước:
```bash
curl "http://localhost:8084/internal/alerts/count-unresolved"
```
Nếu lệnh này lỗi (connection refused) — `alert-service` chưa chạy, phải fix trước khi test tiếp phần dưới.

## 3. Test Case 1 — `/warnings` (CPU cao)

```bash
curl "http://localhost:8083/api/monitor/warnings" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, danh sách chứa `db-server-high-cpu` (cpuUsage=92.0), mỗi item có `warningMessage` dạng "Cảnh báo: CPU usage >= 80%".

**Bước xác nhận quan trọng — Alert phải được tự động tạo:**
```bash
curl "http://localhost:8084/api/alerts?alertType=CPU_HIGH" \
  -H "Authorization: Bearer $TOKEN"
```
Kỳ vọng: thấy 1 Alert mới với `instanceId` = id của `db-server-high-cpu`, `isResolved = false`.

## 4. Test Case 2 — Gọi lại `/warnings` lần 2 (test chống trùng — dedup)

```bash
curl "http://localhost:8083/api/monitor/warnings" \
  -H "Authorization: Bearer $TOKEN"
```
Rồi kiểm tra lại:
```bash
curl "http://localhost:8084/api/alerts?alertType=CPU_HIGH" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** **Vẫn chỉ có đúng 1 Alert** `CPU_HIGH` cho instance đó — KHÔNG được tạo thêm bản ghi trùng. Đây chính là logic "skip if unresolved Alert already exists" theo đúng yêu cầu đề bài, và cũng là điểm dễ vỡ nhất sau khi đổi từ `alertRepository.save()` trực tiếp sang gọi API `POST /internal/alerts` — nếu thấy **2 Alert trùng** xuất hiện, nghĩa là `AlertInternalController.createIfAbsent()` bên `alert-service` bị lỗi dedup, cần xem lại.

## 5. Test Case 3 — `/errors`

```bash
curl "http://localhost:8083/api/monitor/errors" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Danh sách chứa `app-server-error`, tự động sinh Alert `ERROR_DETECTED`. Verify tương tự Test Case 1 — gọi `GET /api/alerts?alertType=ERROR_DETECTED` bên `alert-service`.

## 6. Test Case 4 — `/long-stopped`

Test case này khó tạo dữ liệu thật (cần Instance STOPPED từ 48 giờ trước) — 2 cách:

**Cách 1 (nhanh, để demo):** Sửa trực tiếp `updateAt` của 1 Instance STOPPED trong DB `techvalley_instance` lùi về quá khứ:
```sql
UPDATE instances
SET status = 'STOPPED', update_at = NOW() - INTERVAL '50 hours'
WHERE id = <id-instance-bat-ky>;
```

**Cách 2 (đúng nhất nhưng chậm):** Chờ thật 48 giờ sau khi 1 instance chuyển sang STOPPED — không khả thi trong thời gian làm đồ án, dùng Cách 1 để demo.

```bash
curl "http://localhost:8083/api/monitor/long-stopped" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Instance vừa sửa xuất hiện trong danh sách, tự sinh Alert `LONG_STOPPED`.

## 7. Test Case 5 — `/report` (tổng hợp)

```bash
curl "http://localhost:8083/api/monitor/report" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:**
```json
{
  "totalInstances": ...,
  "runningInstances": ...,
  "stoppedInstances": ...,
  "errorInstances": ...,
  "averageCpuUsage": ...,
  "unresolvedAlerts": ...,
  "totalClients": ...
}
```
Đối chiếu tay: `totalInstances = runningInstances + stoppedInstances + errorInstances`, `unresolvedAlerts` phải khớp số Alert `isResolved=false` hiện có (kiểm tra qua `GET /api/alerts?isResolved=false` bên `alert-service`).

## 8. Test Case 6 — Phân quyền CLIENT_MANAGER (chỉ thấy dữ liệu của Client mình quản lý)

```bash
curl "http://localhost:8083/api/monitor/warnings" \
  -H "Authorization: Bearer $TOKEN_MANAGER"
```
**Kết quả mong đợi:** Nếu `$TOKEN_MANAGER` không phải người quản lý client chứa `db-server-high-cpu` → kết quả rỗng `[]`, KHÔNG được thấy instance của client khác. Đây là test quan trọng — verify `getManagedClientIdsIfManager()` trong `MonitoringServiceImpl` hoạt động đúng (gọi `GET /internal/clients/by-manager/{id}` sang `client-service`).

## 9. Test Case 7 — Không có token (chặn truy cập trái phép)

```bash
curl "http://localhost:8083/api/monitor/warnings"
```
**Kết quả mong đợi:** HTTP 401/403.

## 10. Debug khi có lỗi 500

Nếu bất kỳ test case nào ở trên trả 500, kiểm tra theo thứ tự:

```bash
# 1. Xem log monitoring-service — thường sẽ thấy lỗi kết nối tới service khác
docker logs monitoring-service --tail 50

# 2. Xác nhận instance-service, alert-service, client-service đều đang chạy
docker ps -a | grep -E "instance-service|alert-service|client-service"

# 3. Xác nhận biến môi trường URL đúng trong container
docker exec monitoring-service env | grep -E "INSTANCE_SERVICE_URL|ALERT_SERVICE_URL|CLIENT_SERVICE_URL"
# Kỳ vọng: http://instance-service:8081, http://alert-service:8084, http://client-service:8082
```

Lỗi thường gặp nhất: URL trong `application.yml`/`docker-compose.yml` trỏ nhầm `localhost` thay vì tên container (`instance-service`) — vì bên trong Docker network, các container gọi nhau bằng tên service, không phải `localhost`.

## 11. Checklist hoàn thành

- [ ] `/warnings` trả đúng, tự tạo Alert `CPU_HIGH`
- [ ] Gọi `/warnings` lần 2 KHÔNG tạo Alert trùng (dedup hoạt động đúng qua kiến trúc mới)
- [ ] `/errors` trả đúng, tự tạo Alert `ERROR_DETECTED`
- [ ] `/long-stopped` trả đúng (test bằng cách sửa DB thủ công)
- [ ] `/report` số liệu khớp thực tế
- [ ] CLIENT_MANAGER chỉ thấy dữ liệu của mình
- [ ] Không token bị chặn 401/403
- [ ] Không còn lỗi 500 do kết nối service
