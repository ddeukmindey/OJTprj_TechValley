import { createContext, useContext, useState, useCallback } from "react";
import apiClient from "../api/client";

const AuthContext = createContext(null);

// Field name phải khớp CHÍNH XÁC với LoginResponse.java (auth-gateway-service):
// { memberId, name, email, role, accessToken }
function readStoredUser() {
  const raw = localStorage.getItem("user");
  return raw ? JSON.parse(raw) : null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const login = useCallback(async (email, password) => {
    // POST /api/auth/login -> ApiResponse<LoginResponse>
    const res = await apiClient.post("/auth/login", { email, password });
    const data = res.data; // { memberId, name, email, role, accessToken }

    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem(
      "user",
      JSON.stringify({
        memberId: data.memberId,
        name: data.name,
        email: data.email,
        role: data.role,
      })
    );
    setUser({
      memberId: data.memberId,
      name: data.name,
      email: data.email,
      role: data.role,
    });
    return data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
    setUser(null);
  }, []);

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin: user?.role === "ADMIN",
    isClientManager: user?.role === "CLIENT_MANAGER",
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth() phải được gọi bên trong <AuthProvider>");
  return ctx;
}
