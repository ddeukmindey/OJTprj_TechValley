# Test chi tiết — alert-service (port 8084)

Yêu cầu trước: đã chạy xong file `04_TEST_monitoring-service.md` để có sẵn Alert được tự động tạo (`CPU_HIGH`, `ERROR_DETECTED`).

## 1. Danh sách endpoint thực tế trong code

### API public (`/api/alerts`)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/alerts` | Lịch sử alert, filter theo `alertType`/`instanceId`/`isResolved`/`date`/`month`/`fromDate`/`toDate` |
| PATCH | `/api/alerts/{id}/resolve` | Đánh dấu đã xử lý |

### API nội bộ (`/internal/alerts`) — chỉ để `monitoring-service`/`client-service` gọi

| Method | Endpoint | Dùng bởi |
|---|---|---|
| POST | `/internal/alerts` | monitoring-service (tạo alert, tự dedup) |
| GET | `/internal/alerts/count-unresolved?instanceIds=` | monitoring-service (`/api/monitor/report`) |
| GET | `/internal/alerts/downtime?instanceIds=` | client-service (tính SLA) |

## 2. Test Case 1 — Xem toàn bộ Alert (không filter)

```bash
curl "http://localhost:8084/api/alerts" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, thấy các Alert đã được `monitoring-service` tự động tạo ở file 04 (`CPU_HIGH`, `ERROR_DETECTED`). **Lưu lại 1 `$ALERT_ID`** để test resolve ở bước sau.

## 3. Test Case 2 — Filter theo `alertType`

```bash
curl "http://localhost:8084/api/alerts?alertType=CPU_HIGH" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Chỉ trả Alert loại `CPU_HIGH`.

## 4. Test Case 3 — Filter theo `instanceId`

```bash
curl "http://localhost:8084/api/alerts?instanceId=$INSTANCE_ID_HIGH_CPU" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Chỉ trả Alert của đúng instance đó.

## 5. Test Case 4 — Filter theo `isResolved`

```bash
curl "http://localhost:8084/api/alerts?isResolved=false" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Chỉ trả Alert chưa xử lý — số lượng phải khớp với `unresolvedAlerts` trong `GET /api/monitor/report` (test ở file 04).

## 6. Test Case 5 — Filter theo khoảng ngày

```bash
curl "http://localhost:8084/api/alerts?fromDate=2026-08-01&toDate=2026-08-31" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Chỉ trả Alert có `detectedAt` nằm trong khoảng ngày trên. Test thêm với khoảng ngày không chứa alert nào (ví dụ tháng trước) → kỳ vọng danh sách rỗng.

## 7. Test Case 6 — Đánh dấu Alert đã xử lý

```bash
curl -X PATCH http://localhost:8084/api/alerts/$ALERT_ID/resolve \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, `isResolved` chuyển thành `true`, `resolvedAt` được set.

**Xác nhận lại:**
```bash
curl "http://localhost:8084/api/alerts?instanceId=$INSTANCE_ID_HIGH_CPU" \
  -H "Authorization: Bearer $TOKEN"
```
Alert đó phải hiện `isResolved: true`.

## 8. Test Case 7 — Resolve lại Alert đã resolve (test idempotent/lỗi hợp lý)

```bash
curl -X PATCH http://localhost:8084/api/alerts/$ALERT_ID/resolve \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Tuỳ thiết kế — hoặc trả 200 (không đổi gì, coi như thành công), hoặc trả lỗi rõ ràng kiểu "Alert đã được xử lý trước đó". Miễn là **không crash 500**.

## 9. Test Case 8 — Resolve Alert không tồn tại

```bash
curl -X PATCH http://localhost:8084/api/alerts/99999/resolve \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 404.

## 10. Test Case 9 — Verify lại: sau khi resolve, Alert CPU_HIGH mới có xuất hiện lại không (test dedup theo `isResolved`)

Quay lại gọi `monitoring-service`:
```bash
curl "http://localhost:8083/api/monitor/warnings" \
  -H "Authorization: Bearer $TOKEN"
```
Nếu instance CPU cao **vẫn còn CPU >= 80%** sau khi Alert cũ đã resolve — kỳ vọng lần này **CÓ tạo Alert mới** (vì logic dedup chỉ check `isResolved = 0`, alert cũ đã resolve thì không tính là "đang tồn tại" nữa). Đây là hành vi đúng theo đề bài: "skip if unresolved Alert already exists" — ngầm định là **được phép tạo lại nếu alert cũ đã resolve nhưng vấn đề tái diễn**.

## 11. Test Case 10 — Test trực tiếp endpoint nội bộ

```bash
# Test tạo alert qua API nội bộ (mô phỏng đúng cách monitoring-service gọi)
curl -X POST http://localhost:8084/internal/alerts \
  -H "Content-Type: application/json" \
  -d '{"instanceId": '"$INSTANCE_ID"', "alertType": "CPU_HIGH", "message": "Test thủ công"}'

# Gọi lại lần 2 với cùng dữ liệu — kiểm tra KHÔNG tạo trùng
curl -X POST http://localhost:8084/internal/alerts \
  -H "Content-Type: application/json" \
  -d '{"instanceId": '"$INSTANCE_ID"', "alertType": "CPU_HIGH", "message": "Test thủ công lần 2"}'

# Đếm alert chưa resolve
curl "http://localhost:8084/internal/alerts/count-unresolved?instanceIds=$INSTANCE_ID"

# Lấy alert phục vụ tính SLA (loại trừ CPU_HIGH)
curl "http://localhost:8084/internal/alerts/downtime?instanceIds=$INSTANCE_ID"
```
**Kết quả mong đợi lệnh cuối:** Alert `CPU_HIGH` vừa tạo **KHÔNG xuất hiện** trong kết quả `/downtime` — vì endpoint này chủ động loại trừ `CPU_HIGH` (chỉ tính downtime thật sự: `ERROR_DETECTED`, `LONG_STOPPED`).

## 12. Checklist hoàn thành

- [ ] Xem toàn bộ Alert, lưu `$ALERT_ID`
- [ ] Filter `alertType`, `instanceId`, `isResolved`, khoảng ngày đều đúng
- [ ] Resolve Alert thành công, `resolvedAt` được set
- [ ] Resolve lại Alert đã resolve không crash
- [ ] Resolve Alert không tồn tại trả 404
- [ ] Alert dedup hoạt động đúng: cùng loại + chưa resolve → không tạo trùng; đã resolve → cho tạo lại
- [ ] `/internal/alerts/downtime` loại trừ đúng `CPU_HIGH`
