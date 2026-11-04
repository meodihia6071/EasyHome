package com.easyhome;
import com.easyhome.Rules.*;

import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ExtrasApi {
  private final Listings listings;private final Identity identity;private final Favorites favorites;private final Leads leads;private final Reports reports;
  ExtrasApi(Listings l,Identity i,Favorites f,Leads leads,Reports reports){this.listings=l;identity=i;favorites=f;this.leads=leads;this.reports=reports;}
  private Listing visible(Long id){Listing l=listings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin"));if(!Rules.visible(l.status,l.property.availability,l.expiresAt,Instant.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tin không còn hiển thị");return l;}
  public record Message(String message){}
  public record LeadView(Long id,Long listingId,String title,String sender,String phone,String message,Instant createdAt){
    static LeadView from(Lead l){return new LeadView(l.id,l.listing.id,l.listing.title,l.requester.fullName,l.requester.phone,l.message,l.createdAt);}
  }
  @GetMapping("/api/my/favorites") @Transactional(readOnly=true)
  public List<ListingApi.View> favorites(@AuthenticationPrincipal Jwt jwt){return favorites.findByUser_Id(identity.current(jwt).id).stream().map(f->f.listing).filter(l->Rules.visible(l.status,l.property.availability,l.expiresAt,Instant.now())).map(ListingApi.View::from).toList();}
  @PostMapping("/api/my/favorites/{id}") @Transactional
  public Map<String,Boolean> favorite(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User u=identity.current(jwt);Listing l=visible(id);
    if(favorites.findByUser_IdAndListing_Id(u.id,id).isEmpty()){Favorite f=new Favorite();f.user=u;f.listing=l;favorites.save(f);}return Map.of("saved",true);
  }
  @DeleteMapping("/api/my/favorites/{id}") @Transactional
  public Map<String,Boolean> unfavorite(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User u=identity.current(jwt);favorites.findByUser_IdAndListing_Id(u.id,id).ifPresent(favorites::delete);return Map.of("saved",false);}
  @PostMapping("/api/listings/{id}/contact") @Transactional
  public LeadView contact(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Message input){
    User u=identity.current(jwt);Listing l=visible(id);Rules.check(!u.id.equals(l.owner.id),"Không thể tự gửi liên hệ cho chính mình");
    Rules.check(input!=null && input.message()!=null&&input.message().trim().length()>=10&&input.message().length()<=800,"Nội dung liên hệ phải từ 10–800 ký tự");
    Rules.check(!leads.existsByRequester_IdAndListing_IdAndCreatedAtAfter(u.id,id,Instant.now().minus(Duration.ofHours(24))),"Bạn đã liên hệ tin này trong 24 giờ qua");
    Lead lead=new Lead();lead.listing=l;lead.requester=u;lead.message=input.message().trim();return LeadView.from(leads.save(lead));
  }
  @GetMapping("/api/my/leads") @Transactional(readOnly=true)
  public List<LeadView> received(@AuthenticationPrincipal Jwt jwt){return leads.findByListing_Owner_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(LeadView::from).toList();}
  @PostMapping("/api/listings/{id}/report") @Transactional
  public Map<String,String> report(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Message input){
    User u=identity.current(jwt);Listing l=visible(id);Rules.check(!u.id.equals(l.owner.id),"Không thể tự báo cáo tin của mình");
    Rules.check(input!=null && input.message()!=null&&input.message().trim().length()>=10&&input.message().length()<=800,"Lý do báo cáo phải từ 10–800 ký tự");
    Rules.check(!reports.existsByReporter_IdAndListing_IdAndStatus(u.id,id,ReportStatus.OPEN),"Bạn đã gửi báo cáo đang chờ xử lý cho tin này");
    Report r=new Report();r.listing=l;r.reporter=u;r.reason=input.message().trim();reports.save(r);return Map.of("message","Đã gửi báo cáo tới đội kiểm duyệt");
  }
}
