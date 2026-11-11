# EasyHome V2 – Kiến trúc source sau khi tách thư mục

Nguồn: `EasyHome_V2_Phase1_DB_AutoInit.zip` đã bàn giao trước đó. Lượt này **chỉ tổ chức lại source/package/import** để dễ bảo trì, không thay đổi quy tắc thương mại, API URL, schema database hay cấu hình XAMPP. File `start-dev.bat` và script tạo DB vẫn giữ nguyên.

## Backend – Spring Boot (Java 21)

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/easyhome/
    │   │   ├── EasyHomeApplication.java   # Lớp khởi động, scan toàn bộ package con
    │   │   ├── api/                   # REST controllers, DTO gắn với API
    │   │   ├── service/               # Xử lý nghiệp vụ, xác thực, ví, kiểm duyệt, lưu ảnh
    │   │   ├── model/                 # JPA entities, quan hệ dữ liệu
    │   │   ├── repository/            # Spring Data repositories, 1 interface / file
    │   │   ├── common/                # Business rules, lỗi dùng chung
    │   │   ├── config/                # Spring Security, seed data
    │   │   └── job/                   # Scheduler: hết hạn tin, mở khóa, cleanup
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       └── db/migration/         # Flyway
    └── test/java/com/easyhome/       # Kiểm thử tích hợp
```

Điểm cần lưu ý:
- `Repos.java` trước đây chứa nhiều interface trong một file. Các interface đã được **tách thành file riêng** trong `repository/`. Giữ lại `Repos.java` như file chỉ dẫn tương thích tên file; không còn khai báo trùng repository.
- `AuthApi.java`, `AccountManagementApi.java`, `MediaApi.java`, `PlanCatalog.java`, `WalletService.java`, `EasyHomeApplication.java` trước đây gộp nhiều top-level class. Các class phụ đã chuyển thành `Identity.java`, `AutoUnlockJob.java`, `PrivateMediaStorage.java`, `PlanAdminApi.java`, `RulesV2.java`, `ExpiryJob.java`, `ReviewDeadlineJob.java`.
- Do code gốc truy cập trực tiếp field JPA giữa Controller/Service/Entity, một số field/constructor được mở `public` để chuyển package mà **không đổi hành vi**. Đây là bước chuyển tiếp; hướng cải tiến lâu dài là encapsulate entity, dùng DTO/service boundary và kiểm tra việc truy cập proxy Hibernate.
- Tên bảng JPA và file migration không bị thay đổi bởi lần tách này.

## Frontend – React/Vite

```text
frontend/src/
├── App.jsx, main.jsx             # Router và entrypoint
├── pages.jsx, phase1.jsx        # Barrel exports giữ tương thích import cũ
├── api.js                       # Barrel exports cho API service
├── style.css                    # Import từ styles/style.css
├── pages/
│   ├── explore/                 # Explore, Detail
│   ├── auth/                    # SignIn, SignUp, VerifyEmail, IdentityCenter
│   ├── listing/                 # MyListings, ListingForm, ListingMediaCenter, Saved, Leads
│   ├── billing/                 # Checkout, Orders, WalletCenter, Phase1Checkout
│   ├── moderation/              # Moderation, ModerationDetail, StaffManagement
│   └── common/                  # NotFound
├── components/
│   ├── shared-core.jsx          # Card, Photo, Spinner, Header, form helpers
│   └── shared-phase1.jsx        # Alert, Block, upload helpers
├── services/api.js             # API client triển khai thật
└── styles/style.css            # CSS của ứng dụng
```

Các file `pages.jsx`, `phase1.jsx`, `api.js`, `style.css` được giữ nguyên tên dưới dạng **barrel/import tương thích**, để phần router và code bên ngoài không bị gãy. Màn hình thực nằm trong `pages/`.

## Chạy local với XAMPP

1. Bật MySQL/MariaDB trong XAMPP.
2. Chạy `start-dev.bat` tại thư mục EasyHome. Script hiện có sẽ cố tạo database local trước khi chạy ứng dụng.
3. Hoặc chạy riêng: `cd backend && mvn clean test && mvn spring-boot:run`, và `cd frontend && npm install && npm run dev`.
4. Dùng cấu hình `dev` theo README_XAMPP.md khi thử OTP console và thanh toán giả.

## Kiểm tra đã thực hiện tại môi trường đóng gói

- `bash scripts/test-core.sh`: **198/198 business rule checks PASS**.
- JDK parser `--release 21`: **67 file Java (66 main + 1 integration test), không lỗi cú pháp**.
- TypeScript parser: **27 file JS/JSX, không lỗi cú pháp**.
- Kiểm tra đường dẫn import JS tương đối: **không thiếu file import**.

**Chưa xác minh được:** `mvn clean test`, `npm run build`, khởi động ứng dụng và truy vấn MariaDB qua XAMPP trong môi trường đóng gói, vì không có Maven và không có mạng tải dependencies. Không coi bản này đã sẵn sàng production cho đến khi chạy nghiệm thu thực tế.

> Tạo thư mục thử nghiệm mới, sao lưu source và database trước khi thay thế. Không chép `target/`, `node_modules/`, `.env` có mật khẩu hoặc thư mục `private-uploads` lên Git.
