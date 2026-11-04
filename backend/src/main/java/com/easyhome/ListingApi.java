package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ListingApi {
  private final Listings listings;private final Properties properties;private final Identity identity;private final Audits audits;
  ListingApi(Listings l,Properties p,Identity i,Audits a){listings=l;properties=p;identity=i;audits=a;}
  public record Form(String title,String description,String city,String district,String address,String type,
    BigDecimal monthlyRent,Double area,Integer bedrooms,String imageUrl){}
  public record View(Long id,Long propertyId,Long ownerId,String ownerName,String ownerType,boolean ownerVerified,String phone,
    String title,String description,String city,String district,String type,BigDecimal monthlyRent,Double area,
    Integer bedrooms,String imageUrl,String status,String availability,String plan,int priority,
    Instant publishedAt,Instant expiresAt,String reviewNote,Instant createdAt) {
    static View from(Listing l){Property p=l.property;User u=l.owner;return new View(l.id,p.id,u.id,u.fullName,u.publisherType.name(),u.verified,u.phone,
      l.title,l.description,p.city,p.district,p.type,p.monthlyRent,p.area,p.bedrooms,p.imageUrl,l.status.name(),p.availability.name(),
      l.plan==null?null:l.plan.name(),l.priority,l.publishedAt,l.expiresAt,l.reviewNote,l.createdAt);}
  }
  private Listing get(Long id){return listings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin đăng"));}
  private Listing own(Long id,User u){Listing l=get(id);if(!l.owner.id.equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tin đăng không thuộc tài khoản của bạn");return l;}
  private void validate(Form f){Rules.check(f!=null,"Thiếu nội dung tin đăng");Rules.content(f.title(),f.description(),f.city(),f.district(),f.address(),f.type(),f.monthlyRent(),f.area(),f.bedrooms(),f.imageUrl());}
  private void apply(Property p,Listing l,Form f){
    p.city=f.city().trim();p.district=f.district().trim();p.address=f.address().trim();p.type=f.type();p.monthlyRent=f.monthlyRent();
    p.area=f.area();p.bedrooms=f.bedrooms();p.imageUrl=f.imageUrl().trim();l.title=f.title().trim();l.description=f.description().trim();
  }
  @GetMapping("/api/listings") @Transactional(readOnly=true)
  public Page<View> browse(@RequestParam(required=false) String q,@RequestParam(required=false) String city,
       @RequestParam(required=false) String type,@RequestParam(required=false) BigDecimal minPrice,
       @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="0") int page,
       @RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="newest") String sort){
    Rules.check(page>=0 && page<=10000 && size>=1 && size<=40,"Thông số phân trang không hợp lệ");
    Rules.check(minPrice==null||maxPrice==null||minPrice.compareTo(maxPrice)<=0,"Khoảng giá không hợp lệ");
    Rules.check(Set.of("newest","priceAsc","priceDesc").contains(sort),"Kiểu sắp xếp không hợp lệ");
    Instant now=Instant.now();
    Specification<Listing> spec=(root,query,cb)->{
      List<Predicate> filters=new ArrayList<>();
      var p=root.join("property");
      filters.add(cb.equal(root.get("status"),ListingStatus.ACTIVE));
      filters.add(cb.greaterThan(root.get("expiresAt"),now));
      filters.add(cb.equal(p.get("availability"),Availability.AVAILABLE));
      if(q!=null&&!q.isBlank()){
        String v="%"+q.trim().toLowerCase(Locale.ROOT)+"%";
        filters.add(cb.or(cb.like(cb.lower(root.get("title")),v),cb.like(cb.lower(p.get("district")),v),cb.like(cb.lower(p.get("city")),v)));
      }
      if(city!=null&&!city.isBlank())filters.add(cb.like(cb.lower(p.get("city")),"%"+city.trim().toLowerCase(Locale.ROOT)+"%"));
      if(type!=null&&!type.isBlank())filters.add(cb.equal(p.get("type"),type));
      if(minPrice!=null)filters.add(cb.greaterThanOrEqualTo(p.get("monthlyRent"),minPrice));
      if(maxPrice!=null)filters.add(cb.lessThanOrEqualTo(p.get("monthlyRent"),maxPrice));
      return cb.and(filters.toArray(new Predicate[0]));
    };
    // VIP first within default ranking; price sorts honor the user's requested order.
    PageRequest pageable=switch(sort){
      case "priceAsc"->PageRequest.of(page,size,Sort.by(Sort.Direction.ASC,"property.monthlyRent").and(Sort.by(Sort.Direction.DESC,"createdAt")));
      case "priceDesc"->PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"property.monthlyRent").and(Sort.by(Sort.Direction.DESC,"createdAt")));
      default->PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"priority","publishedAt"));
    };
    return listings.findAll(spec,pageable).map(View::from);
  }
  @GetMapping("/api/listings/{id}") @Transactional(readOnly=true)
  public View detail(@PathVariable Long id){Listing l=get(id);if(!Rules.visible(l.status,l.property.availability,l.expiresAt,Instant.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tin đã ẩn hoặc hết hạn");return View.from(l);}
  @GetMapping("/api/my/listings") @Transactional(readOnly=true)
  public List<View> mine(@AuthenticationPrincipal Jwt jwt){return listings.findByOwner_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(View::from).toList();}
  @GetMapping("/api/my/listings/{id}") @Transactional(readOnly=true)
  public Form editForm(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    Listing l=own(id,identity.current(jwt));Property p=l.property;
    return new Form(l.title,l.description,p.city,p.district,p.address,p.type,p.monthlyRent,p.area,p.bedrooms,p.imageUrl);
  }
  @PostMapping("/api/my/listings") @Transactional
  public View create(@AuthenticationPrincipal Jwt jwt,@RequestBody Form f){
    User u=identity.current(jwt);validate(f);
    Property p=new Property();p.owner=u;Listing l=new Listing();l.owner=u;l.property=p;apply(p,l,f);
    properties.save(p);listings.save(l);audits.save(new Audit(u.id,"CREATE_DRAFT","listing:"+l.id));return View.from(l);
  }
  @PutMapping("/api/my/listings/{id}") @Transactional
  public View update(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Form f){
    User u=identity.current(jwt);Listing l=own(id,u);Rules.edit(l.status);validate(f);apply(l.property,l,f);l.reviewNote=null;return View.from(l);
  }
  @PostMapping("/api/my/listings/{id}/submit") @Transactional
  public View submit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);Listing l=own(id,u);
    Rules.content(l.title,l.description,l.property.city,l.property.district,l.property.address,l.property.type,l.property.monthlyRent,l.property.area,l.property.bedrooms,l.property.imageUrl);
    Rules.check(l.property.availability==Availability.AVAILABLE,"Nhà không còn cho thuê");
    l.status=Rules.submit(l.status);l.reviewNote=null;audits.save(new Audit(u.id,"SUBMIT","listing:"+l.id));return View.from(l);
  }
  @PostMapping("/api/my/listings/{id}/relist") @Transactional
  public View relist(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);Listing original=own(id,u);
    Rules.check(original.status==ListingStatus.EXPIRED||original.status==ListingStatus.CLOSED,"Chỉ đăng lại tin đã hết hạn hoặc đã đóng");
    Rules.check(original.property.availability==Availability.AVAILABLE,"Bất động sản không còn cho thuê");
    Rules.check(!listings.existsByProperty_IdAndStatusIn(original.property.id,List.of(
      ListingStatus.DRAFT,ListingStatus.REJECTED,ListingStatus.PENDING_REVIEW,ListingStatus.AWAITING_PAYMENT,ListingStatus.ACTIVE,ListingStatus.PAUSED)),
      "Bất động sản này đã có tin đang xử lý hoặc hiển thị");
    Listing fresh=new Listing();fresh.property=original.property;fresh.owner=u;fresh.title=original.title;fresh.description=original.description;
    listings.save(fresh);audits.save(new Audit(u.id,"RELIST_DRAFT","listing:"+fresh.id));return View.from(fresh);
  }
  @PostMapping("/api/my/listings/{id}/pause") @Transactional
  public View pause(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){Listing l=own(id,identity.current(jwt));l.status=Rules.pause(l.status);return View.from(l);}
  @PostMapping("/api/my/listings/{id}/resume") @Transactional
  public View resume(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){Listing l=own(id,identity.current(jwt));l.status=Rules.resume(l.status,l.property.availability,l.expiresAt,Instant.now());return View.from(l);}
  @PostMapping("/api/my/listings/{id}/close") @Transactional
  public View close(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){Listing l=own(id,identity.current(jwt));l.status=Rules.close(l.status);return View.from(l);}
  @PostMapping("/api/my/listings/{id}/rented") @Transactional
  public View rented(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    Listing l=own(id,identity.current(jwt));l.status=Rules.close(l.status);l.property.availability=Availability.RENTED;return View.from(l);
  }
}
