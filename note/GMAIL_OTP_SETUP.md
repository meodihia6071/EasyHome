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

**Dùng PowerShell**, thay bằng email và App Password của chính bạn:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:APP_EMAIL_MODE = 'smtp'     # Ghi đè chế độ console mặc định của profile dev
$env:SMTP_USERNAME = 'yourgmail@gmail.com'
$env:APP_EMAIL_FROM = $env:SMTP_USERNAME
$env:SMTP_APP_PASSWORD = Read-Host 'Dán Gmail App Password (không hiện ký tự)' -AsSecureString | ForEach-Object { [System.Net.NetworkCredential]::new('', $_).Password }
cd backend
mvn spring-boot:run
```

**Chú ý:** Lệnh `Read-Host -AsSecureString` không hiển thị mật khẩu; giá trị vẫn được chuyển thành biến môi trường cho JVM. Khi kết thúc phiên thử nghiệm, xóa biến môi trường nhạy cảm: `Remove-Item Env:SMTP_APP_PASSWORD`.

Nếu dùng `start-dev.bat`, hãy thiết lập các biến môi trường trong terminal trước khi chạy batch; batch hiện kích hoạt profile `dev`, nhưng `APP_EMAIL_MODE=smtp` sẽ ghi đè `console` của dev.

Nếu chỉ muốn test mà **không gửi Gmail**: `$env:APP_EMAIL_MODE='console'` (chỉ profile dev/test), mã OTP sẽ xuất hiện trong terminal backend. Không bật chế độ này trên hệ thống public.

## Kiểm tra

Đăng ký bằng một email thử nghiệm khác email người gửi; kiểm tra Inbox/Spam. Nếu lỗi `535` hoặc `534`, kiểm tra bật 2 bước, App Password, đúng Gmail, và quyền truy cập tài khoản. Nếu gặp `Connection timed out`, kiểm tra SMTP port 587 và firewall.

Chỉ gửi thử OTP bằng email nhận do bạn kiểm soát. Google App Password có thể bị thu hồi sau khi đổi mật khẩu Google. Dữ liệu CCCD và tiền trong project này vẫn ở chế độ phát triển, không đưa ra Internet.
