// Mỗi href là RELATIVE PATH, khớp đúng với nginx/nginx.conf:
//   /docs/instance/ -> instance-service:8081/swagger-ui/
//   /docs/client/   -> client-service:8082/swagger-ui/
//   /docs/monitor/  -> monitoring-service:8083/swagger-ui/
//   /docs/alert/    -> alert-service:8084/swagger-ui/
// Không nhúng iframe (đã chốt hướng 1) — mở tab mới để demo Swagger UI thật,
// dùng đúng lúc thuyết trình slide #9 "Swagger Demo".
const docs = [
  {
    key: "instance",
    title: "Instance Service",
    desc: "Đăng ký / truy vấn / xoá instance, cập nhật trạng thái.",
    href: "/docs/instance/",
  },
  {
    key: "client",
    title: "Client Service",
    desc: "Khách hàng, chi phí, dự báo chi phí, SLA uptime.",
    href: "/docs/client/",
  },
  {
    key: "monitor",
    title: "Monitoring Service",
    desc: "Cảnh báo CPU cao, lỗi hệ thống, máy chủ dừng lâu, báo cáo tổng quan.",
    href: "/docs/monitor/",
  },
  {
    key: "alert",
    title: "Alert Service",
    desc: "Lịch sử alert, đánh dấu đã xử lý.",
    href: "/docs/alert/",
  },
];

export default function ApiDocs() {
  return (
    <div>
      <h1 className="text-xl font-semibold mb-1" style={{ color: "var(--tv-text)" }}>
        API Docs
      </h1>
      <p className="text-sm mb-6" style={{ color: "var(--tv-text-muted)" }}>
        Swagger UI của từng microservice, phục vụ demo trực tiếp khi thuyết trình.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {docs.map((d) => (
          <a
            key={d.key}
            href={d.href}
            target="_blank"
            rel="noreferrer"
            className="block rounded-xl border p-5 transition-colors hover:border-[var(--tv-accent)]"
            style={{ borderColor: "var(--tv-border)", background: "var(--tv-surface)" }}
          >
            <div className="flex items-center justify-between mb-2">
              <span className="font-medium" style={{ color: "var(--tv-text)" }}>
                {d.title}
              </span>
              <span className="text-xs tv-mono" style={{ color: "var(--tv-accent)" }}>
                {d.href}
              </span>
            </div>
            <p className="text-sm" style={{ color: "var(--tv-text-muted)" }}>
              {d.desc}
            </p>
          </a>
        ))}
      </div>
    </div>
  );
}
