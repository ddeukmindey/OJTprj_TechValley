# Test chi tiết — client-service (port 8082)

Yêu cầu trước: đã có `$TOKEN` (ADMIN) và `$TOKEN_MANAGER` (CLIENT_MANAGER) từ file `01_TEST_auth-gateway-service.md`.

## 1. Danh sách endpoint thực tế trong code

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/clients` | ADMIN only | Tạo khách hàng mới |
| GET | `/api/clients` | ADMIN/CLIENT_MANAGER | Danh sách (phân trang, search) — ADMIN thấy tất cả, MANAGER chỉ thấy của mình |
| GET | `/api/clients/{id}/instances` | Có kiểm tra quyền sở hữu | Danh sách Instance của client (gọi sang `instance-service` qua WebClient) |
| GET | `/api/clients/{id}/cost` | Có kiểm tra quyền sở hữu | Tổng chi phí tháng hiện tại |
| GET | `/api/clients/{id}/cost-forecast` | Có kiểm tra quyền sở hữu | Dự báo chi phí cuối tháng |
| GET | `/api/clients/{id}/sla` | Có kiểm tra quyền sở hữu | Tính SLA uptime (gọi sang `alert-service` qua WebClient) |

## 2. Test Case 1 — Tạo Client mới (ADMIN)

```bash
curl -X POST http://localhost:8082/api/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Cong ty ABC",
    "contractPlan": "PREMIUM",
    "managerId": 1,
    "email": "contact@abc.com",
    "company": "ABC Corp"
  }'
```
**Kết quả mong đợi:** HTTP 201, trả về object Client có `id`. **Lưu lại `$CLIENT_ID`**.

`managerId` phải là `id` thật của 1 member trong bảng `members` (dùng `id` của tài khoản CLIENT_MANAGER đã tạo ở bước trước, không phải email).

## 3. Test Case 2 — Tạo Client bằng tài khoản CLIENT_MANAGER (phải bị từ chối)

```bash
curl -X POST http://localhost:8082/api/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_MANAGER" \
  -d '{"name": "Test", "contractPlan": "BASIC", "managerId": 2}'
```
**Kết quả mong đợi:** HTTP 403 — vì `validateAdminRole()` chặn CLIENT_MANAGER tạo Client. Đây là test quan trọng nhất cho tiêu chí chấm điểm "JWT Auth — role-based access control".

## 4. Test Case 3 — Tạo Client với `contractPlan` không hợp lệ

```bash
curl -X POST http://localhost:8082/api/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name": "Test Invalid Plan", "contractPlan": "GOLD", "managerId": 1}'
```
**Kết quả mong đợi:** HTTP 400, message `"Gói hợp đồng không hợp lệ. Chỉ chấp nhận: BASIC, STANDARD, PREMIUM"`.

## 5. Test Case 4 — Lấy danh sách Client (phân trang, ADMIN thấy tất cả)

```bash
curl "http://localhost:8082/api/clients?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, `data.items` chứa client vừa tạo, `data.pagination` có đủ `currentPage/pageSize/totalElements/totalPages`.

## 6. Test Case 5 — CLIENT_MANAGER chỉ thấy Client được gán cho mình

```bash
curl "http://localhost:8082/api/clients?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN_MANAGER"
```
**Kết quả mong đợi:** Chỉ trả về Client có `managerId` = id của tài khoản manager đang đăng nhập. Đây là điểm chấm quan trọng — nếu trả về TẤT CẢ client là sai logic phân quyền.

## 7. Test Case 6 — Search theo tên

```bash
curl "http://localhost:8082/api/clients?page=1&size=10&search=ABC" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** Chỉ trả Client có tên chứa "ABC" (không phân biệt hoa thường).

## 8. Test Case 7 — Lấy danh sách Instance của Client (⚠️ endpoint gọi cross-service)

*(Test case này chỉ có ý nghĩa SAU KHI đã tạo Instance ở file `03_TEST_instance-service.md` — nếu chưa có Instance nào, kết quả sẽ rỗng, không phải lỗi.)*

```bash
curl "http://localhost:8082/api/clients/$CLIENT_ID/instances" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, danh sách Instance thuộc client này. **Đây là endpoint quan trọng nhất để verify kiến trúc mới hoạt động đúng** — `client-service` phải gọi API `GET /internal/instances?clientIds=...` sang `instance-service` qua WebClient (không còn JPQL trực tiếp như code cũ). Nếu lỗi 500 ở đây, khả năng cao `instance-service` chưa chạy hoặc `INSTANCE_SERVICE_URL` cấu hình sai trong `docker-compose.yml`.

**Cách xác nhận đúng là đang gọi qua API (không phải DB chung):**
```bash
docker logs instance-service --tail 20
```
Nếu thấy log ghi nhận có request `GET /internal/instances` mới xuất hiện đúng lúc bạn gọi API trên — xác nhận kiến trúc mới hoạt động đúng.

## 9. Test Case 8 — Tính chi phí (`/cost`)

```bash
curl "http://localhost:8082/api/clients/$CLIENT_ID/cost" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** `runningCost + stoppedCost = totalMonthlyCost`, số liệu khớp với `monthlyCost` của các Instance đã tạo cho client này.

## 10. Test Case 9 — Dự báo chi phí (`/cost-forecast`)

```bash
curl "http://localhost:8082/api/clients/$CLIENT_ID/cost-forecast" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** `activeRunningInstances` khớp số Instance đang `RUNNING`, `projectedSpentEndOfMonth >= currentSpent`.

## 11. Test Case 10 — Tính SLA (⚠️ endpoint gọi cross-service quan trọng nhất)

```bash
curl "http://localhost:8082/api/clients/$CLIENT_ID/sla" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 200, `slaPercentage` từ 0-100, `targetSla` khớp với `contractPlan` (PREMIUM=99.9, STANDARD=99, BASIC=95), `status` là `NORMAL` hoặc `VIOLATION`.

Đây là endpoint gọi `GET /internal/alerts/downtime` sang `alert-service`. Test 2 trường hợp:
- **Client chưa có Instance nào bị lỗi/downtime** → `slaPercentage = 100.0`, `status = NORMAL`
- **Client có Instance từng bị Alert `ERROR_DETECTED` hoặc `LONG_STOPPED`** (test sau khi chạy `monitoring-service` ở file 04) → `slaPercentage < 100`, kiểm tra tính đúng bằng tay: tổng thời gian downtime / tổng thời gian đã chạy trong tháng

## 12. Test Case 11 — Truy cập Client không thuộc quyền quản lý (CLIENT_MANAGER)

Dùng `$TOKEN_MANAGER` của 1 manager KHÔNG phải người quản lý `$CLIENT_ID`:
```bash
curl "http://localhost:8082/api/clients/$CLIENT_ID/cost" \
  -H "Authorization: Bearer $TOKEN_MANAGER_KHAC"
```
**Kết quả mong đợi:** HTTP 403 — do `getClientAndValidateAccess()` kiểm tra `user.getMemberId().equals(client.getManagerId())`.

## 13. Test Case 12 — Client ID không tồn tại

```bash
curl "http://localhost:8082/api/clients/99999/cost" \
  -H "Authorization: Bearer $TOKEN"
```
**Kết quả mong đợi:** HTTP 404, message `"Không tìm thấy khách hàng với ID: 99999"`.

## 14. Checklist hoàn thành

- [ ] Tạo Client thành công (ADMIN), lưu `$CLIENT_ID`
- [ ] CLIENT_MANAGER bị chặn khi tạo Client (403)
- [ ] Validate `contractPlan` sai bị từ chối (400)
- [ ] ADMIN xem được tất cả Client, CLIENT_MANAGER chỉ xem được của mình
- [ ] `/instances` trả đúng danh sách, xác nhận gọi qua API sang `instance-service` (không lỗi 500)
- [ ] `/cost`, `/cost-forecast` tính đúng
- [ ] `/sla` trả đúng, xác nhận gọi qua API sang `alert-service`
- [ ] Truy cập Client không thuộc quyền bị chặn 403
- [ ] Client không tồn tại trả 404
