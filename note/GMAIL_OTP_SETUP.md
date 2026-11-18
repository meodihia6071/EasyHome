# EasyHome - gửi OTP bằng Gmail (SMTP)

## Tạo Google App Password

1. Bật **Xác minh 2 bước** cho Gmail tại https://myaccount.google.com/security.
2. Mở https://myaccount.google.com/apppasswords và đăng nhập.
3. Tạo **App Password** với tên `EasyHome Local`; Google cấp chuỗi 16 ký tự.
4. Copy chuỗi này **một lần**. Đây là mật khẩu ứng dụng, KHÔNG phải mật khẩu đăng nhập Gmail.
5. Không gửi App Password cho người khác, không đưa vào GitHub, log, commit hoặc ảnh chụp màn hình.

Google có thể không hiển thị App Password nếu tài khoản thuộc Workspace do quản trị viên hạn chế, bật Advanced Protection hoặc cấu hình 2 bước không phù hợp.

## Cấu hình backend

File `backend/src/main/resources/application.yml` đã có cấu hình SMTP qua biến môi trường:
- `spring.mail.host=${SMTP_HOST:smtp.gmail.com}`
- `spring.mail.port=${SMTP_PORT:587}` (STARTTLS)
- `spring.mail.username=${SMTP_USERNAME:}`
- `spring.mail.password=${SMTP_APP_PASSWORD:}`
- `app.email-mode=${APP_EMAIL_MODE:smtp}`
- `app.email-from=${APP_EMAIL_FROM:${SMTP_USERNAME:no-reply@easyhome.vn}}`
- `app.frontend-url=${APP_FRONTEND_URL:http://localhost:5173}` (được dùng làm URL trong thư đặt lại mật khẩu)

Email này được dùng cho OTP đăng ký, OTP bắt buộc khi Admin/Staff đăng nhập, và liên kết đặt lại mật khẩu. OTP Admin/Staff chỉ trả session sau khi mã đúng; mã hết hạn sau 5 phút, tối đa 5 lần gửi trong 24 giờ và 5 lần nhập sai.

### Lưu cấu hình trong project để chạy bằng `start-dev.bat`

Tạo file `.env` ở thư mục gốc project (cùng cấp với `start-dev.bat`) và điền:

```dotenv
APP_EMAIL_MODE=smtp
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=yourgmail@gmail.com
SMTP_APP_PASSWORD=PASTE_A_NEW_GMAIL_APP_PASSWORD_HERE
APP_EMAIL_FROM=yourgmail@gmail.com
APP_FRONTEND_URL=http://localhost:5173
```

Thay `SMTP_APP_PASSWORD` bằng App Password mới, bỏ khoảng trắng giữa các nhóm ký tự, rồi chạy `start-dev.bat`. Launcher tự nạp `.env`; file này đã nằm trong `.gitignore` và không được commit. Không dùng mật khẩu đã dán trong chat.

**Dùng PowerShell** nếu chạy backend riêng, thay bằng email và App Password của chính bạn:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:APP_EMAIL_MODE = 'smtp'     # SMTP là chế độ mặc định
$env:SMTP_USERNAME = 'yourgmail@gmail.com'
$env:APP_EMAIL_FROM = $env:SMTP_USERNAME
$env:SMTP_APP_PASSWORD = Read-Host 'Dán Gmail App Password (không hiện ký tự)' -AsSecureString | ForEach-Object { [System.Net.NetworkCredential]::new('', $_).Password }
cd backend
mvn spring-boot:run
```

**Chú ý:** Lệnh `Read-Host -AsSecureString` không hiển thị mật khẩu; giá trị vẫn được chuyển thành biến môi trường cho JVM. Khi kết thúc phiên thử nghiệm, xóa biến môi trường nhạy cảm: `Remove-Item Env:SMTP_APP_PASSWORD`.

Nếu dùng `start-dev.bat`, hãy thiết lập các biến môi trường trong terminal trước khi chạy batch. Profile `dev` mặc định gửi email qua SMTP.

Nếu chỉ muốn test mà **không gửi Gmail**: `$env:APP_EMAIL_MODE='console'` (chỉ profile dev/test), mã OTP sẽ xuất hiện trong terminal backend. Không bật chế độ này trên hệ thống public.

## Kiểm tra

Đăng ký bằng một email thử nghiệm khác email người gửi; kiểm tra Inbox/Spam. Nếu lỗi `535` hoặc `534`, kiểm tra bật 2 bước, App Password, đúng Gmail, và quyền truy cập tài khoản. Nếu gặp `Connection timed out`, kiểm tra SMTP port 587 và firewall.

Chỉ gửi thử OTP bằng email nhận do bạn kiểm soát. Google App Password có thể bị thu hồi sau khi đổi mật khẩu Google. Dữ liệu CCCD và tiền trong project này vẫn ở chế độ phát triển, không đưa ra Internet.
