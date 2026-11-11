> **Tổ chức lại source:** Xem `ARCHITECTURE.md` để hiểu cấu trúc `api/service/model/repository` (backend) và `pages/components/services` (frontend). Thao tác refactor không đổi luồng nghiệp vụ.

# EasyHome V2.0 — Phase 1 (bản phát triển và kiểm thử)

**Nguồn chuẩn:** https://github.com/meodihia6071/EasyHome, nhánh `main` (đối chiếu commit `9851a06f80a470b7373453273c69b874ab3af637`).

**Đã viết source:** Email OTP, số điện thoại duy nhất; xác thực người đăng với 2 ảnh tài liệu riêng; ảnh đăng tin và minh chứng riêng; thanh toán giả/đơn dịch vụ/số dư; kiểm duyệt sau thanh toán; từ chối và hoàn tiền; yêu cầu bổ sung 72 giờ; khóa/mở tài khoản có lịch sử; bảng giá lưu DB, API điều chỉnh giá; yêu cầu rút mô phỏng và Finance xử lý; Flyway bổ sung bảng.

**KHÔNG SỬ DỤNG ĐỂ GIAO DỊCH TIỀN THẬT.** Nạp tiền, QR, số dư và rút đều mô phỏng. Không có kết nối ngân hàng/cổng thanh toán thật. Ảnh CCCD chỉ dùng để thử luồng bằng tài liệu thử nghiệm phù hợp; chưa nghiệm thu bảo mật dữ liệu nhạy cảm.

**KẾT QUẢ ĐÃ CHẠY Ở MÔI TRƯỜNG TẠO SOURCE:** Java rules 198/198 PASS; Java syntax parse 41 file PASS; frontend JSX syntax parse 4 file PASS. **KHÔNG CÓ MAVEN/npm dependencies hoặc MariaDB server ở môi trường biên soạn** nên `mvn clean test`, `npm run build`, Spring context và E2E chưa được xác minh. Không coi ZIP là release production.

## 1. Cài đặt và chạy local trên Windows

Yêu cầu: JDK 21 (khuyến nghị, tránh Java 27), Maven 3.9+, Node.js 22+, XAMPP MariaDB/MySQL, và Internet lần đầu để cài dependencies. **Sao lưu source và xuất database trước khi thử bản V2.**

1. Bật **MySQL** trong XAMPP. Chạy `start-dev.bat` ở thư mục gốc để **tự tạo database `easyhome` nếu chưa có**. Nếu muốn chạy backend thủ công, chạy `powershell -ExecutionPolicy Bypass -File .\scripts\init-local-db.ps1` trong thư mục gốc trước, rồi mới chạy `mvn spring-boot:run`. Có thể tạo database thủ công bằng `database.sql` nếu script không dùng được.
2. Mở PowerShell trong thư mục `backend`:

```powershell
mvn clean test
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn spring-boot:run
```

3. Mở PowerShell thứ hai trong `frontend`:

```powershell
npm install
npm run dev
```

4. Trang web `http://localhost:5173`; API `http://localhost:8081/api/listings`; phpMyAdmin `http://localhost/phpmyadmin`.

**Quan trọng:** Phải dùng profile `dev` để bật OTP email in trên console và thanh toán giả. Không sử dụng profile này trên máy chủ công khai. Nếu không có profile `dev`, mặc định không bật mock-payment và email phải cấu hình SMTP.

Nếu XAMPP dùng mật khẩu hoặc cổng khác, đặt biến môi trường trước khi chạy backend:

```powershell
$env:DB_URL = 'jdbc:mariadb://127.0.0.1:3306/easyhome'
$env:DB_USER = 'root'
$env:DB_PASSWORD = ''
```

## 2. Luồng thử nghiệm đầy đủ

1. **Đăng ký:** mở `/register`. Email và số điện thoại không được trùng. Sau khi đăng ký, chuyển đến `/verify-email`. Mã OTP 6 số được in trong terminal backend dưới dòng `[EASYHOME DEV EMAIL OTP]`. OTP hết hạn sau 5 phút, gửi lại sau 60 giây, tối đa 5 email OTP/24h và 5 lần nhập sai/mã. Tài khoản chưa xác thực hết thời hạn chờ 24 giờ sẽ được dọn bằng scheduler.
2. **Xác thực người đăng:** đăng nhập rồi mở `/identity`. Chọn `Bắt đầu xác thực`, tải **hai ảnh thử nghiệm** (JPEG/PNG), nhấn gửi. Admin đăng nhập → `/staff` → xem từng ảnh, duyệt hoặc yêu cầu bổ sung. Một tài khoản duyệt thành công chỉ phải xác thực lần nữa nếu sau này bị thu hồi/quy định yêu cầu.
3. **Tạo tin:** mở `/new`, nhập thông tin và lưu `DRAFT`. `/media/:id`: tải ít nhất **hai ảnh công khai** (`PUBLIC`) và **một ảnh minh chứng riêng tư** (`EVIDENCE`). Mỗi file tối đa 6 MB, JPEG/PNG đã xác minh cấu trúc.
4. **Mua gói:** `/checkout/:id`. Chọn FREE (0đ/7 ngày một lần), STANDARD (29.000đ/15 ngày), VIP Bạc (79.000đ/15 ngày), VIP Vàng (149.000đ/15 ngày) hoặc VIP Kim Cương (299.000đ/15 ngày). Có thể dùng `/wallet` nạp số dư demo rồi thanh toán ví DEMO, hoặc nhấn `Mô phỏng QR thành công` để tạo thanh toán giả cho đơn. **Không quét hoặc chuyển tiền thật.**
5. **Gửi kiểm duyệt:** sau khi đơn được ghi `PAID`, bấm `Gửi tin kiểm duyệt`; `PENDING_REVIEW` không xuất hiện công khai.
6. **Moderator:** đăng nhập `mod@easyhome.vn` (tài khoản demo), chọn `Kiểm duyệt`, mở **chi tiết bài** để kiểm tra đầy đủ nội dung, ảnh, chứng cứ và lịch sử. Duyệt → `ACTIVE` và thời hạn bắt đầu tại thời điểm duyệt. Từ chối (lý do ≥10 ký tự) → `REJECTED`, đơn `REFUNDED`, số tiền đưa vào số dư demo. Yêu cầu bổ sung → `NEEDS_UPDATE` trong 72 giờ rồi tự từ chối/hoàn khi quá hạn.
7. **Khóa/mở:** Admin mở `/staff`, khóa theo số ngày hoặc không thời hạn, ghi lý do, xem lịch sử, mở trước hạn nếu cần. Scheduler xử lý khóa hết hạn.
8. **Rút DEMO:** `/wallet` tạo yêu cầu rút từ **số dư giả**; Admin hoặc FINANCE `/staff` từ chối (giải phóng số dư giữ) hoặc hoàn thành **mô phỏng**, không chuyển tiền ngân hàng.

Tài khoản seeded **chỉ dùng trên máy local**: `admin@easyhome.vn` / `Admin@12345`, `mod@easyhome.vn` / `Mod@123456`, `owner@easyhome.vn` / `Owner@12345`, `user@easyhome.vn` / `User@123456`.

## 3. Bảo mật dữ liệu

- Ảnh được lưu ở `backend/private-uploads/`, **không** ở thư mục web public. `/api/media/{id}` kiểm tra quyền. Tài liệu xác thực chỉ owner và Admin/Super Admin xem được; Moderator chỉ xem ảnh đăng tin/minh chứng tài sản và trạng thái người đăng.
- File upload kiểm tra magic bytes JPEG/PNG, ảnh giải mã được, giới hạn kích thước. Đường dẫn lưu trữ ngẫu nhiên, không trả về filesystem path.
- `APP_MOCK_PAYMENTS=false` theo mặc định và profile `prod` từ chối các API giả lập. `app.email-mode=console` chỉ dùng trong profile dev/test.
- Sổ giao dịch dùng reference duy nhất, số dư demo tách khỏi tiền thật. Không có endpoint chi tiền thật; hoàn tiền khi bị từ chối idempotent.
- Đây là hệ thống đang phát triển, **chưa đủ điều kiện tiếp nhận CCCD thật hoặc giao dịch thật** khi chưa kiểm thử xâm nhập, quyền truy cập, lưu giữ dữ liệu và đánh giá pháp lý.

## 4. Database và nâng cấp source cũ

- Flyway bổ sung bảng qua `backend/src/main/resources/db/migration/V1__base_marker.sql` và `V2__phase1_addon_tables.sql`.
- Trên máy dev, `ddl-auto=update` còn tự bổ sung trường vào các bảng V1 (`users`, `listings`, `service_orders`). **Chưa có migration production đầy đủ cho các trường cũ**, nên profile `prod` (`ddl-auto=validate`) có thể không khởi động nếu chưa chuyển đổi schema.
- Dữ liệu `PENDING_REVIEW`/`AWAITING_PAYMENT` thuộc luồng cũ **không tự chuyển** thành các đơn hợp lệ của luồng V2. Sau khi backup, phải đối chiếu trạng thái các tin chờ xử lý trước khi chạy V2 trên database cũ.
- Dữ liệu H2 file cũ không được tự động nhập vào MariaDB.
- Tài khoản demo seeded trước đó được đánh dấu xác thực theo đúng credential demo; các tài khoản thật có từ trước phải xác thực email/danh tính theo quy trình mới.
- Đối với số điện thoại cũ dạng `0...`, đăng ký mới kiểm tra trùng cả hai dạng `0...` và `+84...`; trước khi áp dụng unique index và thay đổi dữ liệu lớn cần chuẩn hóa dữ liệu cũ có kiểm soát.
- Nếu cần an toàn tối đa, chạy thử trên **database `easyhome_v2_test` riêng biệt** (sửa DB_URL tương ứng) trước khi chuyển bản gốc.

## 5. Các API mới

| Nhóm | Endpoint |
|---|---|
| OTP | `POST /api/auth/register`, `/api/auth/verify-email`, `/api/auth/resend-email` |
| Identity | `GET /api/identity/me`, `POST /api/identity/start`, `/api/identity/{id}/upload`, `/api/identity/{id}/submit` |
| Identity Admin | `GET /api/admin/identity/pending`, `POST /api/admin/identity/{id}/review` |
| Media | `POST /api/my/listings/{id}/media`, `GET /api/my/listings/{id}/media`, `GET /api/media/{id}` |
| Review | `GET /api/admin/listings/{id}/moderation-detail`, `POST /api/admin/listings/{id}/needs-update`, `/approve`, `/reject` |
| Orders | `POST /api/orders`, `/api/orders/{id}/wallet-pay`, `/api/dev/orders/{id}/confirm`, `/api/dev/orders/{id}/fail` |
| Wallet | `GET /api/wallet`, `/api/wallet/ledger`, `POST /api/dev/wallet/topup` |
| Withdrawal | `GET/POST /api/wallet/withdrawals`, `GET /api/finance/withdrawals`, `POST /api/finance/withdrawals/{id}/decision` |
| Account locks | `POST /api/admin/users/{id}/lock`, `/unlock`, `GET /api/admin/users/{id}/lock-history` |
| Pricing | `GET /api/plans`, `POST /api/admin/plans/{code}` |

**Giai đoạn tiếp theo, chưa có trong bản này:** giao dịch thật/QR ngân hàng, đẩy tin ngay/lượt/hẹn giờ, hội viên môi giới, tính chênh lệch nâng cấp, email SMTP production/OTP điện thoại; không dùng các tính năng demo thay thế dịch vụ thật.

## 6. Bản source và hạn chế môi trường

- Source bàn giao là bản chỉnh sửa dựa trên các file EasyHome nhánh `main` đã đối chiếu. ZIP không kèm `.git`, dữ liệu `.env`, database hay ảnh CCCD.
- File `frontend/package-lock.json` gốc không có trong bản ZIP nguồn do môi trường không tải được archive binary đầy đủ. Chạy `npm install` để tái tạo lock; không dùng `npm ci` cho đến khi có file lock hợp lệ.
- Thư mục `private-uploads/` **không** được push lên GitHub và phải được backup/thiết lập phân quyền riêng khi vận hành.
- Xem `TEST_REPORT.md` và `PHASE1_CHANGELOG.md` để biết chính xác kiểm thử đã chạy và các điểm cần nghiệm thu.

### Bản vá bổ sung: tự khởi tạo database local
`start-dev.bat` gọi `scripts/init-local-db.ps1` trước khi chạy các terminal. Script chỉ chạy `CREATE DATABASE IF NOT EXISTS`, không xóa DB hoặc bảng nào. Flyway V1/V2 và Hibernate tạo/cập nhật schema khi backend chạy. Phải sao lưu DB cũ; không thể tự xử lý mọi trường hợp dữ liệu legacy trùng hoặc sai.


### Gmail SMTP và kiểm thử Fix03
- Xem `GMAIL_OTP_SETUP.md` để cấu hình Gmail App Password qua biến môi trường, không commit mật khẩu.
- Xem `FIX03_TEST_GUIDE.md` và chạy `scripts/test-phase1.ps1` để test Maven + frontend trên Windows.
