package com.easyhome;
import com.easyhome.Rules.*;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BillingApi {
  private final Orders orders;private final Listings listings;private final Identity identity;private final Audits audits; private final Users users;
  private final boolean mockPayments;
  BillingApi(Orders o,Listings l,Identity i,Audits a,Users users,@Value("${app.mock-payments}") boolean mockPayments){
    orders=o;listings=l;identity=i;audits=a;this.users=users;this.mockPayments=mockPayments;
  }
  public record PlanView(String code,String name,int price,int days,int priority) {
    static PlanView from(ListingPlan p){return new PlanView(p.name(),p.label,p.price,p.days,p.priority);}
  }
  public record OrderInput(Long listingId,ListingPlan plan){}
  public record OrderView(Long id,Long listingId,String reference,String plan,int amount,int days,String status,Instant paidAt,Instant createdAt){
    static OrderView from(ServiceOrder o){return new OrderView(o.id,o.listing.getId(),o.reference,o.plan.name(),o.priceSnapshot,o.durationDaysSnapshot,o.status.name(),o.paidAt,o.createdAt);}
  }
  @GetMapping("/api/plans") public List<PlanView> plans(){return Arrays.stream(ListingPlan.values()).map(PlanView::from).toList();}
  @GetMapping("/api/my/orders") @Transactional(readOnly=true)
  public List<OrderView> mine(@AuthenticationPrincipal Jwt jwt){return orders.findByBuyer_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(OrderView::from).toList();}
  @PostMapping("/api/orders") @Transactional
  public OrderView checkout(@AuthenticationPrincipal Jwt jwt,@RequestBody OrderInput request){
    User u=identity.current(jwt);
    Rules.check(request!=null && request.listingId()!=null && request.plan()!=null,"Cần chọn tin đăng và gói dịch vụ");
    Listing l=listings.locked(request.listingId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin"));
    if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn không sở hữu tin đăng này");
    Rules.checkout(l.getStatus());
    // Lock this account before its first consistent read, avoiding double free orders under MySQL REPEATABLE READ.
    if(request.plan()==ListingPlan.FREE)users.locked(u.id).orElseThrow();
    Optional<ServiceOrder> existing=orders.findFirstByListing_IdAndStatusOrderByIdDesc(l.id,PaymentStatus.PENDING);
    if(existing.isPresent()){
      Rules.check(existing.get().plan==request.plan(),"Đơn cũ đang chờ thanh toán; hãy hủy đơn trước khi đổi gói");
      return OrderView.from(existing.get());
    }
    ListingPlan plan=request.plan();
    if(plan==ListingPlan.FREE){
      Rules.check(!orders.existsByBuyer_IdAndPlanAndStatus(u.id,ListingPlan.FREE,PaymentStatus.PAID),"Tài khoản đã dùng lượt đăng miễn phí");
    }
    ServiceOrder o=new ServiceOrder();o.listing=l;o.buyer=u;o.plan=plan;o.priceSnapshot=plan.price;
    o.durationDaysSnapshot=plan.days;o.prioritySnapshot=plan.priority;o.reference="EASY-"+UUID.randomUUID().toString().replace("-","").substring(0,18).toUpperCase(Locale.ROOT);
    if(plan==ListingPlan.FREE){o.status=PaymentStatus.PAID;o.paidAt=Instant.now();activate(l,o);}
    orders.save(o);audits.save(new Audit(u.id,"CHECKOUT","order:"+o.id));return OrderView.from(o);
  }
  @PostMapping("/api/orders/{id}/cancel") @Transactional
  public OrderView cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);ServiceOrder o=orders.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đơn"));
    if(!o.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền thao tác");
    Rules.check(o.status==PaymentStatus.PENDING,"Chỉ đơn đang chờ mới được hủy");o.status=PaymentStatus.CANCELLED;return OrderView.from(o);
  }
  /** Local/sandbox simulation only. Must be disabled in any public deployment. */
  @PostMapping("/api/dev/orders/{id}/confirm") @Transactional
  public OrderView confirmMock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    if(!mockPayments)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Thanh toán mô phỏng đã tắt");
    User u=identity.current(jwt);
    ServiceOrder o=orders.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đơn"));
    if(!o.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền thanh toán đơn này");
    if(o.status==PaymentStatus.PAID)return OrderView.from(o); // replay-safe
    Rules.check(o.status==PaymentStatus.PENDING,"Đơn đã bị hủy");
    Listing l=listings.locked(o.listing.getId()).orElseThrow();
    Rules.checkout(l.getStatus());
    o.paidAt=Instant.now();o.status=PaymentStatus.PAID;activate(l,o);
    audits.save(new Audit(u.id,"PAYMENT_SIMULATED","order:"+o.id));return OrderView.from(o);
  }
  private void activate(Listing l,ServiceOrder o){
    l.activatePackage(o.plan,o.prioritySnapshot,o.paidAt,o.durationDaysSnapshot);
  }
}
