package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BillingApi {
  private final Orders orders;private final Listings listings;private final Identity identity;private final Audits audits;
  private final Users users;private final WalletService wallet;private final Payments payments;private final PlanCatalog catalog;
  private final boolean mockPayments; private final Environment env;
  BillingApi(Orders o,Listings l,Identity i,Audits a,Users users,WalletService wallet,Payments payments,PlanCatalog catalog,
    @Value("${app.mock-payments:false}") boolean mockPayments,Environment env){
    orders=o;listings=l;identity=i;audits=a;this.users=users;this.wallet=wallet;this.payments=payments;
    this.mockPayments=mockPayments;this.env=env;this.catalog=catalog;
  }
  public record PlanView(String code,String name,int price,int days,int priority){
    static PlanView from(ListingPlan p){return new PlanView(p.name(),p.label,p.price,p.days,p.priority);}
  }
  public record OrderInput(Long listingId,ListingPlan plan){}
  public record OrderView(Long id,Long listingId,String reference,String plan,int amount,int days,String status,String method,
    Instant paidAt,Instant refundedAt,Instant createdAt){
    static OrderView from(ServiceOrder o){return new OrderView(o.id,o.listing.getId(),o.reference,o.plan.name(),o.priceSnapshot,o.durationDaysSnapshot,o.status.name(),o.paymentMethod,o.paidAt,o.refundedAt,o.createdAt);}
  }
  @GetMapping("/api/plans") public List<PlanView> plans(){return catalog.available();}
  @GetMapping("/api/my/orders") @Transactional(readOnly=true)
  public List<OrderView> mine(@AuthenticationPrincipal Jwt jwt){return orders.findByBuyer_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(OrderView::from).toList();}
  @PostMapping("/api/orders") @Transactional
  public OrderView checkout(@AuthenticationPrincipal Jwt jwt,@RequestBody OrderInput request){
    User u=identity.current(jwt);RulesV2.require(u.isVerified(),"Cần xác thực CCCD trước khi mua gói đăng tin");
    RulesV2.require(request!=null&&request.listingId()!=null&&request.plan()!=null,"Cần chọn tin và gói dịch vụ");
    RulesV2.require(request.plan()!=ListingPlan.VIP,"Không còn bán gói VIP cũ");
    Listing l=listings.locked(request.listingId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Rules.checkout(l.getStatus());
    if(request.plan()==ListingPlan.FREE){
      users.locked(u.id).orElseThrow();
      RulesV2.require(!orders.existsByBuyer_IdAndPlanAndStatus(u.id,ListingPlan.FREE,PaymentStatus.PAID),"Bạn đã sử dụng hoặc đang giữ lượt FREE");
    }
    ServicePlan selected=catalog.get(request.plan());
    ServiceOrder o=new ServiceOrder();o.listing=l;o.buyer=u;o.plan=request.plan();o.priceSnapshot=selected.price;
    o.durationDaysSnapshot=selected.days;o.prioritySnapshot=selected.priority;
    o.reference="EASY-"+UUID.randomUUID().toString().replace("-","").substring(0,18).toUpperCase(Locale.ROOT);
    if(o.plan==ListingPlan.FREE){o.status=PaymentStatus.PAID;o.paidAt=Instant.now();o.paymentMethod="FREE";}
    orders.save(o);l.setStatus(ListingStatus.AWAITING_PAYMENT);
    // The payment endpoint must read the updated listing state in a separate HTTP request.
    listings.saveAndFlush(l);
    audits.save(new Audit(u.id,"ORDER_CREATED","order:"+o.id));return OrderView.from(o);
  }
  @PostMapping("/api/orders/{id}/cancel") @Transactional
  public OrderView cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);ServiceOrder o=orders.findById(id).orElseThrow();
    if(!o.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Listing l=listings.locked(o.listing.getId()).orElseThrow();o=orders.locked(id).orElseThrow();
    RulesV2.require(o.status==PaymentStatus.PENDING,"Chỉ hủy đơn chưa thanh toán");
    o.status=PaymentStatus.CANCELLED;l.setStatus(ListingStatus.DRAFT);orders.saveAndFlush(o);listings.saveAndFlush(l);return OrderView.from(o);
  }
  @PostMapping("/api/orders/{id}/wallet-pay") @Transactional
  public OrderView payWallet(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    requireDemo();User u=identity.current(jwt);ServiceOrder snapshot=orders.findById(id).orElseThrow();
    if(!snapshot.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Listing l=listings.locked(snapshot.listing.getId()).orElseThrow();ServiceOrder o=orders.locked(id).orElseThrow();
    if(o.status==PaymentStatus.PAID)return OrderView.from(o);
    RulesV2.require(o.status==PaymentStatus.PENDING&&l.getStatus()==ListingStatus.AWAITING_PAYMENT,"Đơn không được thanh toán");
    WalletAccount w=wallet.locked(u);wallet.move(w,-o.priceSnapshot,"BUY_LISTING","purchase:order:"+o.id);
    markPaid(o,"DEMO_WALLET");
    PaymentTransaction t=new PaymentTransaction();t.order=o;t.payer=u;t.amount=o.priceSnapshot;
    t.method="DEMO_WALLET";t.status="PAID";t.confirmedAt=Instant.now();t.reference="wallet:order:"+o.id;payments.save(t);
    return OrderView.from(o);
  }
  @PostMapping("/api/dev/orders/{id}/confirm") @Transactional
  public OrderView confirmMock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    requireDemo();User u=identity.current(jwt);ServiceOrder snapshot=orders.findById(id).orElseThrow();
    if(!snapshot.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Listing l=listings.locked(snapshot.listing.getId()).orElseThrow();ServiceOrder o=orders.locked(id).orElseThrow();
    if(o.status==PaymentStatus.PAID)return OrderView.from(o);
    RulesV2.require(o.status==PaymentStatus.PENDING&&l.getStatus()==ListingStatus.AWAITING_PAYMENT,"Đơn không được thanh toán");
    markPaid(o,"DEMO_QR");
    PaymentTransaction t=new PaymentTransaction();t.order=o;t.payer=u;t.amount=o.priceSnapshot;
    t.method="DEMO_QR";t.status="PAID";t.confirmedAt=Instant.now();t.reference="mock:order:"+o.id;payments.save(t);
    return OrderView.from(o);
  }
  @PostMapping("/api/dev/orders/{id}/fail") @Transactional
  public OrderView failMock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    requireDemo();User u=identity.current(jwt);ServiceOrder o=orders.findById(id).orElseThrow();
    if(!o.buyer.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    RulesV2.require(o.status==PaymentStatus.PENDING,"Đơn đã được xử lý");
    Listing l=listings.locked(o.listing.getId()).orElseThrow();o.status=PaymentStatus.FAILED;l.setStatus(ListingStatus.DRAFT);orders.saveAndFlush(o);listings.saveAndFlush(l);return OrderView.from(o);
  }
  private void markPaid(ServiceOrder o,String method){
    o.status=PaymentStatus.PAID;o.paidAt=Instant.now();o.paymentMethod=method;
    orders.saveAndFlush(o);
    audits.save(new Audit(o.buyer.getId(),"PAYMENT_"+method,"order:"+o.id));
  }
  public void refund(ServiceOrder o){
    if(o.status==PaymentStatus.REFUNDED)return;
    RulesV2.require(o.status==PaymentStatus.PAID,"Chưa thanh toán, không được hoàn tiền");
    if(o.priceSnapshot>0)wallet.refund(o.buyer,o.priceSnapshot,o.id);
    o.status=PaymentStatus.REFUNDED;o.refundedAt=Instant.now();orders.saveAndFlush(o);
    audits.save(new Audit(o.buyer.getId(),"REFUND_REJECTION","order:"+o.id));
  }
  public ServiceOrder paidOrder(Listing l){return orders.findFirstByListing_IdAndStatusOrderByIdDesc(l.id,PaymentStatus.PAID)
    .orElseThrow(()->new Rules.BusinessException("Tin chưa có đơn đã thanh toán"));}
  private void requireDemo(){
    if(!mockPayments||env.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Mock chỉ dùng ở dev/test");
  }
}
