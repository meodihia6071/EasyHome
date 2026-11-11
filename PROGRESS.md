# CURRENT — EasyHome V2 Phase 1 (12/11/2026)

Đã bắt đầu triển khai bản đặc tả V2 được duyệt. Code mới có trong ZIP này, không tự push GitHub.

- DONE IN SOURCE: email OTP, unique phone, identity doc review, upload public/evidence, reviewed-after-payment, mock payment, demo wallet/withdrawal/refund, account locks, plans in DB, UI and versioned additive migrations.
- VERIFIED: Java domain rules 198/198 PASS; Java syntax parse 41 files PASS; JSX syntax parse 4 files PASS.
- PENDING: Maven integration tests, React production build, XAMPP/MariaDB/Flyway startup, end-to-end smoke and security test. See TEST_REPORT.md.
- DO NOT DEPLOY PRODUCTION or replace a working DB before backup and tests. Existing log below is historical and refers to older MVP versions.

---

# PROGRESS — EasyHome

**Ngày ghi nhận:** 04/11/2026  |  **Nhánh gốc:** dự án mới, chưa push  |  **Giai đoạn:** MVP source + QA offline

Nguyên tắc: `DONE` = file đã tạo và quy tắc có thể kiểm tra; `TESTED` = thực tế có lệnh chạy và kết quả; `PENDING` = chưa được kiểm chứng. Không dùng từ "hoàn thành toàn hệ thống" khi chưa có build/test tích hợp.

## Phiên làm việc 05 — Sửa lỗi xác nhận đơn VIP (05/11/2026 00:58, log Windows)

**Bằng chứng:** `mvn clean test` trên Windows đã biên dịch toàn bộ source; Spring Boot / MockMvc kết nối H2 thành công. **5 test chạy: 4 PASS, 1 FAILED, 0 ERROR; Maven BUILD FAILURE**. Test còn lỗi `paidListingRequiresReviewAndPaymentAndReplayIsSafe` tại bước `POST /api/dev/orders/{id}/confirm`, trả HTTP 400 với thông báo `Tin chưa đủ điều kiện mua gói`. Các bước duyệt tin, tạo đơn VIP và giữ tin chưa công khai trước thanh toán đều trả đúng trạng thái theo log.

**Chẩn đoán đối chiếu source:** Sau khi tải `ServiceOrder`, `o.listing` được ánh xạ `LAZY`. `BillingApi.confirmMock` gọi `listings.locked(...)` nhưng vẫn **đọc `l.status` trực tiếp**; đối với Hibernate proxy, đọc field trực tiếp có thể lấy trạng thái mặc định của proxy thay vì giá trị trong database. Code còn ghi trực tiếp `l.status`, `l.plan`, `l.publishedAt`, `l.expiresAt`, có nguy cơ cập nhật sai vào proxy. Đây là nguyên nhân *rất phù hợp* với bằng chứng log, cần xác minh bằng Maven chạy lại (chưa có state dump trong log chứng minh giá trị thực tế).

**Đã thay đổi, giữ nguyên tên file:**

- `BillingApi.java`: thay `Rules.checkout(l.status)` bằng `Rules.checkout(l.getStatus())` ở cả checkout và confirm; kích hoạt tin qua `Listing.activatePackage(...)` thay vì ghi field trực tiếp trong Controller.
- `Listing.java`: thêm một phương thức `activatePackage(plan, priority, paidAt, days)` gọi `Rules.activate` và cập nhật status/plan/publishedAt/expiresAt trên entity Hibernate đã khởi tạo; tiếp tục dùng giá/thời hạn snapshot trong Order.
- `MarketplaceFlowTest.java`: bổ sung assertion kiểm tra trạng thái đã lưu `AWAITING_PAYMENT` ngay trước xác nhận, và `ACTIVE` ngay sau xác nhận để bắt lỗi hồi quy transaction/proxy. **Vẫn giữ 5 test**, không tạo test dư thừa.
- Không thay schema, payment price, các file frontend hay tên file/thư mục.

**Kiểm tra đã chạy ở môi trường sửa:**

| Trạng thái | Test | Kết quả |
|---|---|---|
| TESTED | `bash scripts/test-core.sh` (Java 21, `-Xlint:all -Werror`) | **177/177 PASS**, chạy 3 lượt trong vòng sửa |
| TESTED | JDK 21 JavacTask parse (`--release 21`) trên backend `src` | **20 Java files**, cú pháp PASS |
| PENDING | `mvn clean test` 5 test MockMvc sau sửa | Chờ chạy trên máy Windows / GitHub Actions; không có Maven/dependencies ở môi trường sửa |
| PENDING | `mvn spring-boot:run`, React browser E2E, Docker MySQL | Chưa chạy |

**Bước nghiệm thu tiếp:** giải nén `easyhome.zip` mới vào **thư mục trống** (tránh chạy source cũ), trong `backend` chạy `mvn clean test`. Chỉ khi có `Tests run: 5, Failures: 0, Errors: 0` và `BUILD SUCCESS` mới đánh dấu tích hợp PASS. Nếu vẫn lỗi 400, lấy trạng thái `Listing` qua assertion mới và file `target/surefire-reports` để xác định bước gây lỗi.

## Phiên làm việc 04 — Sửa lỗi integration test từ Maven Windows (05/11/2026)

**Bằng chứng đầu vào:** log `mvn clean test` trên Windows 05/11/2026 00:40. Java compile **19 source PASS**, test compile **1 source PASS**, Spring Boot test context khởi động, H2 in-memory kết nối; tuy nhiên `MarketplaceFlowTest` chạy **3 test: 0 PASS, 2 FAILURE, 1 ERROR**. Không được nhầm bước compile thành Maven BUILD SUCCESS.

**Nguyên nhân xác định từ log và đối chiếu source:** entity `User`, `Property`, `Listing` có quan hệ `@ManyToOne(fetch = LAZY)`, nhưng nhiều Controller truy cập trực tiếp `l.owner.id`, `l.property.city`, `l.property.availability` và các thuộc tính khác. Hibernate proxy không bảo đảm giá trị đúng khi đọc/ghi trực tiếp field; vì vậy quyền sở hữu sinh NullPointerException tại `ListingApi.own`, còn `Rules.content()` nhìn thấy `city == null` khi submit nên trả 400 "Thiếu tỉnh/thành phố". Vấn đề cần sửa đồng bộ, không phải bỏ kiểm tra thành phố hoặc cho phép ghi đè Bean.

**Các thay đổi trong source (không đổi tên file):**

- Bổ sung getter cho các entity `User`, `Property`, `Listing` để truy cập proxy đúng cách.
- Sửa `ListingApi`, `AdminApi`, `BillingApi`, `ExtrasApi` đọc các quan hệ bằng getter, bao gồm DTO, phân quyền, duyệt tin, thanh toán, favorite, contact và report.
- Việc sửa Property khi chỉnh tin dùng `Property.updateDetails(...)` và khi đánh dấu cho thuê dùng `Property.setAvailability(...)`, tránh ghi trực tiếp vào field của Hibernate proxy và không lưu DB.
- Thêm `project.build.sourceEncoding=UTF-8` trong `backend/pom.xml`; đặt UTF-8 tường minh cho các request test có dấu tiếng Việt. Dấu `?` trong bản sao console có thể do encoding hiển thị; nguyên nhân FAIL 400 đã được xác định là truy cập proxy sai. Không tuyên bố đã kiểm chứng encoding trên máy Windows sau sửa.
- Thêm 2 test hồi quy vào `MarketplaceFlowTest`: dữ liệu từ quan hệ LAZY (favorite, detail, contact, inbox) và sửa thông tin Property rồi tải lại. Tổng hiện có **5 bài test MockMvc**, chờ chạy.

**QA đã chạy trong môi trường tạo bản vá:**

| Trạng thái | Kiểm tra | Kết quả |
|---|---|---|
| TESTED | `bash scripts/test-core.sh` (Java 21, `-Xlint:all -Werror`) | PASS **177/177** business rule checks |
| TESTED | JDK Java Compiler parser `--release 21` trên `backend/src` | **20 Java files**, 0 lỗi cú pháp |
| TESTED | Quét source tìm cách đọc field trực tiếp qua các quan hệ LAZY đã biết | Không còn lỗi theo mẫu quét (không thay thế JPA runtime test) |
| PENDING | `mvn clean test` sau sửa, 5 test MockMvc | Cần chạy trên Windows có Maven hoặc CI |
| PENDING | `mvn spring-boot:run`, browser E2E, MySQL Docker | Chưa chạy |

**Vấn đề cần lưu ý:** cảnh báo Java 27/Mockito tự nạp agent trong log không làm 3 test fail; chưa cần chỉnh cấu hình Mockito trước khi sửa hai lỗi nghiệp vụ thực sự. Hãy chạy lại `cd backend` → `mvn clean test`, sau đó khởi động server chỉ nếu `BUILD SUCCESS`.

## Phiên làm việc 03 — Sửa lỗi `MacAlgorithm` không biên dịch được (05/11/2026)

- **Log người dùng:** `mvn clean test` dừng ở `AuthApi.java:46:37` với `cannot find symbol: variable MacAlgorithm`, trước giai đoạn test.
- **Nguyên nhân:** `AuthApi.java` sử dụng `JwsHeader.with(MacAlgorithm.HS256)` nhưng thiếu import `org.springframework.security.oauth2.jose.jws.MacAlgorithm` (đã có trong `SecurityConfig.java`).
- **Sửa:** bổ sung đúng một dòng import trong `backend/src/main/java/com/easyhome/AuthApi.java`. Không đổi tên class, file, logic JWT, `pom.xml` hay cấu trúc project.
- **Đã kiểm tra:** kiểm tra tĩnh import và điểm sử dụng thuật toán HS256 trong source; chạy lại bộ test quy tắc độc lập (`scripts/test-core.sh`).
- **Chưa kiểm chứng:** `mvn clean test` và `mvn spring-boot:run` trong môi trường tạo bản vá (không có Maven); cần người dùng chạy lại và cung cấp log nếu lỗi kế tiếp xuất hiện.

**Lệnh chạy tại `easyhome/backend`:** `mvn clean test` rồi `mvn spring-boot:run`. Chỉ đánh dấu TESTED sau khi có log test Maven thành công.

## Phiên làm việc 02 — Sửa lỗi khởi động Spring Boot (05/11/2026)

**Bản đóng gói cập nhật:** giữ tên `easyhome.zip` và giữ nguyên file `Seed.java` (cả tên lớp `Seed`); chỉ thay tên method `@Bean` bên trong. Không ghi đè database cũ. Lần kiểm tra này chạy lại `scripts/test-core.sh`: PASS 177/177; kiểm tra xung đột tên Bean: PASS. Chưa xác nhận `mvn spring-boot:run` trên môi trường này.

| Trạng thái | Hạng mục | Kết quả |
|---|---|---|
| FIXED | Trùng tên Bean `seed` giữa `@Configuration Seed` và `@Bean seed(...)` | Đổi method thành `seedDemoData(...)`, giữ nguyên nghiệp vụ seed |
| TESTED | Kiểm tra source Bean không còn trùng tên | PASS (kiểm tra cấu trúc code) |
| TESTED | `bash scripts/test-core.sh` | PASS – bộ kiểm thử business rules độc lập, không thay thế test Spring |
| PENDING | `mvn clean test` và `mvn spring-boot:run` | Môi trường tạo bản sửa không có Maven; cần chạy tại máy Windows hoặc CI |

**Lỗi gốc:** `APPLICATION FAILED TO START: The bean 'seed' ... could not be registered`. Không bật `allow-bean-definition-overriding` để lách lỗi. Bản vá chỉ đổi tên Bean, không xóa/khởi tạo lại database và không đổi nghiệp vụ.

**Lệnh kiểm tra trên Windows:** mở PowerShell trong `easyhome/backend`, chạy `mvn clean test`, sau đó `mvn spring-boot:run`. Nếu chưa chạy được, gửi log lỗi tiếp theo và giữ nguyên `PROGRESS.md` ở trạng thái PENDING.

## Phiên làm việc 01 — Khởi tạo lại từ đầu

| Trạng thái | Hạng mục | Ghi chú |
|---|---|---|
| DONE | Repository độc lập, thư mục nông (`backend`, `frontend`, `scripts`) | Không chỉnh repository cũ |
| DONE | Spring Boot 3.5.16 + Java 21 + H2 local / MySQL Docker | Cổng API 8081; Vite/Nginx proxy nhất quán |
| DONE | React 19 + React Router 7 + Vite 7, UI responsive | Các trang Home, Detail, Login, Register, My Listings, Editor, Checkout, Orders, Saved, Inbox, Moderation |
| DONE | Mô hình User, Property, Listing, ServiceOrder, Favorite, Lead, Report, Audit | Một tài khoản vừa tìm vừa đăng nhà |
| DONE | Luồng kiểm duyệt, trạng thái tin, gia hạn bằng đăng lại nháp | Không có "đặt thuê" hay giữ tiền cọc |
| DONE | Gói FREE/STANDARD/VIP, mock confirmation, snapshot giá/gói | Mock chỉ dành cho local/sandbox |
| DONE | API phân quyền owner/staff/admin | Các thao tác ghi đều kiểm tra tài khoản từ JWT/DB |
| DONE | Kịch bản integration test MockMvc và GitHub Actions CI | **Chưa chạy Maven/Actions ở môi trường này** |
| TESTED | 177 kiểm tra quy tắc nghiệp vụ Java 21 | `bash scripts/test-core.sh` PASS, `-Xlint:all -Werror` |
| TESTED | Phân tích cú pháp 5 file frontend (TypeScript parser) | PASS, **không đồng nghĩa Vite build** |
| PENDING | Maven compile + JUnit/MockMvc | Chưa có Maven và không tải được dependencies |
| PENDING | `npm install && npm run build` | Không tải được npm packages; cần CI/user local |
| PENDING | Docker Compose/MySQL, login thực tế, browser E2E | Chưa thực thi trên máy này |

## Những sửa đổi đã thực hiện trong vòng rà soát

1. **Cổng ứng dụng:** toàn bộ backend, Vite, Nginx, Docker và README thống nhất `8081` (tránh Jenkins có thể dùng 8080).
2. **Tách trạng thái:** `ListingStatus` độc lập với `Availability`; tin đóng hoặc nhà đã thuê không tự mở lại.
3. **Chặn xuất bản lách kiểm duyệt:** tạo tin luôn là `DRAFT`, sau đó staff duyệt mới được mua gói.
4. **Thanh toán lặp:** đơn có khóa `PESSIMISTIC_WRITE`; lặp xác nhận cùng đơn chỉ trả về PAID, không tạo thời hạn mới.
5. **Ưu đãi miễn phí:** khóa tài khoản khi tạo đơn FREE để tránh dùng song song ở nhiều tin; mỗi tài khoản tối đa một FREE đã thanh toán.
6. **Giá gói cố định theo đơn:** lưu `priceSnapshot`, `durationDaysSnapshot`, `prioritySnapshot`; thay đổi giá bảng gói không ảnh hưởng đơn đã tạo.
7. **Thời hạn:** chỉ bắt đầu tại `paidAt`; tin tạm ẩn không dừng đồng hồ; truy vấn công khai vẫn lọc `expiresAt > now` ngay cả khi job quá hạn chưa chạy.
8. **Phân quyền:** người ngoài không được sửa/duyệt/đóng tin; người dùng đăng ký không thể tự chọn vai trò Admin.
9. **Dữ liệu đầu vào:** kiểm tra tiêu đề, mô tả, giá, diện tích, loại nhà, ảnh URL tại domain, không tin tưởng chỉ validation frontend.
10. **Giảm spam:** chặn gửi liên hệ trùng 24h và báo cáo OPEN trùng trên cùng tin.

## Vấn đề tồn tại / điểm cần cải thiện (có thứ tự)

| Ưu tiên | Hạng mục | Vì sao cần | Điều kiện nghiệm thu |
|---|---|---|---|
| P0 | Chạy CI Maven + Vite và sửa mọi lỗi thực tế | Hiện chỉ mới kiểm tra Rules Java và cú pháp JSX | `mvn test`, `npm run build` PASS trên GitHub Actions |
| P0 | Chạy luồng API & DB thật trên Windows hoặc Docker | Chưa kiểm tra JPA mapping, serialization, reverse proxy và concurrency | Kịch bản README bước 1–10 hoàn thành, log/screenshot bằng chứng |
| P0 | Tích hợp payment gateway sandbox rồi payment production | Mock không phải bằng chứng thanh toán và có thể bị lạm dụng nếu bật public | Webhook xác thực chữ ký, idempotent, đối soát và hoàn tiền |
| P0 | Tắt demo & mock khi deploy | Tránh tài khoản demo, secret yếu và kích hoạt dịch vụ miễn phí | `APP_SEED_DEMO=false`, `APP_MOCK_PAYMENTS=false`, JWT secret an toàn |
| P1 | Migrations Flyway + backup + kiểm thử dữ liệu | `ddl-auto=update` không bảo đảm migration production | Migration reproducible, backup/restore thử nghiệm |
| P1 | Giới hạn tần suất API, chống brute-force | Chưa có captcha, OTP và rate-limiting | Kịch bản spam/login flood bị chặn có log |
| P1 | Lưu phiên bản tin và lịch sử giá thuê | Sửa thông tin Property khi đăng lại sẽ thay đổi dữ liệu lịch sử | Snapshot giá và phiên bản được duyệt bất biến |
| P1 | Phát hiện đăng tin trùng và xác minh chủ đăng | Chống tin rác và giả mạo | Rule trùng và quy trình chứng minh quyền đăng |
| P1 | Upload ảnh an toàn / lưu trữ object | URL ảnh ngoài có thể chết hoặc theo dõi người dùng | Upload, validate, resize, lưu CDN, quyền sở hữu |
| P2 | SEO động, sitemap, analytics, bộ lọc bản đồ | Tăng khả năng tiếp cận khách | Kiểm tra hiệu suất + SEO và thống kê chuẩn |
| P2 | Thông báo email/OTP và reset mật khẩu | Khả dụng thương mại | Gửi nhận thành công và quy trình bảo mật |

## Quyết định kinh doanh đang dùng (cần xác nhận trước khi thương mại hóa)

- Gói thử nghiệm: `FREE` 7 ngày một lần/tài khoản, `STANDARD` 29.000đ/15 ngày, `VIP` 99.000đ/15 ngày.
- Tin được **kiểm duyệt trước** rồi mới chọn gói và thanh toán.
- Người xem và chủ nhà **liên hệ trực tiếp**; EasyHome không giữ tiền thuê/cọc và không thu hoa hồng giao dịch.
- Một tài khoản đăng nhập dùng chung cho người tìm thuê và người đăng; phân biệt loại hồ sơ người đăng `OWNER/BROKER/AGENCY`.
- Tiếp nhận yêu cầu liên hệ qua nền tảng, không phải yêu cầu đặt thuê; gửi trùng bị chặn trong 24 giờ.
- Bản MVP chưa tự động xác minh danh tính; Admin có thể đánh dấu xác minh thủ công. Không coi "verified" là bảo đảm pháp lý.

## Vòng lặp tiếp theo (sau khi đẩy repo mới)

1. Xem tab GitHub Actions, sửa lỗi Java/Maven/React đến khi cả hai job xanh.
2. Trên máy có Java/Maven/Node: chạy local và kiểm thử thủ công 10 bước theo README; ghi kết quả pass/fail vào `TEST_REPORT.md`.
3. Chạy Docker với MySQL sạch; kiểm tra dữ liệu giữ lại sau restart, rollback khi giao dịch lỗi, callback lặp, sai quyền.
4. Chỉ khi đã ổn định mới bổ sung payment sandbox, migrations và upload ảnh. Không tự ý đưa thêm tính năng ngoài quy trình chính.

> **Lưu ý:** PROGRESS này ghi lại công việc thực tế của phiên tạo source. Những mục PENDING không được tự đổi thành DONE nếu chưa có bằng chứng chạy test.

## 12/11/2026 — Bàn giao source phương án A từ meodihia6071/EasyHome/main

- Đã đối chiếu blob SHA các file nghiệp vụ Java và React với nhánh `main`; giữ nguyên mã nguồn nghiệp vụ, giao diện và các endpoint.
- Chuyển database mặc định khi chạy local từ H2 file sang MariaDB XAMPP; giữ MySQL Docker; backend và Vite vốn đã dùng cổng `8081`.
- Bổ sung MariaDB JDBC Driver, `database.sql`, tài liệu chạy XAMPP UTF-8 và cấu hình test H2 riêng.
- Không thực hiện thay đổi business logic, schema entity, giá gói, cấu trúc Java/React.
- Giới hạn bàn giao: môi trường đóng gói không truy cập được npm/GitHub archive trực tiếp; `frontend/package-lock.json` cần giữ từ checkout hiện có hoặc tạo lại bằng `npm install` (nếu thiếu).
- Kiểm thử có thể thực hiện tại môi trường đóng gói được ghi ở `TEST_REPORT.md`; không tự đánh dấu Maven, frontend build hoặc MariaDB thật là PASS.


## 12/11/2026 – Tách lớp và thư mục source
- Xem `ARCHITECTURE.md`. Chuyển code từ package phẳng thành cấu trúc Spring Boot theo lớp, và tách React pages theo chức năng.
- Giữ tên file hiện có và các URL API; chỉ di chuyển file, tách class cùng file và cập nhật import/visibility cần thiết.
- Đã kiểm tra cú pháp và chạy 198 domain checks. Maven/npm/XAMPP runtime còn chờ nghiệm thu.
