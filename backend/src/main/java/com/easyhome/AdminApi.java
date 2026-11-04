package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/admin")
public class AdminApi {
  private final Listings listings;private final Reports reports;private final Users users;private final Identity identity;private final Audits audits;
  AdminApi(Listings l,Reports r,Users u,Identity i,Audits a){listings=l;reports=r;users=u;identity=i;audits=a;}
  public record Review(String note){}
  public record ReportView(Long id,Long listingId,String listingTitle,String reporter,String reason,String status){
    static ReportView from(Report r){return new ReportView(r.id,r.listing.id,r.listing.title,r.reporter.email,r.reason,r.status.name());}
  }
  public record UserView(Long id,String name,String email,String role,boolean verified,boolean locked){
    static UserView from(User u){return new UserView(u.id,u.fullName,u.email,u.role.name(),u.verified,u.locked);}
  }
  @GetMapping("/pending") @Transactional(readOnly=true)
  public List<ListingApi.View> pending(@AuthenticationPrincipal Jwt jwt){identity.staff(jwt);return listings.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING_REVIEW).stream().map(ListingApi.View::from).toList();}
  @PostMapping("/listings/{id}/approve") @Transactional
  public ListingApi.View approve(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User staff=identity.staff(jwt);Listing l=listings.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tin không tồn tại"));
    Rules.content(l.title,l.description,l.property.city,l.property.district,l.property.address,l.property.type,l.property.monthlyRent,l.property.area,l.property.bedrooms,l.property.imageUrl);
    Rules.check(l.property.availability==Availability.AVAILABLE,"Nhà không sẵn sàng cho thuê");
    l.status=Rules.review(l.status,true);l.reviewNote=null;audits.save(new Audit(staff.id,"APPROVE","listing:"+l.id));return ListingApi.View.from(l);
  }
  @PostMapping("/listings/{id}/reject") @Transactional
  public ListingApi.View reject(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Review input){
    User staff=identity.staff(jwt);Rules.check(input!=null && input.note()!=null && input.note().trim().length()>=10 && input.note().length()<=500,"Cần nêu lý do từ chối (10–500 ký tự)");
    Listing l=listings.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tin không tồn tại"));
    l.status=Rules.review(l.status,false);l.reviewNote=input.note().trim();audits.save(new Audit(staff.id,"REJECT","listing:"+l.id));return ListingApi.View.from(l);
  }
  @GetMapping("/reports") @Transactional(readOnly=true)
  public List<ReportView> reports(@AuthenticationPrincipal Jwt jwt){identity.staff(jwt);return reports.findByStatusOrderByCreatedAtAsc(ReportStatus.OPEN).stream().map(ReportView::from).toList();}
  @PostMapping("/reports/{id}/dismiss") @Transactional
  public ReportView dismiss(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User staff=identity.staff(jwt);Report r=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy báo cáo"));Rules.check(r.status==ReportStatus.OPEN,"Đã xử lý báo cáo");r.status=ReportStatus.DISMISSED;audits.save(new Audit(staff.id,"DISMISS_REPORT","report:"+r.id));return ReportView.from(r);}
  @PostMapping("/reports/{id}/remove") @Transactional
  public ReportView remove(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User staff=identity.staff(jwt);Report r=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy báo cáo"));
    Rules.check(r.status==ReportStatus.OPEN,"Đã xử lý báo cáo");
    Listing l=listings.locked(r.listing.id).orElseThrow();
    l.status=ListingStatus.REMOVED;r.status=ReportStatus.RESOLVED;
    audits.save(new Audit(staff.id,"REMOVE_REPORTED_LISTING","listing:"+l.id));return ReportView.from(r);
  }
  @GetMapping("/users") @Transactional(readOnly=true)
  public List<UserView> users(@AuthenticationPrincipal Jwt jwt){identity.admin(jwt);return users.findAll().stream().map(UserView::from).toList();}
  @PostMapping("/users/{id}/verify") @Transactional
  public UserView verify(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User admin=identity.admin(jwt);User u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy người dùng"));u.verified=true;audits.save(new Audit(admin.id,"VERIFY_USER","user:"+u.id));return UserView.from(u);}
  @PostMapping("/users/{id}/lock") @Transactional
  public UserView lock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User a=identity.admin(jwt);Rules.check(!a.id.equals(id),"Không tự khóa tài khoản quản trị của mình");User u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy người dùng"));u.locked=true;audits.save(new Audit(a.id,"LOCK_USER","user:"+u.id));return UserView.from(u);}
}
