import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Bọc quanh route cần đăng nhập. Nếu truyền allowedRoles, chỉ những role đó
 * mới vào được (ví dụ trang quản lý client chỉ ADMIN được thấy nút "Thêm mới").
 *
 * Lưu ý: đây là guard PHÍA GIAO DIỆN (UX) — ẩn/chặn điều hướng cho gọn.
 * Việc kiểm tra quyền THẬT SỰ vẫn nằm ở backend (JwtInterceptor + role check
 * trong Service impl), vì người dùng luôn có thể sửa localStorage/gọi API
 * trực tiếp. Không bao giờ được coi guard này là lớp bảo mật duy nhất.
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
