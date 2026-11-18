package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
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
  private final Listings listings;private final Properties properties;private final Identity identity;private final Audits audits;private final BillingApi billing;private final MediaFiles media;private final ListingReviews reviews;private final Users users;
  ListingApi(Listings l,Properties p,Identity i,Audits a,BillingApi billing,MediaFiles media,ListingReviews reviews,Users users){
    listings=l;properties=p;identity=i;audits=a;this.billing=billing;this.media=media;this.reviews=reviews;this.users=users;
  }
  public record Form(String title,String description,String city,String district,String address,String type,
    BigDecimal monthlyRent,Double area,Integer bedrooms,String imageUrl,PublisherType publisherType){}
  public record View(Long id,Long propertyId,Long ownerId,String ownerName,String ownerType,boolean ownerVerified,String phone,
    String title,String description,String city,String district,String type,BigDecimal monthlyRent,Double area,
    Integer bedrooms,String imageUrl,String status,String availability,String plan,int priority,
    Instant publishedAt,Instant expiresAt,String reviewNote,Instant createdAt) {
    static View from(Listing l){Property p=l.getProperty();User u=l.getOwner();PublisherType publisher=l.publisherType==null?PublisherType.OWNER:l.publisherType;return new View(l.getId(),p.getId(),u.getId(),u.getFullName(),publisher.name(),u.isVerified(),u.getPhone(),
      l.getTitle(),l.getDescription(),p.getCity(),p.getDistrict(),p.getType(),p.getMonthlyRent(),p.getArea(),p.getBedrooms(),p.getImageUrl(),l.getStatus().name(),p.getAvailability().name(),
      l.getPlan()==null?null:l.getPlan().name(),l.getPriority(),l.getPublishedAt(),l.getExpiresAt(),l.getReviewNote(),l.getCreatedAt());}
  }
  private Listing get(Long id){return listings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin đăng"));}
  private Listing own(Long id,User u){Listing l=get(id);if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tin đăng không thuộc tài khoản của bạn");return l;}
  private void validate(Form f){Rules.check(f!=null,"Thiếu nội dung tin đăng");Rules.content(f.title(),f.description(),f.city(),f.district(),f.address(),f.type(),f.monthlyRent(),f.area(),f.bedrooms(),f.imageUrl());}
  private void apply(Property p,Listing l,Form f){
    p.updateDetails(f.city().trim(),f.district().trim(),f.address().trim(),f.type(),f.monthlyRent(),
      f.area(),f.bedrooms(),f.imageUrl()==null?"":f.imageUrl().trim());l.title=f.title().trim();l.description=f.description().trim();
  }
  public record Locations(List<String> cities,List<String> districts,List<String> streets){}
  public record ListingHistory(Instant at,String action,String details,Long actorId,String actorName){}
  @GetMapping("/api/listings/locations") @Transactional(readOnly=true)
  public Locations locations(@RequestParam(required=false) String city,@RequestParam(required=false) String district){
    Instant now=Instant.now();List<String> streets=city==null||district==null?List.of():listings.findActiveAddresses(ListingStatus.ACTIVE,Availability.AVAILABLE,now,city,district);
    return new Locations(listings.findActiveCities(ListingStatus.ACTIVE,Availability.AVAILABLE,now),
      listings.findActiveDistricts(ListingStatus.ACTIVE,Availability.AVAILABLE,now),streets);
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
  public View detail(@PathVariable Long id){Listing l=get(id);if(!Rules.visible(l.status,l.property.getAvailability(),l.expiresAt,Instant.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tin đã ẩn hoặc hết hạn");return View.from(l);}
  @GetMapping("/api/my/listings") @Transactional(readOnly=true)
  public List<View> mine(@AuthenticationPrincipal Jwt jwt){return listings.findByOwner_IdOrderByCreatedAtDesc(identity.current(jwt).id).stream().map(View::from).toList();}
  @GetMapping("/api/my/listings/{id}") @Transactional(readOnly=true)
  public Form editForm(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    Listing l=own(id,identity.current(jwt));Property p=l.property;
    return new Form(l.title,l.description,p.getCity(),p.getDistrict(),p.getAddress(),p.getType(),p.getMonthlyRent(),p.getArea(),p.getBedrooms(),p.getImageUrl(),l.publisherType==null?PublisherType.OWNER:l.publisherType);
  }
  @GetMapping("/api/my/listings/{id}/history") @Transactional(readOnly=true)
  public List<ListingHistory> history(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    own(id,identity.current(jwt));var history=reviews.findByListing_IdOrderByCreatedAtAsc(id);
    List<Audit> activity=audits.findByTargetOrderByCreatedAtAsc("listing:"+id);
    Set<Long> actorIds=new HashSet<>(activity.stream().map(a->a.actorId).filter(actor->actor!=null&&actor>0).toList());
    history.stream().map(r->r.reviewerId).filter(Objects::nonNull).forEach(actorIds::add);
    Map<Long,String> names=new HashMap<>();
    for(User user:users.findAllById(actorIds))names.put(user.id,user.fullName);
    List<ListingHistory> result=new ArrayList<>();
    activity.stream().filter(a->!a.action.startsWith("LISTING_")).forEach(a->result.add(new ListingHistory(a.createdAt,a.action,a.details,a.actorId,
      a.actorId==0?"Hệ thống":names.getOrDefault(a.actorId,"Tài khoản #"+a.actorId))));
    history.forEach(r->result.add(new ListingHistory(r.createdAt,"LISTING_"+r.decision,r.note,r.reviewerId,
      r.reviewerId==null?"Hệ thống":names.getOrDefault(r.reviewerId,"Nhân viên #"+r.reviewerId))));
    result.sort(Comparator.comparing(ListingHistory::at));return result;
  }
  @PostMapping("/api/my/listings") @Transactional
  public View create(@AuthenticationPrincipal Jwt jwt,@RequestBody Form f){
    User u=identity.current(jwt);validate(f);
    Property p=new Property();p.owner=u;Listing l=new Listing();l.owner=u;l.property=p;l.publisherType=f.publisherType()==null?PublisherType.OWNER:f.publisherType();apply(p,l,f);
    properties.save(p);listings.save(l);audits.save(new Audit(u.id,"CREATE_DRAFT","listing:"+l.id));return View.from(l);
  }
  @PutMapping("/api/my/listings/{id}") @Transactional
  public View update(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Form f){
    User u=identity.current(jwt);Listing l=own(id,u);Rules.edit(l.status);validate(f);apply(l.property,l,f);if(f.publisherType()!=null)l.publisherType=f.publisherType();l.reviewNote=null;
    audits.save(new Audit(u.id,"UPDATE_DRAFT","listing:"+id));return View.from(l);
  }
  @PostMapping("/api/my/listings/{id}/submit") @Transactional
  public View submit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);RulesV2.require(u.isVerified(),"Cần xác thực danh tính trước khi gửi kiểm duyệt");
    Listing l=own(id,u);RulesV2.require(l.needsUpdateDeadline==null || l.needsUpdateDeadline.isAfter(Instant.now()),"Đã quá hạn bổ sung");
    billing.paidOrder(l);
    Rules.content(l.title,l.description,l.property.getCity(),l.property.getDistrict(),l.property.getAddress(),l.property.getType(),l.property.getMonthlyRent(),l.property.getArea(),l.property.getBedrooms(),l.property.getImageUrl());
    RulesV2.require(media.countByListing_IdAndPurpose(id,"PUBLIC")>=2,"Cần ít nhất 2 ảnh thực tế công khai");
    RulesV2.require(media.countByListing_IdAndPurpose(id,"EVIDENCE")>=1,"Cần ít nhất 1 ảnh minh chứng riêng");
    RulesV2.require(l.property.getAvailability()==Availability.AVAILABLE,"Nhà không còn cho thuê");
    l.setStatus(Rules.submit(l.getStatus()));l.reviewRevision++;l.reviewRequestedAt=Instant.now();l.needsUpdateDeadline=null;l.reviewNote=null;
    listings.saveAndFlush(l);
    audits.save(new Audit(u.id,"SUBMIT_REVIEW","listing:"+l.id));return View.from(l);
  }
  @PostMapping("/api/my/listings/{id}/relist") @Transactional
  public View relist(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);Listing original=own(id,u);
    Rules.check(original.status==ListingStatus.EXPIRED||original.status==ListingStatus.CLOSED,"Chỉ đăng lại tin đã hết hạn hoặc đã đóng");
    Rules.check(original.property.getAvailability()==Availability.AVAILABLE,"Bất động sản không còn cho thuê");
    Rules.check(!listings.existsByProperty_IdAndStatusIn(original.property.getId(),List.of(
      ListingStatus.DRAFT,ListingStatus.REJECTED,ListingStatus.NEEDS_UPDATE,ListingStatus.PENDING_REVIEW,ListingStatus.AWAITING_PAYMENT,ListingStatus.ACTIVE,ListingStatus.PAUSED)),
      "Bất động sản này đã có tin đang xử lý hoặc hiển thị");
    Listing fresh=new Listing();fresh.property=original.property;fresh.owner=u;fresh.title=original.title;fresh.description=original.description;fresh.publisherType=original.publisherType;
    listings.save(fresh);audits.save(new Audit(u.id,"RELIST_DRAFT","listing:"+fresh.id));return View.from(fresh);
  }
  @PostMapping("/api/my/listings/{id}/pause") @Transactional
  public View pause(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User u=identity.current(jwt);Listing l=own(id,u);l.setStatus(Rules.pause(l.getStatus()));audits.save(new Audit(u.id,"PAUSE_LISTING","listing:"+id));return View.from(l);}
  @PostMapping("/api/my/listings/{id}/resume") @Transactional
  public View resume(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User u=identity.current(jwt);Listing l=own(id,u);l.setStatus(Rules.resume(l.getStatus(),l.property.getAvailability(),l.expiresAt,Instant.now()));audits.save(new Audit(u.id,"RESUME_LISTING","listing:"+id));return View.from(l);}
  @PostMapping("/api/my/listings/{id}/close") @Transactional
  public View close(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){User u=identity.current(jwt);Listing l=own(id,u);l.setStatus(Rules.close(l.getStatus()));audits.save(new Audit(u.id,"CLOSE_LISTING","listing:"+id));return View.from(l);}
  @PostMapping("/api/my/listings/{id}/rented") @Transactional
  public View rented(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);Listing l=own(id,u);l.setStatus(Rules.close(l.getStatus()));l.property.setAvailability(Availability.RENTED);
    audits.save(new Audit(u.id,"MARK_RENTED","listing:"+id));return View.from(l);
  }
}
