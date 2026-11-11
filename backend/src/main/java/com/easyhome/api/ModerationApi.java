package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class ModerationApi {
  private final Listings listings;private final ListingReviews reviews;private final MediaFiles media;
  private final Identity identity;private final ModerationService moderation;
  ModerationApi(Listings l,ListingReviews r,MediaFiles m,Identity i,ModerationService s){listings=l;reviews=r;media=m;identity=i;moderation=s;}
  public record Detail(ListingApi.View listing,String ownerIdentityStatus,List<MediaApi.MediaView> media,List<ReviewView> history){}
  public record ReviewView(int revision,String decision,String note,Long reviewerId,java.time.Instant at){}
  public record Reason(String note){}
  @GetMapping("/api/admin/listings/{id}/moderation-detail") @Transactional(readOnly=true)
  public Detail detail(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    identity.staff(jwt);Listing l=listings.findById(id).orElseThrow();
    return new Detail(ListingApi.View.from(l),l.getOwner().getIdentityStatus().name(),
      media.findByListing_IdOrderByIdAsc(id).stream().map(MediaApi.MediaView::of).toList(),
      reviews.findByListing_IdOrderByCreatedAtAsc(id).stream().map(r->new ReviewView(r.revision,r.decision,r.note,r.reviewerId,r.createdAt)).toList());
  }
  @PostMapping("/api/admin/listings/{id}/needs-update") @Transactional
  public ListingApi.View needsUpdate(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Reason reason){
    return ListingApi.View.from(moderation.needsUpdate(id,identity.staff(jwt),reason.note()));
  }
}
