# EasyHome V2 Fix03 — kiểm tra và giới hạn xác minh

## Lỗi trong log 03:01 ngày 12/11/2026

- H2 test đã build Java; **3 PASS / 2 FAIL**.
- `IdentityApi.review`: response VERIFIED nhưng `/api/auth/me` vẫn PENDING. Trong cùng transaction, User được tham chiếu qua `IdentityVerification.user` (lazy proxy); gán thẳng field không đáng tin cậy. Đổi sang `User.setIdentityStatus()` để Hibernate khởi tạo proxy trước khi ghi.
- `BillingApi.confirmMock`: tạo đơn trả 200 nhưng mock pay trả 400, trong khi `l.status` được đọc trực tiếp trên entity có thể là lazy proxy sau khi load ServiceOrder. Đổi sang `Listing.getStatus()`; các bước chuyển trạng thái gọi `Listing.setStatus()`.
- Chuyển các thao tác tương tự trong ListingApi/ModerationService sang setter để giảm tái phát.

## Đã chạy tại môi trường đóng gói

- Rule tests (`scripts/test-core.sh`) — chạy được với JDK, không cần Maven.
- Java parser/tree-sitter + kiểm tra import và nguồn cấu hình SMTP.
- Frontend JavaScript syntax/import checks, không thay đổi giao diện.

## Chưa chạy ở đây

- Không có Maven repository và kết nối XAMPP/MariaDB để chạy `mvn clean test` / integration database thực.
- Không có tài khoản Gmail + App Password của người dùng; **chưa thể xác nhận gửi Gmail thật**.

## Lệnh người dùng chạy trên Windows

```powershell
cd backend
mvn clean test
# Nếu 5/5 pass, chạy với database thử nghiệm:
$env:DB_URL='jdbc:mariadb://127.0.0.1:3306/easyhome_v2_test'
$env:SPRING_PROFILES_ACTIVE='dev'
mvn spring-boot:run
```

**Không ghi đè database chính khi chưa backup.** `README_XAMPP.md` và `GMAIL_OTP_SETUP.md` có cấu hình.

## Bài test hồi quy mở rộng (8 bài test)

- `privateEvidenceRequiresOwnerOrAuthorizedStaff`: ảnh minh chứng không công khai cho người lạ.
- `onlyStaffMayApproveListings`: USER không được duyệt bài của chính mình.
- `demoWalletTopupProducesLedgerEntry`: nạp tiền giả ghi đúng số dư và sổ giao dịch.

**Trạng thái:** Các test mới đã được thêm và kiểm tra cú pháp; Maven/JUnit chưa chạy trong môi trường đóng gói.

## Chạy toàn bộ test cần thiết trên Windows

Từ thư mục gốc:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-phase1.ps1
```

Script chạy **Maven integration tests** bằng H2 và **npm install + npm run build**. Nếu một bước lỗi, script dừng và in lỗi. Sau đó cần chạy thêm test XAMPP/MariaDB và gửi OTP Gmail thật trên tài khoản thử nghiệm (không cung cấp App Password cho bất cứ ai).
