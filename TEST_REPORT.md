# TEST REPORT — EasyHome V2 / Phase 1

Ngày lập: 12/11/2026.

## Kiểm thử đã chạy

| Kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| Domain rules `bash scripts/test-core.sh` | **PASS 198/198** | `verification/RulesChecks.java` |
| Java parse 41 source và test `.java` | **PASS cú pháp** | JDK 21 JavaCompiler parse |
| React JSX/JS parse 4 file chính | **PASS cú pháp** | TypeScript compiler parser |
| Maven `mvn clean test` | **CHƯA CHẠY** | Môi trường không có Maven/dependencies |
| React `npm install && npm run build` | **CHƯA CHẠY** | Môi trường không truy cập npm registry |
| MariaDB XAMPP/Flyway | **CHƯA CHẠY** | Không có máy chủ MariaDB local trong container |
| Browser E2E / phân quyền trên môi trường live | **CHƯA CHẠY** | Không khởi động được backend/frontend tích hợp |

**Không được coi kết quả parse là compile/build thành công.**

## Kịch bản đã viết vào MarketplaceFlowTest.java (CHƯA CHẠY)

1. OTP đăng ký, chặn đăng nhập khi email chưa xác thực, số điện thoại trùng chuẩn hóa và OTP sai.
2. Đăng ký → OTP email → upload hồ sơ danh tính → Admin duyệt → upload tin → mock QR → gửi kiểm duyệt → Moderator duyệt → công khai.
3. Thanh toán trước, từ chối sau, hoàn tiền vào số dư đúng một lần, chặn từ chối lặp.
4. Chỉ được dùng FREE một lần đối với tin có quyền lợi đã kích hoạt.
5. Khóa tài khoản theo hạn, kiểm tra lịch sử, mở lại và đăng nhập.

## Bắt buộc chạy trên Windows trước khi thay source chính

```powershell
cd backend
mvn clean test
$env:SPRING_PROFILES_ACTIVE='dev'
mvn spring-boot:run
```

Trong terminal khác:

```powershell
cd frontend
npm install
npm run build
npm run dev
```

Sau đó tạo database test riêng trong XAMPP, chạy thực tế luồng 1–8 tại `README.md`. Nếu `BUILD FAILURE`, **không ghi đè source đang hoạt động**. Lưu lỗi ở `backend/target/surefire-reports/` và log Spring Boot.

## Những hạn chế còn cần nghiệm thu / kiểm tra

- Flyway + Hibernate update trên database cũ có thể cần kiểm tra thủ công các cột/ràng buộc và những bản ghi theo luồng V1.
- Dữ liệu định danh thực cần đánh giá bảo mật/lưu giữ trước khi dùng production.
- Số dư/QR/rút đều là demo và không kết nối tiền thật.
- Email SMTP production chưa được cấu hình/kiểm tra.
- Người dùng và frontend cần thử ảnh, mã OTP sai/hết hạn, đồng thời nạp/hoàn tiền, lịch sử duyệt và khóa/mở.
- File `frontend/package-lock.json` nguồn không có trong ZIP bản làm việc; `npm install` sẽ tạo lại.


## Refactor cấu trúc – 12/11/2026

- Tổ chức source Java thành các package `api`, `service`, `model`, `repository`, `common`, `config`, `job`; tách các repository khỏi `Repos.java`.
- Tách màn hình React vào các feature folder, giữ các barrel export tương thích.
- **PASS 198/198** kiểm tra business rule độc lập Java.
- Parser JDK 21: 67 file `.java` không lỗi cú pháp; parser TS: 27 file JS/JSX không lỗi cú pháp; import đường dẫn tương đối hợp lệ.
- **CHƯA CHẠY** Maven test, npm build, MariaDB runtime/E2E trên source đã refactor; phải nghiệm thu lại các bước này trên máy Windows.
