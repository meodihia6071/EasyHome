# TEST REPORT — EasyHome V2 / Phase 1

Ngày lập: 15/11/2026.

## Kiểm thử đã chạy

| Kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| Domain rules `scripts/test-core.ps1` | **PASS 198/198** | `verification/RulesChecks.java`; PowerShell dùng `ExecutionPolicy Bypass` cho tiến trình này |
| Maven `mvn.cmd -q clean test` | **PASS 11/11** | Build sạch, H2 in-memory; 0 failure, 0 error |
| React `npm.cmd run build` | **PASS build** | Vite 7.3.8, production bundle thành công |
| MariaDB XAMPP/Flyway | **CHƯA CHẠY** | Không có máy chủ MariaDB local trong container |
| Browser E2E / phân quyền trên môi trường live | **CHƯA CHẠY** | Không khởi động được backend/frontend tích hợp |

## Kiểm thử sau cập nhật yêu cầu — 15/11/2026

| Kiểm tra | Kết quả | Chi tiết |
|---|---|---|
| `mvn.cmd -q clean test` | **PASS** | 11 test, 0 failure, 0 error; build sạch trên H2 in-memory; gồm OTP tự đăng nhập, khóa tài khoản, reset mật khẩu, thu hồi JWT cũ và tư cách tin/địa điểm |
| `npm.cmd run build` | **PASS** | Vite 7.3.8; 1.618 modules transformed; production bundle thành công |
| MariaDB/XAMPP migration/runtime | **CHƯA CHẠY** | Chưa có DB server kiểm chứng ở môi trường này |
| Gmail SMTP reset link | **CHƯA CHẠY** | Test sử dụng email console của profile test |
| Browser E2E / khóa tài khoản đang hoạt động | **CHƯA CHẠY** | Backend session-status được kiểm tra qua MockMvc; giao diện cần thử trên trình duyệt |

Các kết quả parser lịch sử không thay thế build; cập nhật này đã chạy Maven tests và Vite production build thành công.

## Kịch bản trong MarketplaceFlowTest.java (đã chạy 15/11/2026)

1. OTP đăng ký, chặn đăng nhập khi email chưa xác thực, số điện thoại trùng chuẩn hóa và OTP sai.
2. Đăng ký → OTP email → upload hồ sơ danh tính → Admin duyệt → upload tin → mock QR → gửi kiểm duyệt → Moderator duyệt → công khai.
3. Thanh toán trước, từ chối sau, hoàn tiền vào số dư đúng một lần, chặn từ chối lặp.
4. Chỉ được dùng FREE một lần đối với tin có quyền lợi đã kích hoạt.
5. Khóa tài khoản theo hạn, kiểm tra lịch sử, mở lại và đăng nhập.
6. Reset mật khẩu qua token, đăng nhập bằng mật khẩu mới, từ chối JWT cũ và chặn dùng lại token.
7. Chọn tư cách BROKER trên một tin và truy vấn danh sách vị trí ACTIVE.

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
- Nạp/rút dùng đối soát thủ công; ứng dụng chưa tích hợp webhook ngân hàng.
- Email SMTP production chưa được cấu hình/kiểm tra.
- Người dùng và frontend cần thử ảnh, mã OTP sai/hết hạn, đồng thời nạp/hoàn tiền, lịch sử duyệt và khóa/mở.
- File `frontend/package-lock.json` nguồn không có trong ZIP bản làm việc; `npm install` sẽ tạo lại.


## Refactor cấu trúc – 12/11/2026

- Tổ chức source Java thành các package `api`, `service`, `model`, `repository`, `common`, `config`, `job`; tách các repository khỏi `Repos.java`.
- Tách màn hình React vào các feature folder, giữ các barrel export tương thích.
- **PASS 198/198** kiểm tra business rule độc lập Java.
- Parser JDK 21: 67 file `.java` không lỗi cú pháp; parser TS: 27 file JS/JSX không lỗi cú pháp; import đường dẫn tương đối hợp lệ.
- **CHƯA CHẠY** Maven test, npm build, MariaDB runtime/E2E trên source đã refactor; phải nghiệm thu lại các bước này trên máy Windows.

## Kiểm thử và yêu cầu cập nhật — 16/11/2026

| Kiểm tra | Kết quả | Chi tiết |
|---|---|---|
| `scripts/test-core.ps1` | **PASS 198/198** | Java business rules |
| `mvn -Dmaven.repo.local=... clean test` | **PASS 14/14** | H2 + MockMvc; OTP/reset, checkout, moderation, nạp/rút, refund và cô lập số dư legacy mock |
| `npm.cmd run build` | **PASS** | Vite 7.3.8; production build |
| MariaDB/XAMPP + Flyway V6/V7 | **CHƯA CHẠY** | Không có MariaDB server để nâng cấp schema legacy |
| Gmail SMTP gửi thực | **CHƯA CHẠY** | Cần cấu hình App Password mới qua biến môi trường; thông tin đã dán trong chat cần thu hồi |
| Browser E2E / server live | **CHƯA CHẠY** | Chưa chạy toàn bộ luồng trên server/trình duyệt thật |

Kịch bản ví tích hợp xác nhận: tạo VietQR không tăng số dư; Admin không thấy yêu cầu trước khi người dùng bấm “Tôi đã chuyển khoản”; Admin duyệt nạp mới cộng số dư; gửi yêu cầu rút chưa trừ số dư; duyệt sau khi chuyển tiền mới trừ số dư. Build frontend và Maven/H2 test chạy sau các chỉnh sửa ngày 16/11.
Test ví khởi tạo giá trị legacy mock 90.000đ trên tài khoản rồi xác nhận /api/wallet vẫn trả số dư khả dụng 0đ trước khi nạp VietQR được Admin duyệt. Thanh toán VietQR đã duyệt được hoàn lại đúng một lần; từ chối đơn thanh toán mock cũ không tạo số dư khả dụng.

Kiểm thử bổ sung: Admin duyệt hồ sơ danh tính sang NEEDS_UPDATE thì lý do xuất hiện tại /api/identity/me và thao tác IDENTITY_NEEDS_UPDATE xuất hiện trong /api/admin/audit.
