package com.easyhome;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Dependency-free business rules. Controller and service code must invoke these. */
public final class Rules {
  private Rules() {}
  static void check(boolean okay, String message) { if (!okay) throw new BusinessException(message); }

  public static void content(String title, String description, String city, String district, String address,
                             String type, BigDecimal monthlyRent, Double area, Integer bedrooms, String imageUrl) {
    check(title != null && title.trim().length() >= 12 && title.trim().length() <= 160, "Tiêu đề phải có 12–160 ký tự");
    check(description != null && description.trim().length() >= 30 && description.trim().length() <= 3000, "Mô tả phải có 30–3000 ký tự");
    check(city != null && !city.isBlank() && city.length() <= 100, "Thiếu tỉnh/thành phố");
    check(district != null && !district.isBlank() && district.length() <= 100, "Thiếu quận/huyện");
    check(address != null && !address.isBlank() && address.length() <= 240, "Thiếu địa chỉ bất động sản");
    check(type != null && (type.equals("APARTMENT") || type.equals("ROOM") || type.equals("HOUSE") || type.equals("STUDIO")), "Loại nhà không hợp lệ");
    check(monthlyRent != null && monthlyRent.compareTo(new BigDecimal("100000")) >= 0 && monthlyRent.compareTo(new BigDecimal("1000000000")) <= 0, "Giá thuê không hợp lệ");
    check(area != null && Double.isFinite(area) && area >= 5 && area <= 10000, "Diện tích không hợp lệ");
    check(bedrooms != null && bedrooms >= 0 && bedrooms <= 30, "Số phòng ngủ không hợp lệ");
    check(imageUrl != null && imageUrl.length() <= 1000 && (imageUrl.startsWith("https://") || imageUrl.startsWith("http://")), "Cần URL ảnh bắt đầu bằng https:// hoặc http://");
  }
  public static ListingStatus submit(ListingStatus status) {
    check(status == ListingStatus.DRAFT || status == ListingStatus.REJECTED, "Chỉ bản nháp hoặc tin bị từ chối mới được gửi duyệt");
    return ListingStatus.PENDING_REVIEW;
  }
  public static ListingStatus review(ListingStatus status, boolean approved) {
    check(status == ListingStatus.PENDING_REVIEW, "Tin phải đang chờ kiểm duyệt");
    return approved ? ListingStatus.AWAITING_PAYMENT : ListingStatus.REJECTED;
  }
  public static void checkout(ListingStatus status) {
    check(status == ListingStatus.AWAITING_PAYMENT, "Tin chưa đủ điều kiện mua gói");
  }
  public static void edit(ListingStatus status) {
    check(status == ListingStatus.DRAFT || status == ListingStatus.REJECTED, "Chỉ sửa bản nháp hoặc tin bị từ chối; tin đang duyệt/hiển thị phải tạo phiên bản mới");
  }
  public static ListingStatus activate(ListingStatus status, Availability availability) {
    checkout(status);
    check(availability == Availability.AVAILABLE, "Bất động sản không còn cho thuê");
    return ListingStatus.ACTIVE;
  }
  public static ListingStatus pause(ListingStatus status) {
    check(status == ListingStatus.ACTIVE, "Chỉ tin đang hiển thị mới được tạm ẩn");
    return ListingStatus.PAUSED;
  }
  public static ListingStatus resume(ListingStatus status, Availability availability, Instant expiresAt, Instant now) {
    check(status == ListingStatus.PAUSED, "Chỉ tin tạm ẩn mới được mở lại");
    check(availability == Availability.AVAILABLE, "Nhà không còn cho thuê");
    check(expiresAt != null && expiresAt.isAfter(now), "Tin đã hết hạn, cần gia hạn bằng dịch vụ mới");
    return ListingStatus.ACTIVE;
  }
  public static ListingStatus close(ListingStatus status) {
    check(status == ListingStatus.ACTIVE || status == ListingStatus.PAUSED || status == ListingStatus.EXPIRED,
      "Chỉ có thể đóng tin đã từng xuất bản");
    return ListingStatus.CLOSED;
  }
  public static boolean visible(ListingStatus status, Availability availability, Instant expiresAt, Instant now) {
    return status == ListingStatus.ACTIVE && availability == Availability.AVAILABLE && expiresAt != null && expiresAt.isAfter(now);
  }
  public static Instant expiry(Instant paidAt, ListingPlan plan) {
    check(paidAt != null && plan != null, "Thiếu thời điểm hoặc gói đăng tin");
    return paidAt.plus(plan.days, ChronoUnit.DAYS);
  }

  enum ListingStatus { DRAFT, PENDING_REVIEW, REJECTED, AWAITING_PAYMENT, ACTIVE, PAUSED, EXPIRED, CLOSED, REMOVED }
enum Availability { AVAILABLE, RENTED, UNAVAILABLE }
enum AccountRole { USER, MODERATOR, ADMIN }
enum PublisherType { OWNER, BROKER, AGENCY }
enum PaymentStatus { PENDING, PAID, CANCELLED }
enum ReportStatus { OPEN, RESOLVED, DISMISSED }
enum ListingPlan {
  FREE("Miễn phí lần đầu", 0, 7, 0),
  STANDARD("Tin thường", 29000, 15, 1),
  VIP("Tin VIP", 99000, 15, 2);
  final String label; final int price, days, priority;
  ListingPlan(String label, int price, int days, int priority) { this.label=label;this.price=price;this.days=days;this.priority=priority; }
}
static class BusinessException extends RuntimeException {
  private static final long serialVersionUID=1L;
  BusinessException(String message) { super(message); }
}

}
