# KẾ HOẠCH CẢI TIẾN HỆ THỐNG LẬP LỊCH (SCHEDULING SYSTEM IMPROVEMENT PLAN)
**Dự án**: Cloud Instance Monitoring System - `monitoring-service`  
**Ngày cập nhật**: 09/08/2026  

---

## 📌 1. TỔNG QUAN VÀ TRẠNG THÁI HIỆN TẠI (CURRENT STATE)

### 🔹 Trạng Thái Ngắn Hạn (Hiện Tại - Option 1)
Hiện tại, `monitoring-service` đang áp dụng **Phương án 1: Native Spring `@Scheduled` + `@ConditionalOnProperty`**.

* **Mã nguồn áp dụng**:
  * [MonitoringScheduler.java](file:///d:/OTJprj_TechValley/monitoring-service/src/main/java/com/techvalley/monitor/monitoring/scheduler/MonitoringScheduler.java): Sử dụng `@Scheduled(cron = "0 */5 * * * *")` kết hợp `@ConditionalOnProperty(name = "monitoring.scheduler.enabled", havingValue = "true", matchIfMissing = true)`.
  * [SchedulerConfig.java](file:///d:/OTJprj_TechValley/monitoring-service/src/main/java/com/techvalley/monitor/config/SchedulerConfig.java): Bật tính năng lập lịch bằng `@EnableScheduling`.

  
### 🔹 Lý Do Cần Cải Tiến Hệ Thống (Rationale for System Improvement)
Hệ thống lập lịch hiện tại trong `monitoring-service` đóng vai trò là động cơ giám sát 24/7 tự động phát hiện máy chủ quá tải CPU, gặp sự cố lỗi hoặc bị tắt quá 48 giờ để phát sinh cảnh báo kịp thời; tuy nhiên, khi dự án mở rộng quy mô chạy đa instance (scale-out trên nhiều container/pod song song), giải pháp đơn node hiện tại sẽ gặp giới hạn về chịu tải và nguy cơ chạy trùng lặp tiến trình (race condition), do đó việc chuẩn bị sẵn kế hoạch cải tiến sang các phương án như REST External Trigger hay Redis Distributed Lock là vô cùng cần thiết nhằm đảm bảo tính sẵn sàng cao (High Availability), tối ưu hiệu năng In-Memory và ngăn chặn hoàn toàn việc phát sinh cảnh báo trùng lặp trong môi trường phân tán.

---

## 🚀 2. KẾ HOẠCH CẢI TIẾN TRONG TƯƠNG LAI (FUTURE IMPROVEMENT ROADMAP)

Khi hệ thống mở rộng quy mô (Scale-out) chạy nhiều container/replica song song (High Availability Cluster), dự án sẽ cân nhắc nâng cấp sang một trong hai phương án kiến trúc dưới đây:

---

### 🔹 PHƯƠNG ÁN 2: CHUYỂN THÀNH REST API ENDPOINT + EXTERNAL TRIGGER (K8S CRONJOB / CLOUD SCHEDULER)

#### 1. Kiến trúc tổng quan (Architecture)
* **Ý tưởng**: Loại bỏ hoàn toàn `@Scheduled` khỏi ứng dụng Java. Chuyển Job thành một Internal REST API Endpoint. Công cụ bên ngoài (Kubernetes CronJob, Linux Crontab, hoặc AWS EventBridge) sẽ đóng vai trò hẹn giờ và gửi HTTP Request kích hoạt Job.
* **Mô hình**:
  ```text
  [ External Scheduler ] (K8s CronJob / CloudWatch)
            │
            │ HTTP POST /api/v1/monitoring/jobs/auto-scan (với Secret Header)
            ▼
  [ API Gateway / Load Balancer ]
            │
            ▼
  [ monitoring-service Pods ] (Stateless execution)
  ```

#### 2. Kế hoạch triển khai mã nguồn (Implementation Steps)
1. **Tạo Controller Endpoint**:
   ```java
   @RestController
   @RequestMapping("/api/v1/monitoring/jobs")
   @RequiredArgsConstructor
   public class MonitoringJobController {

       private final MonitoringService monitoringService;

       @PostMapping("/auto-scan")
       public ResponseEntity<ApiResponse<Void>> triggerAutoScan(
               @RequestHeader("X-Internal-Secret") String internalSecret) {
           // Validate Secret Token bảo mật
           monitoringService.getWarnings();
           monitoringService.getErrors();
           monitoringService.getLongStopped();
           return ResponseEntity.ok(ApiResponse.success("Job triggered successfully"));
       }
   }
   ```
2. **Cấu hình Kubernetes CronJob YAML (`monitoring-cronjob.yaml`)**:
   ```yaml
   apiVersion: batch/v1
   kind: CronJob
   metadata:
     name: monitoring-auto-scan-job
   spec:
     schedule: "*/5 * * * *"
     jobTemplate:
       spec:
         template:
           spec:
             containers:
             - name: curl-job
               image: curlimages/curl:latest
               command: ["curl", "-X", "POST", "http://monitoring-service/api/v1/monitoring/jobs/auto-scan", "-H", "X-Internal-Secret: TOP_SECRET_KEY"]
             restartPolicy: OnFailure
   ```

#### 3. Đánh giá Ưu / Nhược điểm
* **Ưu điểm**: `monitoring-service` trở thành hoàn toàn *Stateless*, dễ dàng kiểm thử thủ công qua API, theo dõi log và thất bại tập trung trên Kubernetes Dashboard.
* **Nhược điểm**: Phụ thuộc vào hạ tầng DevOps bên ngoài.

---

### 🔹 PHƯƠNG ÁN 3: KHOÁ PHÂN TÁN BẰNG REDIS (REDIS ATOMIC DISTRIBUTED LOCK)

#### 1. Kiến trúc tổng quan (Architecture)
* **Ý tưởng**: Khi ứng dụng scale-out lên $N$ instance container song song, cả $N$ instance đều chạy timer `@Scheduled`. Tuy nhiên, ngay trước khi thực thi công việc, từng instance phải tranh chấp cấp khóa (Lock) trong **Redis In-Memory Database** bằng lệnh nguyên tử `SET lock_key value NX EX duration`.
* **Mô hình**:
  ```text
  [ Pod 1 ] ──(SETNX lock)──► [ REDIS SERVER ] ◄──(SETNX lock)── [ Pod 2 ]
     │                              │                                │
  (Thành công: SUCCESS)      (Chỉ 1 key duy nhất)           (Thất bại: NULL)
     │                                                               │
     ▼                                                               ▼
  [ Thực thi Auto-Scan Job ]                               [ Bỏ qua / Skip Job ]
  ```

#### 2. Kế hoạch triển khai mã nguồn (Implementation Steps)
1. **Thêm dependency Redis vào `pom.xml`**:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```
2. **Cập nhật `MonitoringScheduler.java`**:
   ```java
   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class MonitoringScheduler {

       private final MonitoringService monitoringService;
       private final StringRedisTemplate redisTemplate;

       private static final String LOCK_KEY = "lock:monitoring:auto-scan";

       @Scheduled(cron = "0 */5 * * * *")
       public void runAutoScan() {
           String lockValue = UUID.randomUUID().toString();
           
           // Thử lấy khóa trong Redis với TTL 4 phút (Ngăn Race Condition)
           Boolean acquired = redisTemplate.opsForValue()
                   .setIfAbsent(LOCK_KEY, lockValue, Duration.ofMinutes(4));

           if (Boolean.TRUE.equals(acquired)) {
               try {
                   log.info("⏰ [Node Master] Bắt đầu chạy Cron Job quét tự động...");
                   monitoringService.getWarnings();
                   monitoringService.getErrors();
                   monitoringService.getLongStopped();
                   log.info("✅ Hoàn tất Cron Job quét hạ tầng.");
               } finally {
                   if (lockValue.equals(redisTemplate.opsForValue().get(LOCK_KEY))) {
                       redisTemplate.delete(LOCK_KEY);
                   }
               }
           } else {
               log.info("⏭️ Tiến trình khác đang thực thi Job. Bỏ qua để tránh trùng lặp.");
           }
       }
   }
   ```

#### 3. Đánh giá Ưu / Nhược điểm
* **Ưu điểm**: Chịu tải cực cao (Redis xử lý In-Memory < 1ms), chống trùng lặp tiến trình (Race Condition) tuyệt đối, hỗ trợ tự động chuyển vùng (Failover) nếu 1 pod sập.
* **Nhược điểm**: Yêu cầu duy trì hạ tầng Redis Cluster.

---

## 📊 3. TIÊU CHÍ KÍCH HOẠT CHUYỂN ĐỔI (MIGRATION TRIGGER CRITERIA)

| Giai đoạn | Phương án áp dụng | Điều kiện kích hoạt nâng cấp |
| :--- | :--- | :--- |
| **Giai đoạn 1 (Hiện tại)** | **Phương án 1** (Native `@Scheduled`) | Đang chạy 1 Instance `monitoring-service` cho môi trường Development & Testing. |
| **Giai đoạn 2 (Scale-Out Single Cluster)** | **Phương án 3** (Redis Distributed Lock) | Khi dự án tích hợp Redis Caching và nâng số lượng Pod/Replica của `monitoring-service` lên $\ge 2$. |
| **Giai đoạn 3 (Enterprise Cloud Native)** | **Phương án 2** (K8s CronJob / API Trigger) | Khi toàn bộ hạ tầng chuyển sang Kubernetes (K8s) hoặc Serverless Cloud Architecture. |
