import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import DashboardLayout from "./layouts/DashboardLayout";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import ApiDocs from "./pages/ApiDocs";

// Các trang Instances / Clients / Alerts sẽ được thêm ở các bước tiếp theo
// (Member B/C/D trong role assignment guide) theo cùng pattern với Dashboard.jsx:
// gọi apiClient.get(...) trong useEffect, render vào layout có sẵn.

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route
            element={
              <ProtectedRoute>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<Dashboard />} />
            <Route path="/docs" element={<ApiDocs />} />
            {/* <Route path="/instances" element={<Instances />} /> */}
            {/* <Route path="/clients" element={<Clients />} /> */}
            {/* <Route path="/alerts" element={<Alerts />} /> */}
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
