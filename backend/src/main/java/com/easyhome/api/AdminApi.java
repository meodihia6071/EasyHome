package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/admin")
public class AdminApi {
  private final Listings listings;private final Reports reports;private final Users users;private final Identity identity;private final Audits audits;
  private final ModerationService moderation;
  AdminApi(Listings l,Reports r,Users u,Identity i,Audits a,ModerationService m){listings=l;reports=r;users=u;identity=i;audits=a;moderation=m;}
  public record Review(String note){}
  public record ReportView(Long id,Long listingId,String listingTitle,String reporter,String reason,String status){
    static ReportView from(Report r){return new ReportView(r.id,r.listing.getId(),r.listing.getTitle(),r.reporter.getEmail(),r.reason,r.status.name());}
  }
  public record UserView(Long id,String name,String email,String role,boolean verified,boolean locked,Instant lockedUntil,String lockReason){
    static UserView from(User u){return new UserView(u.id,u.fullName,u.email,u.role.name(),u.isVerified(),u.locked,u.lockedUntil,u.lockReason);}
  }
  @GetMapping("/pending") @Transactional(readOnly=true)
  public List<ListingApi.View> pending(@AuthenticationPrincipal Jwt jwt){identity.staff(jwt);return listings.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING_REVIEW).stream().map(ListingApi.View::from).toList();}
  @PostMapping("/listings/{id}/approve") @Transactional
  public ListingApi.View approve(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return ListingApi.View.from(moderation.approve(id,identity.staff(jwt)));}
  @PostMapping("/listings/{id}/reject") @Transactional
  public ListingApi.View reject(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Review input){
    return ListingApi.View.from(moderation.reject(id,identity.staff(jwt),input.note()));
  }
  @GetMapping("/reports") @Transactional(readOnly=true)
  public List<ReportView> reports(@AuthenticationPrincipal Jwt jwt){identity.staff(jwt);return reports.findByStatusOrderByCreatedAtAsc(ReportStatus.OPEN).stream().map(ReportView::from).toList();}
  @PostMapping("/reports/{id}/dismiss") @Transactional
  public ReportView dismiss(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User staff=identity.staff(jwt);Report r=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy báo cáo"));Rules.check(r.status==ReportStatus.OPEN,"Đã xử lý báo cáo");r.status=ReportStatus.DISMISSED;audits.save(new Audit(staff.id,"DISMISS_REPORT","report:"+r.id));return ReportView.from(r);}
  @PostMapping("/reports/{id}/remove") @Transactional
  public ReportView remove(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User staff=identity.staff(jwt);Report r=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy báo cáo"));
    Rules.check(r.status==ReportStatus.OPEN,"Đã xử lý báo cáo");
    Listing l=listings.locked(r.listing.getId()).orElseThrow();
    l.status=ListingStatus.REMOVED;r.status=ReportStatus.RESOLVED;
    audits.save(new Audit(staff.id,"REMOVE_REPORTED_LISTING","listing:"+l.id));return ReportView.from(r);
  }
  @GetMapping("/users") @Transactional(readOnly=true)
  public List<UserView> users(@AuthenticationPrincipal Jwt jwt){identity.admin(jwt);return users.findAll().stream().map(UserView::from).toList();}
}
