# Chạy EasyHome V2 trên XAMPP (local dev)

1. Sao lưu `backend/data`, database MariaDB và thư mục project cũ; không paste replace ngay nếu còn dữ liệu chưa kiểm tra.
2. Bật MySQL trong XAMPP. **Không cần tự tạo database** nếu chạy `start-dev.bat`: file này gọi `scripts/init-local-db.ps1` để tạo database bằng `CREATE DATABASE IF NOT EXISTS`, không xóa dữ liệu cũ.
   - Nếu bạn khởi động thủ công bằng `mvn spring-boot:run`, hãy chạy một lần `powershell -ExecutionPolicy Bypass -File .\scripts\init-local-db.ps1` **tại thư mục gốc EasyHome**, trước khi chạy backend.
   - Script cần tìm thấy `C:\xampp\mysql\bin\mysql.exe`; nếu XAMPP ở nơi khác, đặt `$env:XAMPP_HOME='D:\DuongDan\xampp'`.
   - Database đã tồn tại thì giữ nguyên; Flyway + Hibernate chỉ tiếp tục cập nhật schema trong local dev.
3. Mở PowerShell ở `backend`:

```powershell
mvn clean test
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn spring-boot:run
```

4. PowerShell ở `frontend`: `npm install`, rồi `npm run dev`.
5. Mở `http://localhost:5173`. OTP email local hiển thị ở console Spring Boot dưới dòng `[EASYHOME DEV EMAIL OTP]`.

**Không sử dụng giấy tờ tùy thân thật hay chuyển tiền thật khi chưa nghiệm thu bảo mật.** Tất cả giao dịch ở profile dev chỉ là mô phỏng.

Nếu DB khác cổng `3306`, đặt `$env:DB_URL = 'jdbc:mariadb://127.0.0.1:3307/easyhome'` trước khi chạy backend. Nếu root có mật khẩu, đặt `$env:DB_PASSWORD` trong terminal, không ghi password vào file Git.

**Database đang có dữ liệu:** Flyway tạo bảng bổ sung, còn cột legacy vẫn cập nhật bởi JPA ở chế độ dev. Khuyến nghị chạy trước trên database thử nghiệm riêng. Chưa xác minh migration production và không có đường rollback tự động. Xem `README.md`.

## Database tự động: giới hạn quan trọng
- **Tự tạo database nếu thiếu:** có, khi dùng `start-dev.bat` hoặc chạy script `scripts/init-local-db.ps1` bằng tay.
- **Tự thêm bảng/cột:** Flyway chạy `V1`/`V2`, Hibernate chạy `ddl-auto=update` trong local dev **khi kết nối thành công**.
- **Không tự xóa/tái tạo dữ liệu**: không dùng `DROP DATABASE`, `CREATE-DROP`, `DELETE`.
- **Không tự chữa dữ liệu không tương thích:** trùng số điện thoại cũ, schema cũ có xung đột, giao dịch legacy, dữ liệu H2 cũ vẫn cần kiểm tra/migration riêng.
- Luôn backup trước khi chạy trên database EasyHome đã có dữ liệu. Chưa có chứng nhận migration production và chưa test tích hợp MariaDB ở môi trường tạo ZIP.
