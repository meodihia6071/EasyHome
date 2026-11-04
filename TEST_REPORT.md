# TEST REPORT — EasyHome (04/11/2026)

**Môi trường kiểm thử đã sử dụng:** Linux container, OpenJDK 21.0.12, Node 22.16.0, TypeScript parser cài sẵn ở môi trường tạo code. Không có `mvn`, không tải được Maven/npm registry, không có Docker CLI.

## Kết quả đã chạy thật

| ID | Phạm vi | Lệnh / phương thức | Kết quả |
|---|---|---|---|
| T-001 | Business rules Java | `bash scripts/test-core.sh` (`javac -Xlint:all -Werror`) | **PASS 177/177** |
| T-002 | JSX/JS syntax `App.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-003 | JSX/JS syntax `pages.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-004 | JSX/JS syntax `main.jsx` | TypeScript transpile parser | **PASS syntax** |
| T-005 | JS syntax `api.js` | TypeScript transpile parser | **PASS syntax** |
| T-006 | JS syntax `vite.config.js` | TypeScript transpile parser | **PASS syntax** |

177 kiểm tra domain bao gồm: validation nội dung (trống, vượt giới hạn, giá, diện tích NaN/vô cực, ảnh), chuyển trạng thái cho tất cả enum, review duyệt/từ chối, checkout, sửa nháp, tạm ẩn/mở lại, quá hạn chính xác theo thời điểm, bất động sản đã thuê, thời hạn gói, cấp ưu tiên. **Đây là kiểm tra `Rules.java`, không phải 177 test toàn API.**

## Test tích hợp đã viết nhưng CHƯA chạy được

File `backend/src/test/java/com/easyhome/MarketplaceFlowTest.java` gồm:

- `paidListingRequiresReviewAndPaymentAndReplayIsSafe`: tạo tin → chờ duyệt → duyệt → đơn VIP → mock confirm → thử lặp → public hiển thị.
- `freeBenefitLimitedAndRelistAfterClose`: FREE một lần → chặn lần hai → đóng → đăng lại bản nháp.
- `crossAccountOperationsBlockedAndInvalidListingRejected`: không cho tài khoản khác submit tin, loại dữ liệu không hợp lệ, chặn thao tác chưa đăng nhập.

**Trạng thái:** `PENDING (chưa có kết quả PASS/FAIL)`. Cần chạy `cd backend && mvn test` trên GitHub Actions hoặc máy local có Maven. Nếu test lỗi phải sửa, chạy lại; không tuyên bố release.

## Các lượt test bắt buộc trước khi dùng thật

| TC | Kịch bản | Kỳ vọng | Trạng thái |
|---|---|---|---|
| E2E-01 | Khách xem và lọc tin | Chỉ ACTIVE + AVAILABLE + chưa hết hạn | Chưa chạy |
| E2E-02 | Tạo tài khoản / đăng nhập / token hết hạn | Quyền hoạt động, trả 401 phù hợp | Chưa chạy |
| E2E-03 | USER tạo nháp và sửa | Chỉ chủ tin sửa được | Chưa chạy |
| E2E-04 | Tin gửi duyệt | Không công khai trước khi duyệt | Chưa chạy |
| E2E-05 | Moderator từ chối + sửa gửi lại | Lý do lưu, trạng thái đúng | Chưa chạy |
| E2E-06 | Gói STANDARD/VIP | Đúng giá snapshot, đúng thời hạn | Chưa chạy |
| E2E-07 | Callback thanh toán lặp | Không cộng thời hạn / tạo dịch vụ hai lần | Chưa chạy |
| E2E-08 | FREE lần đầu và lần hai | Lần hai từ chối kể cả đồng thời | Chưa chạy |
| E2E-09 | Tạm ẩn/mở lại/hết hạn | Không thể mở sau hết hạn | Chưa chạy |
| E2E-10 | Đã cho thuê | Tin bị đóng, không mở lại | Chưa chạy |
| E2E-11 | Đăng lại tin cũ | Tạo nháp mới cùng Property, không trùng tin ACTIVE | Chưa chạy |
| E2E-12 | Tin yêu thích, liên hệ, báo cáo | Phân quyền + chặn spam | Chưa chạy |
| E2E-13 | Xử lý báo cáo gỡ tin | Tin biến mất công khai, log thao tác | Chưa chạy |
| E2E-14 | Docker MySQL clean/restart | Dữ liệu bền vững, healthcheck đúng | Chưa chạy |
| E2E-15 | Browser mobile/tablet | Layout và thao tác dễ dùng | Chưa chạy |
| SEC-01 | JWT giả mạo, expired, khác user | 401/403 đúng | Chưa chạy |
| SEC-02 | Input độc hại, URL ảnh xấu, XSS | Không vượt kiểm tra và không chèn script | Chưa chạy |
| PERF-01 | 1.000+ tin, nhiều người dùng | Phân trang và phản hồi ổn định | Chưa chạy |

## Cách chạy và ghi kết quả

1. Trên repo mới, mở **Actions** xem hai job `backend` và `frontend`. Trường hợp fail, lưu log lỗi.
2. Chạy `mvn test` backend; nếu cần xem chi tiết: `backend/target/surefire-reports/`.
3. Chạy `npm install && npm run build`; output thành công nằm trong `frontend/dist/`.
4. Khởi động ứng dụng và chạy 10 bước ở README. Với mỗi TC: ghi thời điểm, tài khoản test, dữ liệu vào, kết quả API, ảnh màn hình (nếu có), PASS/FAIL, link issue/commit sửa.
5. Sau mỗi sửa lỗi, chạy lại core tests + integration + E2E trọng yếu, tránh hồi quy.

**Tiêu chí phát hành MVP:** CI xanh, quy trình đăng tin từ đầu đến cuối qua browser không lỗi, phân quyền được kiểm chứng, test đồng thời thanh toán miễn phí, Docker/MySQL dữ liệu bền vững. Trước đó chỉ coi đây là **bản source phát triển**.
