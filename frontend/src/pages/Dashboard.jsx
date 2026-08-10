import { useEffect, useState } from "react";
import apiClient from "../api/client";

const statCards = [
  { key: "totalInstances", label: "Tổng instance", color: "var(--tv-text)" },
  { key: "runningInstances", label: "RUNNING", color: "var(--tv-running)" },
  { key: "stoppedInstances", label: "STOPPED", color: "var(--tv-stopped)" },
  { key: "errorInstances", label: "ERROR", color: "var(--tv-error)" },
  { key: "unresolvedAlerts", label: "Alert chưa xử lý", color: "var(--tv-error)" },
  { key: "totalClients", label: "Khách hàng", color: "var(--tv-accent)" },
];

export default function Dashboard() {
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get("/monitor/report") // -> GET /api/monitor/report qua nginx
      .then((res) => {
        if (!cancelled) setReport(res.data);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || "Không tải được báo cáo tổng quan.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <h1 className="text-xl font-semibold mb-1" style={{ color: "var(--tv-text)" }}>
        Tổng quan hạ tầng
      </h1>
      <p className="text-sm mb-6" style={{ color: "var(--tv-text-muted)" }}>
        Dữ liệu tổng hợp từ instance-service, alert-service, client-service qua monitoring-service.
      </p>

      {loading && <p style={{ color: "var(--tv-text-muted)" }}>Đang tải...</p>}

      {error && (
        <p
          className="rounded-lg px-3 py-2 text-sm"
          style={{ background: "rgba(248,113,113,0.1)", color: "var(--tv-error)" }}
        >
          {error}
        </p>
      )}

      {report && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          {statCards.map((c) => (
            <div
              key={c.key}
              className="rounded-xl border p-5"
              style={{ borderColor: "var(--tv-border)", background: "var(--tv-surface)" }}
            >
              <p className="text-sm mb-1" style={{ color: "var(--tv-text-muted)" }}>
                {c.label}
              </p>
              <p className="text-3xl font-semibold tv-mono" style={{ color: c.color }}>
                {report[c.key]}
              </p>
            </div>
          ))}

          <div
            className="rounded-xl border p-5 col-span-2 sm:col-span-3"
            style={{ borderColor: "var(--tv-border)", background: "var(--tv-surface)" }}
          >
            <p className="text-sm mb-1" style={{ color: "var(--tv-text-muted)" }}>
              CPU trung bình
            </p>
            <p className="text-3xl font-semibold tv-mono" style={{ color: "var(--tv-text)" }}>
              {report.averageCpuUsage}%
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
