import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const navItems = [
  { to: "/", label: "Tổng quan", roles: ["ADMIN", "CLIENT_MANAGER"] },
  { to: "/instances", label: "Instances", roles: ["ADMIN", "CLIENT_MANAGER"] },
  { to: "/clients", label: "Clients", roles: ["ADMIN"] },
  { to: "/alerts", label: "Alerts", roles: ["ADMIN", "CLIENT_MANAGER"] },
  { to: "/docs", label: "API Docs", roles: ["ADMIN", "CLIENT_MANAGER"] },
];

export default function DashboardLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen" style={{ background: "var(--tv-bg)" }}>
      <aside
        className="w-60 shrink-0 flex flex-col border-r"
        style={{ borderColor: "var(--tv-border)", background: "var(--tv-surface)" }}
      >
        <div className="px-5 py-5 border-b" style={{ borderColor: "var(--tv-border)" }}>
          <p className="text-sm tracking-wide uppercase" style={{ color: "var(--tv-text-muted)" }}>
            TechValley
          </p>
          <p className="text-lg font-semibold">Cloud Monitor</p>
        </div>

        <nav className="flex-1 px-2 py-4 space-y-1">
          {navItems
            .filter((item) => item.roles.includes(user?.role))
            .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/"}
                className={({ isActive }) =>
                  `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    isActive ? "text-white" : ""
                  }`
                }
                style={({ isActive }) => ({
                  background: isActive ? "var(--tv-accent-dim)" : "transparent",
                  color: isActive ? "var(--tv-accent)" : "var(--tv-text-muted)",
                })}
              >
                {item.label}
              </NavLink>
            ))}
        </nav>

        <div className="px-4 py-4 border-t text-sm" style={{ borderColor: "var(--tv-border)" }}>
          <p className="font-medium" style={{ color: "var(--tv-text)" }}>
            {user?.name}
          </p>
          <p style={{ color: "var(--tv-text-muted)" }}>{user?.role}</p>
          <button
            onClick={logout}
            className="mt-3 w-full rounded-lg border px-3 py-1.5 text-sm transition-colors hover:text-white"
            style={{ borderColor: "var(--tv-border)", color: "var(--tv-text-muted)" }}
          >
            Đăng xuất
          </button>
        </div>
      </aside>

      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  );
}
