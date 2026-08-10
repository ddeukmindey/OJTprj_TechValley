import axios from "axios";

/**
 * DUY NHẤT một chỗ định nghĩa baseURL trong toàn bộ frontend.
 *
 * "/api" là RELATIVE PATH — không có domain, không có IP, không có port.
 * Trình duyệt tự ghép vào origin hiện tại (protocol + host + port của
 * chính trang web đang mở). nginx (xem nginx/nginx.conf) nhận request tới
 * "/api/..." và route sang đúng microservice phía sau.
 *
 * => Khi bàn giao, dù chạy ở http://localhost, http://10.0.0.5 hay
 *    https://techvalley.example.com, KHÔNG CẦN SỬA FILE NÀY, không cần
 *    biến môi trường VITE_API_URL nào cả.
 */
const apiClient = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Gắn JWT access token vào mọi request (trừ khi không có token, ví dụ trước khi login)
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Chuẩn hoá response: backend bọc data trong { status, message, data }
// (theo đúng "unified response format" mà đề bài yêu cầu)
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn / không hợp lệ -> buộc đăng nhập lại
      localStorage.removeItem("accessToken");
      localStorage.removeItem("role");
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
