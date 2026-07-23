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
