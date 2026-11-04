# TEST REPORT — EasyHome (04/11/2026)

**Môi trường kiểm thử đã sử dụng:** Linux container, OpenJDK 21.0.12, Node 22.16.0, TypeScript parser cài sẵn ở môi trường tạo code. Không có `mvn`, không tải được Maven/npm registry, không có Docker CLI.

## Báo cáo Maven Windows 05/11/2026 00:58 — vòng tích hợp thứ hai

**Bằng chứng:** test compile, Spring context, JPA, H2 và MockMvc khởi tạo thành công. Kết quả **5 tests: 4 PASS / 1 FAILED / 0 ERROR**; Maven **BUILD FAILURE**. Test `paidListingRequiresReviewAndPaymentAndReplayIsSafe` thất bại tại `POST /api/dev/orders/{id}/confirm` vì HTTP 400 thay vì 200; message `Tin chưa đủ điều kiện mua gói`. Tin đã được duyệt `AWAITING_PAYMENT`, tạo đơn `PENDING` thành công. Các test quyền sở hữu và dữ liệu LAZY trước đó không còn báo lỗi theo tổng kết này.

**Bản vá mới:** trong `BillingApi.confirmMock`, dùng `l.getStatus()` thay vì truy cập `l.status`, và cập nhật gói bằng `Listing.activatePackage(...)` (tránh ghi thẳng các field trên một LAZY proxy). Trong bài test đang lỗi, thêm 2 assertion đọc lại trạng thái từ repository trước/sau xác nhận. **Đây là bản sửa cần được chạy Maven lại để xác minh; không ghi 5/5 PASS khi chưa có log.**

**Đã chạy offline sau sửa:** `scripts/test-core.sh` 177/177 PASS (3 lượt); JDK parser 20 file Java PASS. **Chưa chạy Maven, npm build, Docker, browser E2E** ở môi trường sửa source.

## Test Maven Windows 05/11/2026 (bản trước hotfix LAZY) và kết quả sau bản vá

**Kết quả người dùng cung cấp:** `mvn clean test` compile 19 file Java và 1 file test, Spring Boot 3.5.16 mở Spring TestDispatcherServlet, H2 `jdbc:h2:mem:easyhome_tests` kết nối thành công. **MockMvc 3 tests: 0 PASS; 2 FAILED; 1 ERROR; BUILD FAILURE.**

| Test | Kết quả trên source cũ | Chẩn đoán |
|---|---|---|
| `paidListingRequiresReviewAndPaymentAndReplayIsSafe` | FAIL 400 khi submit tin | `l.property.city` null do Hibernate proxy LAZY |
| `freeBenefitLimitedAndRelistAfterClose` | FAIL 400 khi submit tin | Cùng lỗi đọc trực tiếp `l.property.*` |
| `crossAccountOperationsBlockedAndInvalidListingRejected` | ERROR NullPointerException | `l.owner.id.equals(...)` trên owner LAZY proxy |

**Bản vá hiện tại:** đổi truy cập lazy entity sang getters trong các API, bảo đảm sửa dữ liệu Property thông qua phương thức entity; thêm kiểm tra hồi quy `lazyRelationsKeepListingDetailsAndContactData` và `editedDraftPropertyDetailsSurviveReloadFromDatabase`, tổng **5 test MockMvc**. Có thêm encoding UTF-8 rõ ràng. **Chưa có kết quả Maven PASS/FAIL của 5 test mới**; cần chạy lại trên máy Windows. Cảnh báo tự attach Mockito agent ở Java 27 không phải lỗi khiến test thất bại.

**Đã chạy lại tại môi trường sửa source:** `bash scripts/test-core.sh`: **PASS 177/177**; Java Compiler parser cho **20 file `.java`**: **0 lỗi cú pháp**; kiểm tra mẫu truy cập field LAZY trực tiếp không phát hiện phần còn sót trong source API. Không dùng các kiểm tra này thay cho Maven/JPA integration.

## Hotfix 05/11/2026 – Lỗi compile `MacAlgorithm` trong `AuthApi.java`

- **Thông tin thực tế từ Windows:** `mvn clean test` báo `cannot find symbol: variable MacAlgorithm`, biên dịch 19 file Java rồi dừng; không có test JUnit nào được chạy trong lượt này.
- **Sửa:** bổ sung `import org.springframework.security.oauth2.jose.jws.MacAlgorithm;` trước `org.springframework.security.oauth2.jwt.*;` trong `AuthApi.java`.
- **Kiểm tra trong môi trường sửa:** kiểm tra tĩnh vị trí import/khai báo `JwsHeader.with(MacAlgorithm.HS256)` và kiểm tra bộ business rules độc lập.
- **Trạng thái Maven/JUnit/khởi động:** PENDING, chờ log `mvn clean test` từ Windows. Không tuyên bố ứng dụng đã build hay chạy thành công.

## Hotfix 05/11/2026 – Trùng Bean khi khởi động

- **Nguyên nhân:** `@Configuration class Seed` mặc định mang tên Bean `seed`, trùng với method `@Bean seed(...)`.
- **Sửa:** đổi thành `@Bean seedDemoData(...)` trong `Seed.java`. Không bật cấu hình override Bean.
- **Kiểm tra đã chạy:** xác nhận trong mã nguồn `Seed` và `seedDemoData` không còn trùng tên Bean; chạy lại domain checks.
- **Chưa kiểm chứng:** Spring application context qua Maven, giao diện và database trong bản này; cần chạy `mvn clean test` và `mvn spring-boot:run` trên môi trường có Maven.

## Kết quả đã chạy thật

| ID | Phạm vi | Lệnh / phương thức | Kết quả |
|---|---|---|---|
| T-001 | Business rules Java | `bash scripts/test-core.sh` (`javac -Xlint:all -Werror`) | **PASS 177/177** |
| T-002 | JSX/JS syntax `App.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-003 | JSX/JS syntax `pages.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-004 | JSX/JS syntax `main.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-005 | JS syntax `api.js` | TypeScript transpile parser | **PASS syntax** |
| T-006 | JS syntax `vite.config.js` | TypeScript transpile parser | **PASS syntax** |

177 kiểm tra domain bao gồm: validation nội dung (trống, vượt giới hạn, giá, diện tích NaN/vô cực, ảnh), chuyển trạng thái cho tất cả enum, review duyệt/từ chối, checkout, sửa nháp, tạm ẩn/mở lại, quá hạn chính xác theo thời điểm, bất động sản đã thuê, thời hạn gói, cấp ưu tiên. **Đây là kiểm tra `Rules.java`, không phải 177 test toàn API.**

## Test tích hợp đã viết nhưng CHƯA chạy được

File `backend/src/test/java/com/easyhome/MarketplaceFlowTest.java` gồm:

- `paidListingRequiresReviewAndPaymentAndReplayIsSafe`: tạo tin → chờ duyệt → duyệt → đơn VIP → mock confirm → thử lặp → public hiển thị.
- `freeBenefitLimitedAndRelistAfterClose`: FREE một lần → chặn lần hai → đóng → đăng lại bản nháp.
- `crossAccountOperationsBlockedAndInvalidListingRejected`: không cho tài khoản khác submit tin, loại dữ liệu không hợp lệ, chặn thao tác chưa đăng nhập.
- `lazyRelationsKeepListingDetailsAndContactData`: hồi quy trang chi tiết, yêu thích, liên hệ và nhận liên hệ với entity LAZY.
- `editedDraftPropertyDetailsSurviveReloadFromDatabase`: sửa Property trong bản nháp và xác nhận dữ liệu tồn tại sau request/tải lại.

**Trạng thái:** sau bản vá lazy, 5 test đã chạy trên Windows: **4 PASS, 1 FAILED (payment confirm)**. Bản hotfix payment hiện tại **PENDING** Maven chạy lại. Cần chạy `cd backend && mvn test` trên GitHub Actions hoặc máy local có Maven. Nếu test lỗi phải sửa, chạy lại; không tuyên bố release.

## Các lượt test bắt buộc trước khi dùng thật

| TC | Kịch bản | Kỳ vọng | Trạng thái |
|---|---|---|---|
| E2E-01 | Khách xem và lọc tin | Chỉ ACTIVE + AVAILABLE + chưa hết hạn | Chưa chạy |
| E2E-02 | Tạo tài khoản / đăng nhập / token hết hạn | Quyền hoạt động, trả 401 phù hợp | Chưa chạy |
| E2E-03 | USER tạo nháp và sửa | Chỉ chủ tin sửa được | Chưa chạy |
| E2E-04 | Tin gửi duyệt | Không công khai trước khi duyệt | Chưa chạy |
| E2E-05 | Moderator từ chối + sửa gửi lại | Lý do lưu, trạng thái đúng | Chưa chạy |
| E2E-06 | Gói STANDARD/VIP | Đúng giá snapshot, đúng thời hạn | Chưa chạy |
| E2E-07 | Callback thanh toán lặp | Không cộng thời hạn / tạo dịch vụ hai lần | Chưa chạy |
| E2E-08 | FREE lần đầu và lần hai | Lần hai từ chối kể cả đồng thời | Chưa chạy |
| E2E-09 | Tạm ẩn/mở lại/hết hạn | Không thể mở sau hết hạn | Chưa chạy |
| E2E-10 | Đã cho thuê | Tin bị đóng, không mở lại | Chưa chạy |
| E2E-11 | Đăng lại tin cũ | Tạo nháp mới cùng Property, không trùng tin ACTIVE | Chưa chạy |
| E2E-12 | Tin yêu thích, liên hệ, báo cáo | Phân quyền + chặn spam | Chưa chạy |
| E2E-13 | Xử lý báo cáo gỡ tin | Tin biến mất công khai, log thao tác | Chưa chạy |
| E2E-14 | Docker MySQL clean/restart | Dữ liệu bền vững, healthcheck đúng | Chưa chạy |
| E2E-15 | Browser mobile/tablet | Layout và thao tác dễ dùng | Chưa chạy |
| SEC-01 | JWT giả mạo, expired, khác user | 401/403 đúng | Chưa chạy |
| SEC-02 | Input độc hại, URL ảnh xấu, XSS | Không vượt kiểm tra và không chèn script | Chưa chạy |
| PERF-01 | 1.000+ tin, nhiều người dùng | Phân trang và phản hồi ổn định | Chưa chạy |

## Cách chạy và ghi kết quả

1. Trên repo mới, mở **Actions** xem hai job `backend` và `frontend`. Trường hợp fail, lưu log lỗi.
2. Chạy `mvn test` backend; nếu cần xem chi tiết: `backend/target/surefire-reports/`.
3. Chạy `npm install && npm run build`; output thành công nằm trong `frontend/dist/`.
4. Khởi động ứng dụng và chạy 10 bước ở README. Với mỗi TC: ghi thời điểm, tài khoản test, dữ liệu vào, kết quả API, ảnh màn hình (nếu có), PASS/FAIL, link issue/commit sửa.
5. Sau mỗi sửa lỗi, chạy lại core tests + integration + E2E trọng yếu, tránh hồi quy.

**Tiêu chí phát hành MVP:** CI xanh, quy trình đăng tin từ đầu đến cuối qua browser không lỗi, phân quyền được kiểm chứng, test đồng thời thanh toán miễn phí, Docker/MySQL dữ liệu bền vững. Trước đó chỉ coi đây là **bản source phát triển**.
