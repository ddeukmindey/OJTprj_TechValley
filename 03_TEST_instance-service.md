# Test chi tiết — instance-service (port 8081)

Yêu cầu trước: đã có `$TOKEN`, `$TOKEN_MANAGER` (file 01) và `$CLIENT_ID` (file 02).

## 1. Danh sách endpoint thực tế trong code

### API public (`/api/instances`)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/instances` | Đăng ký instance mới |
| GET | `/api/instances` | Danh sách (phân trang/filter/sort) |
| GET | `/api/instances/{id}` | Chi tiết 1 instance |
| PATCH | `/api/instances/{id}/status` | Cập nhật trạng thái |
| DELETE | `/api/instances/{id}` | Xoá (chặn nếu đang RUNNING) |

### API nội bộ (`/internal/instances`) — không public, chỉ để `monitoring-service`/`client-service` gọi

| Method | Endpoint | Dùng bởi |
|---|---|---|
| GET | `/internal/instances/high-cpu?clientIds=` | monitoring-service |
| GET | `/internal/instances/errors?clientIds=` | monitoring-service |
| GET | `/internal/instances/stopped?clientIds=` | monitoring-service |
| GET | `/internal/instances?clientIds=` | monitoring-service, client-service |

## 2. Test Case 1 — Tạo Instance mới (CPU bình thường)

```bash
curl -X POST http://localhost:8081/api/instances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientId": '"$CLIENT_ID"',
    "name": "web-server-01",
    "region": "ap-southeast-1",
    "type": "MEDIUM",
    "status": "RUNNING",
    "cpuUsage": 45.5,
    "monthlyCost": 120.0
  }'
```
**Kết quả mong đợi:** HTTP 201. **Lưu lại `$INSTANCE_ID`** (dùng cho các test sau + file `04_TEST_monitoring-service.md`).

## 3. Test Case 2 — Tạo thêm 2 Instance đặc biệt để test Monitoring sau này

**Instance CPU cao (để test `/api/monitor/warnings`):**
```bash
curl -X POST http://localhost:8081/api/instances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientId": '"$CLIENT_ID"',
    "name": "db-server-high-cpu",
    "region": "ap-southeast-1",
    "type": "LARGE",
    "status": "RUNNING",
    "cpuUsage": 92.0,
    "monthlyCost": 250.0
  }'
```
→ Lưu `$INSTANCE_ID_HIGH_CPU`

**Instance lỗi (để test `/api/monitor/errors`):**
```bash
curl -X POST http://localhost:8081/api/instances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientId": '"$CLIENT_ID"',
    "name": "app-server-error",
    "region": "ap-southeast-1",
    "type": "SMALL",
    "status": "ERROR",
    "cpuUsage": 10.0,
    "monthlyCost": 50.0
  }'
```
→ Lưu `$INSTANCE_ID_ERROR`

## 4. Test Case 3 — Validate thiếu field bắt buộc

```bash
curl -X POST http://localhost:8081/api/instances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"clientId": '"$CLIENT_ID"'}'
```
**Kết quả mong đợi:** HTTP 400, liệt kê đủ lỗi validate cho `name`, `region`, `type`, `status`, `cpuUsage`, `monthlyCost` (do các annotation `@NotBlank`/`@NotNull`/`@Min` trong `InstanceRequest.java`).

## 5. Test Case 4 — Lấy danh sách Instance có filter

```bash
# Filter theo client
curl "http://localhost:8081/api/instances?clientId=$CLIENT_ID&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Filter theo status
curl "http://localhost:8081/api/instances?status=ERROR&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Filter theo region + sort
curl "http://localhost:8081/api/instances?region=ap-southeast-1&sortBy=cpuUsage&sortOrder=desc" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Mỗi filter trả đúng tập con kỳ vọng, sort đúng thứ tự.

## 6. Test Case 5 — Cập nhật trạng thái Instance

```bash
curl -X PATCH http://localhost:8081/api/instances/$INSTANCE_ID/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status": "STOPPED", "cpuUsage": 0}'
```
**Kết quả mong đợi:** HTTP 200, `status` đổi thành `STOPPED`, `updateAt` được cập nhật.

## 7. Test Case 6 — Xoá Instance đang RUNNING (phải bị chặn)

```bash
curl -X DELETE http://localhost:8081/api/instances/$INSTANCE_ID_HIGH_CPU \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 400 hoặc 409, báo lỗi dạng `ActiveInstanceException` — theo đúng "Instance Deletion Rules" trong đề bài (`RUNNING → deletion blocked`). Đây là 1 trong các tiêu chí chấm điểm "Business Logic".

## 8. Test Case 7 — Xoá Instance đã STOPPED (phải cho phép)

```bash
curl -X DELETE http://localhost:8081/api/instances/$INSTANCE_ID \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200/204 — vì `$INSTANCE_ID` đã đổi sang `STOPPED` ở Test Case 5. ⚠️ Sau bước này `$INSTANCE_ID` không còn tồn tại — nếu muốn giữ lại để test tiếp Monitoring, **bỏ qua test case này** hoặc tạo Instance mới khác để xoá thử.

## 9. Test Case 8 — Instance ID không tồn tại

```bash
curl http://localhost:8081/api/instances/99999 \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 404.

## 10. Test Case 9 — Test trực tiếp endpoint nội bộ (kiểm tra hạ tầng cho monitoring-service)

Test riêng để xác nhận `/internal/...` hoạt động đúng TRƯỚC KHI qua file `04_TEST_monitoring-service.md` — không cần JWT vì các endpoint này không nằm dưới `/api/**`:

```bash
# Danh sách instance CPU >= 80%
curl "http://localhost:8081/internal/instances/high-cpu"
# Kỳ vọng: thấy "db-server-high-cpu" (cpuUsage=92.0) trong kết quả

# Danh sách instance ERROR
curl "http://localhost:8081/internal/instances/errors"
# Kỳ vọng: thấy "app-server-error" trong kết quả

# Lọc theo clientIds cụ thể
curl "http://localhost:8081/internal/instances?clientIds=$CLIENT_ID"
# Kỳ vọng: thấy toàn bộ instance đã tạo cho client này
```

Nếu 3 lệnh trên trả về `[]` dù bạn vừa tạo instance khớp điều kiện — kiểm tra lại:
1. Instance có đúng `cpuUsage`/`status` như đã tạo không (`GET /api/instances/{id}`)
2. `InstanceRepository.java` có đủ method `findByCpuUsageGreaterThanEqual`, `findByStatus` chưa bị xoá nhầm

## 11. Checklist hoàn thành

- [ ] Tạo Instance thành công (3 cái: bình thường, CPU cao, lỗi), lưu đủ 3 ID
- [ ] Validate thiếu field hoạt động đúng
- [ ] Filter theo `clientId`/`status`/`region` + sort đúng
- [ ] Update status thành công
- [ ] Xoá Instance đang RUNNING bị chặn (400/409)
- [ ] Xoá Instance đã STOPPED thành công
- [ ] Instance không tồn tại trả 404
- [ ] `/internal/instances/high-cpu`, `/errors`, `?clientIds=` trả đúng dữ liệu — **đây là điều kiện tiên quyết để file 04 chạy đúng**
