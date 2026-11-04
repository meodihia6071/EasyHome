# EasyHome — Sàn đăng tin cho thuê nhà (MVP)

**Bản làm lại độc lập**, không kế thừa GitHub cũ. EasyHome là trung gian **thu phí đăng/quảng bá tin**; không nhận tiền cọc, tiền thuê, không duyệt yêu cầu thuê thay chủ nhà. Người tìm thuê liên hệ trực tiếp với người đăng tin.

> **Trạng thái:** Có source React + Spring Boot + H2/MySQL và bộ test. Bộ quy tắc Java đã chạy thành công 177 phép kiểm tra. **Chưa xác minh build Spring Boot, React hoặc luồng HTTP chạy thật trong phiên phát triển này** (môi trường không có Maven và không tải được npm packages). Xem `TEST_REPORT.md`.

## 1. Khởi chạy

**Yêu cầu:** JDK 21, Maven 3.9+, Node.js 22+, npm; cần mạng Internet ở lần cài dependencies. **Không cần MySQL** cho bản local.

**Windows:** Nhấp đúp `start-dev.bat` ở thư mục gốc. Nếu muốn tự chạy:

```powershell
# Terminal 1, đứng trong easyhome/backend
mvn spring-boot:run

# Terminal 2, đứng trong easyhome/frontend
npm install
npm run dev
```

| Địa chỉ | Dịch vụ |
|---|---|
| http://localhost:5173 | Web React |
| http://localhost:8081/api/listings | REST API |
| `backend/data/easyhome.mv.db` | File H2 tạo khi backend chạy |

Backend khởi tạo 6 tin demo lần đầu trên CSDL mới. Dữ liệu H2 được giữ lại sau khi tắt. Không chạy project EasyHome cũ song song ở các cổng này.

**Docker (MySQL):** Cài Docker Desktop, từ thư mục gốc:

```powershell
copy .env.example .env
# Mở .env và đổi JWT_SECRET, MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD
docker compose up --build -d
```

Mở **http://localhost:3000**. API ở 8081, database nằm trong volume Docker (không mở cổng MySQL ra host). Dừng bằng `docker compose down` (không dùng `-v` nếu muốn giữ dữ liệu). Docker cần Internet để tải image/dependencies.

## 2. Tài khoản demo — CHỈ LOCAL

| Vai trò | Tài khoản | Mật khẩu |
|---|---|---|
| Chủ nhà (USER) | `owner@easyhome.vn` | `Owner@12345` |
| Người tìm thuê (USER) | `user@easyhome.vn` | `User@123456` |
| Kiểm duyệt (MODERATOR) | `mod@easyhome.vn` | `Mod@123456` |
| Quản trị (ADMIN) | `admin@easyhome.vn` | `Admin@12345` |

Không dùng dữ liệu và thông tin demo trong môi trường công khai. Trong `.env` production phải đặt `APP_SEED_DEMO=false`, `APP_MOCK_PAYMENTS=false` và tạo tài khoản quản trị bằng quy trình an toàn.

## 3. Cấu trúc gọn

```text
easyhome/
├── README.md
├── PROGRESS.md            # Nhật ký, việc đã làm và còn thiếu
├── TEST_REPORT.md         # Bằng chứng và giới hạn kiểm thử
├── start-dev.bat
├── docker-compose.yml
├── backend/               # Spring Boot, Java 21; pom.xml
├── frontend/              # React/Vite, CSS; package.json
├── scripts/               # Kiểm tra nghiệp vụ Java
└── verification/          # 177 kiểm tra nghiệp vụ không phụ thuộc framework
```

Java đặt theo quy ước Maven tại `backend/src/main/java/com/easyhome` nhưng không chia thêm tầng thư mục con. Frontend chỉ có `src/App.jsx`, `src/pages.jsx`, `src/api.js`, `src/style.css`.

## 4. Các chức năng đã viết vào source

**Khách:** Trang chủ có bộ lọc và sắp xếp, phân trang, xem chi tiết và số điện thoại người đăng. Chỉ tin `ACTIVE`, bất động sản `AVAILABLE`, còn thời hạn mới hiển thị.

**Tài khoản:** Đăng ký và đăng nhập chung cho chủ nhà/người tìm thuê; trường `publisherType` phân biệt OWNER/BROKER/AGENCY. Mật khẩu BCrypt, JWT 12 giờ; không cho người dùng tự đăng ký ADMIN/MODERATOR.

**Người đăng:** Tạo/sửa nháp → gửi kiểm duyệt → chờ Admin/Moderator chấp thuận → chọn gói → thanh toán mô phỏng/miễn phí → xuất bản → tạm ẩn/mở lại/đóng/đánh dấu đã thuê. Hết hạn có thể đăng lại từ tin cũ tạo một bản nháp mới.

**Gói dịch vụ giả định:** FREE 7 ngày cho lần đầu; STANDARD 29.000đ/15 ngày; VIP 99.000đ/15 ngày. Đây **không phải giá thị trường**; sửa trong `Rules.java` sau khi chốt nghiệp vụ.

**Thanh toán:** Sinh đơn có `reference`, giữ bản chụp giá/thời hạn/độ ưu tiên khi tạo đơn. Với gói trả phí, API demo xác nhận thanh toán bằng tay; xử lý xác nhận lặp an toàn và không tự gia hạn. **Không thu tiền thật.**

**Người tìm thuê:** Lưu tin yêu thích, liên hệ người đăng (1 tin/người/24 giờ để giảm spam), báo cáo tin vi phạm. Không có chức năng đặt cọc/đặt thuê trên nền tảng.

**Moderator/Admin:** Duyệt hoặc từ chối kèm lý do, xem và xử lý báo cáo; Admin xác minh và khóa tài khoản. Lịch sử thao tác quan trọng được lưu ở `audit_logs`.

## 5. Luồng kiểm thử bằng giao diện (từng bước)

1. Mở `http://localhost:5173`, thử tìm theo `Hà Nội`, thay giá và loại hình. Chọn một tin để xem chi tiết.
2. Đăng nhập `owner@easyhome.vn` → **Quản lý tin** → **Đăng tin mới** → nhập đầy đủ tiêu đề, mô tả ≥30 ký tự, địa chỉ, giá thuê, diện tích và URL ảnh. **Lưu bản nháp**.
3. Ở **Quản lý tin** chọn **Gửi duyệt**. Tin chuyển sang `PENDING_REVIEW` và **chưa** xuất hiện ở tìm kiếm công khai.
4. Đăng xuất → đăng nhập `mod@easyhome.vn` → **Kiểm duyệt** → **Duyệt** tin. Tin chuyển sang `AWAITING_PAYMENT`.
5. Đăng nhập lại chủ nhà → **Quản lý tin** → **Chọn gói & thanh toán**. Chọn `STANDARD` hoặc `VIP` → **Tạo đơn** → **Mô phỏng thanh toán thành công**. Tin chuyển sang `ACTIVE` và xuất hiện ở trang chủ.
6. Thử xác nhận lại cùng đơn qua tab **Đơn dịch vụ**: đơn không được kích hoạt lặp. Kiểm tra **Tạm ẩn** và **Mở lại** tin.
7. Đăng nhập `user@easyhome.vn`, mở tin → **Lưu tin** và gửi lời nhắn. Người đăng xem mục **Khách quan tâm**.
8. Chủ nhà chọn **Đã cho thuê**; tin phải biến mất khỏi kết quả tìm kiếm. Tin không thể được mở lại bằng thao tác tạm ẩn/hiện.
9. Tạo thêm tin rồi thử gói `FREE` bằng tài khoản chưa dùng ưu đãi. Mỗi tài khoản chỉ được hưởng **một lần**.
10. Thử từ chối tin với lý do và chỉnh sửa bản nháp, sau đó gửi duyệt lại; gửi báo cáo tin giả từ tài khoản khác và xử lý ở Moderator.

Các bước trên là **kịch bản cần xác minh trên máy chạy ứng dụng**, không phải tuyên bố rằng đã qua E2E test. Dùng `TEST_REPORT.md` để ghi kết quả thực tế.

## 6. API chính

| Phương thức | API | Quyền |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Công khai |
| GET | `/api/listings`, `/api/listings/{id}`, `/api/plans` | Công khai |
| GET,POST | `/api/my/listings` | Đăng nhập |
| GET,PUT | `/api/my/listings/{id}` | Chủ tin |
| POST | `/api/my/listings/{id}/submit` | Chủ tin |
| POST | `/api/my/listings/{id}/pause`, `/resume`, `/close`, `/rented`, `/relist` | Chủ tin |
| POST | `/api/orders` | Chủ tin |
| GET | `/api/my/orders` | Đăng nhập |
| POST | `/api/orders/{id}/cancel` | Chủ đơn |
| POST | `/api/dev/orders/{id}/confirm` | Chủ đơn, **chỉ mock** |
| GET,POST,DELETE | `/api/my/favorites`, `/api/my/favorites/{id}` | Đăng nhập |
| POST | `/api/listings/{id}/contact`, `/report` | Đăng nhập |
| GET | `/api/my/leads` | Chủ tin |
| GET,POST | `/api/admin/*` | Moderator/Admin |

Sử dụng header `Authorization: Bearer <token>` cho API đăng nhập. API tìm kiếm hỗ trợ `q`, `city`, `type`, `minPrice`, `maxPrice`, `sort=newest|priceAsc|priceDesc`, `page`, `size` (tối đa 40).

## 7. Kiểm thử và CI

```bash
bash scripts/test-core.sh       # javac + 177 kiểm tra Rules.java, không cần Maven
cd backend && mvn test          # JUnit/MockMvc: test tích hợp (cần Internet để lấy deps lần đầu)
cd ../frontend && npm install && npm run build   # Production build React
```

Khi đẩy repository lên GitHub, `.github/workflows/ci.yml` tự chạy bài test backend và build frontend. Nếu CI FAIL, **không coi bản đó đã phát hành**; sửa và chạy lại toàn bộ trước khi merge.

## 8. Giới hạn quan trọng trước triển khai thật

- **Chưa tích hợp cổng thanh toán thực**, webhook, chữ ký giao dịch, đối soát ngân hàng, hóa đơn, chính sách hoàn phí. `APP_MOCK_PAYMENTS=true` phải tắt trên production, nhưng khi tắt thì cần tích hợp thanh toán thật trước khi bán gói.
- Dữ liệu local dùng `ddl-auto=update`; production cần migration Flyway/Liquibase, sao lưu/phục hồi, HTTPS, giám sát và quản lý secrets.
- Chưa có upload ảnh nhiều tệp/CDN, xác thực điện thoại/OTP/KYC tự động, quên mật khẩu/email, chống spam/rate-limit cấp gateway, hệ thống chống tin trùng mạnh, lịch sử sửa phiên bản tin, SEO động/SSR.
- Không có kết quả test tích hợp HTTP/DB thực tế trong môi trường tạo mã. Cần chạy các bước 1–10 trên khi có Maven, npm, Docker. Xem `PROGRESS.md`.
- UI dùng ảnh Unsplash cho tin demo; ảnh thực phải có quyền sử dụng và kiểm tra địa chỉ cung cấp bởi chủ nhà.
- **Không sử dụng trực tiếp với người dùng thật trước khi kiểm thử bảo mật, dữ liệu, chính sách và nghĩa vụ pháp lý.**

## 9. Khi tạo GitHub repository mới

1. Tạo repository **mới, trống** tên `easyhome` (không tick "Add a README" nếu muốn push nguyên dự án).
2. Giải nén ZIP, mở terminal tại thư mục `easyhome` (nơi có README.md):

```bash
git init
git add .
git commit -m "feat: initial EasyHome marketplace MVP"
git branch -M main
git remote add origin https://github.com/<TEN_TAI_KHOAN>/easyhome.git
git push -u origin main
```

Không commit `.env`, dữ liệu DB, mật khẩu ngoài demo. Nếu chạy CI, kiểm tra tab Actions để thấy kết quả `mvn test` và `npm run build` trên môi trường có Internet.
