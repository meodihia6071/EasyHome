package com.easyhome.service;
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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationService {
  private final Listings listings;private final ListingReviews reviews;private final MediaFiles media;
  private final BillingApi billing;private final Audits audits;
  ModerationService(Listings l,ListingReviews r,MediaFiles m,BillingApi b,Audits a){listings=l;reviews=r;media=m;billing=b;audits=a;}
  private Listing ownedPending(long id){
    Listing l=listings.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    RulesV2.require(l.status==ListingStatus.PENDING_REVIEW,"Tin không chờ kiểm duyệt");return l;
  }
  void record(Listing l,Long reviewer,String decision,String note){
    ListingReview r=new ListingReview();r.listing=l;r.revision=l.reviewRevision;r.reviewerId=reviewer;r.decision=decision;r.note=note;
    reviews.save(r);audits.save(new Audit(reviewer==null?l.owner.getId():reviewer,"LISTING_"+decision,"listing:"+l.id));
  }
  @Transactional
  public Listing approve(long id,User staff){
    Listing l=ownedPending(id);ServiceOrder o=billing.paidOrder(l);
    Rules.content(l.title,l.description,l.property.getCity(),l.property.getDistrict(),l.property.getAddress(),l.property.getType(),l.property.getMonthlyRent(),l.property.getArea(),l.property.getBedrooms(),l.property.getImageUrl());
    RulesV2.require(l.property.getAvailability()==Availability.AVAILABLE,"Bất động sản không còn cho thuê");
    RulesV2.require(l.owner.isVerified(),"Người đăng không còn xác thực hợp lệ");
    RulesV2.require(media.countByListing_IdAndPurpose(l.id,"PUBLIC")>=2 && media.countByListing_IdAndPurpose(l.id,"EVIDENCE")>=1,"Thiếu ảnh thực tế hoặc minh chứng");
    l.activatePackage(o.plan,o.prioritySnapshot,Instant.now(),o.durationDaysSnapshot);
    l.reviewNote=null;record(l,staff.id,"APPROVED",null);return l;
  }
  @Transactional
  public Listing reject(long id,User staff,String reason){
    RulesV2.require(reason!=null&&reason.trim().length()>=10&&reason.length()<=500,"Cần lý do từ chối 10-500 ký tự");
    Listing l=ownedPending(id);ServiceOrder o=billing.paidOrder(l);
    l.status=ListingStatus.REJECTED;l.reviewNote=reason.trim();l.needsUpdateDeadline=null;
    billing.refund(o);record(l,staff.id,"REJECTED",reason.trim());return l;
  }
  @Transactional
  public Listing needsUpdate(long id,User staff,String reason){
    RulesV2.require(reason!=null&&reason.trim().length()>=10&&reason.length()<=500,"Cần mô tả minh chứng cần bổ sung");
    Listing l=ownedPending(id);billing.paidOrder(l);
    l.status=ListingStatus.NEEDS_UPDATE;l.reviewNote=reason.trim();l.needsUpdateDeadline=Instant.now().plusSeconds(72*3600);
    record(l,staff.id,"NEEDS_UPDATE",reason.trim());return l;
  }
  @Transactional
  public void expireNeedsUpdate(long id){
    Listing l=listings.locked(id).orElseThrow();
    if(l.status!=ListingStatus.NEEDS_UPDATE||l.needsUpdateDeadline==null||l.needsUpdateDeadline.isAfter(Instant.now()))return;
    ServiceOrder o=billing.paidOrder(l);
    l.status=ListingStatus.REJECTED;l.reviewNote="Quá hạn bổ sung minh chứng 72 giờ";l.needsUpdateDeadline=null;
    billing.refund(o);record(l,null,"AUTO_REJECTED",l.reviewNote);
  }
}
