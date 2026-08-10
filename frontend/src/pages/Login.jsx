import { useState } from "react";
import { useNavigate, useLocation, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={location.state?.from?.pathname || "/"} replace />;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login(email, password);
      navigate(location.state?.from?.pathname || "/", { replace: true });
    } catch (err) {
      // Backend trả { success:false, message, data:null } khi sai
      const msg =
        err.response?.data?.message ||
        "Đăng nhập thất bại. Kiểm tra lại email và mật khẩu.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ background: "var(--tv-bg)" }}
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-2xl border p-8"
        style={{ borderColor: "var(--tv-border)", background: "var(--tv-surface)" }}
      >
        <p
          className="text-xs tracking-widest uppercase mb-1"
          style={{ color: "var(--tv-accent)" }}
        >
          TechValley
        </p>
        <h1 className="text-2xl font-semibold mb-6" style={{ color: "var(--tv-text)" }}>
          Cloud Monitor
        </h1>

        <label className="block text-sm mb-1.5" style={{ color: "var(--tv-text-muted)" }}>
          Email
        </label>
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full mb-4 rounded-lg border px-3 py-2 text-sm outline-none focus:ring-2"
          style={{
            borderColor: "var(--tv-border)",
            background: "var(--tv-surface-2)",
            color: "var(--tv-text)",
          }}
          placeholder="admin@techvalley.com"
        />

        <label className="block text-sm mb-1.5" style={{ color: "var(--tv-text-muted)" }}>
          Mật khẩu
        </label>
        <input
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full mb-5 rounded-lg border px-3 py-2 text-sm outline-none focus:ring-2"
          style={{
            borderColor: "var(--tv-border)",
            background: "var(--tv-surface-2)",
            color: "var(--tv-text)",
          }}
          placeholder="••••••••"
        />

        {error && (
          <p
            className="mb-4 rounded-lg px-3 py-2 text-sm"
            style={{ background: "rgba(248,113,113,0.1)", color: "var(--tv-error)" }}
          >
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg py-2.5 text-sm font-semibold transition-opacity disabled:opacity-60"
          style={{ background: "var(--tv-accent)", color: "#0e1116" }}
        >
          {loading ? "Đang đăng nhập..." : "Đăng nhập"}
        </button>
      </form>
    </div>
  );
}
