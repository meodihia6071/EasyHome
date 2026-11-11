# EasyHome V2 Phase 1 – thay đổi mã nguồn

**Dựa trên:** https://github.com/meodihia6071/EasyHome, nhánh main, commit đối chiếu `9851a06f80a470b7373453273c69b874ab3af637`.

## Giữ nguyên
- Mô hình sàn đăng tin trung gian, không thu cọc/tiền thuê.
- React/Vite + Spring Boot/Java 21 + MariaDB/XAMPP.
- Tên các file Java/JSX hiện có và các chức năng tìm kiếm, liên hệ, favorite, report cơ bản.

## Thay đổi
- `Rules.java`: trạng thái/luồng sau thanh toán, 5 cấp tin (giữ VIP legacy để đọc dữ liệu cũ).
- `AuthApi.java`, `User.java`, `EmailService.java`, `EmailOtp.java`, `RegistrationCleanupJob.java`: OTP email, kiểm tra trùng email/số điện thoại, tài khoản chờ xác thực và hạn gửi OTP.
- `IdentityApi.java`, `IdentityVerification.java`: hồ sơ xác thực danh tính và duyệt thủ công riêng.
- `MediaApi.java`, `ListingMedia.java`: tải/lưu riêng ảnh công khai và minh chứng.
- `ListingApi.java`, `Listing.java`, `ModerationService.java`, `ModerationApi.java`, `ListingReview.java`, `AdminApi.java`: luồng thanh toán trước, kiểm duyệt sau, xem ảnh, yêu cầu bổ sung, quyết định có lịch sử.
- `BillingApi.java`, `ServiceOrder.java`, `PaymentTransaction.java`: đơn, thanh toán giả, snapshot, hoàn tiền idempotent.
- `WalletApi.java`, `WalletService.java`, `WalletAccount.java`, `WalletLedger.java`, `WithdrawalRequest.java`: số dư demo, lịch sử, rút demo thủ công.
- `AccountManagementApi.java`, `AccountLockHistory.java`: khóa/mở theo hạn và lịch sử.
- `PlanCatalog.java`, `ServicePlan.java`, `ServicePlanHistory.java`: bảng giá có phiên bản và đơn chụp giá.
- `Repos.java`: repository mới, khóa DB cho các thao tác nhạy cảm.
- `frontend/src/phase1.jsx`: các màn hình/luồng nghiệp vụ V2; `App.jsx`, `pages.jsx`, `api.js`, `style.css` thêm điều hướng và thành phần cần thiết.
- `application.yml`, `application-dev.yml`, `application-prod.yml`, `pom.xml`: Spring profiles, Flyway, SMTP, upload, mock guard.
- `db/migration`: migration bổ sung bảng; **các thay đổi cột legacy vẫn cần quy trình migration production riêng**.
- `MarketplaceFlowTest.java`, `verification/RulesChecks.java`: cập nhật các phép kiểm tra theo quy tắc V2.

## Trạng thái
- **Đã viết mã:** các phân hệ nêu trên.
- **Đã chạy:** Java domain checks 198/198, Java syntax, JSX syntax.
- **Chưa chạy:** Maven/JUnit thực tế, npm build, MariaDB/Flyway startup, kiểm thử HTTP/browser E2E.
- **Chưa có:** SMS OTP, QR/cổng thanh toán thật, chuyển tiền ngân hàng, tin đẩy VIP nâng cao/hội viên (giai đoạn sau).

## Nguyên tắc bàn giao
Không tự đẩy repository GitHub, không xóa database. Trước khi thay source thật, chạy toàn bộ kiểm thử trên bản sao project và database thử nghiệm. Khi có log BUILD FAILURE, sửa lỗi theo log trước khi sử dụng môi trường chính.

- Bổ sung `scripts/init-local-db.ps1`, `start-dev.bat` tự kiểm tra/tạo MariaDB local không phá dữ liệu, cập nhật hướng dẫn. Chưa kiểm thử XAMPP trên máy Windows.
