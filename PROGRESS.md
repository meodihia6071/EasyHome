# PROGRESS — EasyHome

**Ngày ghi nhận:** 04/11/2026  |  **Nhánh gốc:** dự án mới, chưa push  |  **Giai đoạn:** MVP source + QA offline

Nguyên tắc: `DONE` = file đã tạo và quy tắc có thể kiểm tra; `TESTED` = thực tế có lệnh chạy và kết quả; `PENDING` = chưa được kiểm chứng. Không dùng từ "hoàn thành toàn hệ thống" khi chưa có build/test tích hợp.

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
