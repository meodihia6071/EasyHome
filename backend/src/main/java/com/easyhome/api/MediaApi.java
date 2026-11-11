package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MediaApi {
  private final MediaFiles media;private final Listings listings;private final Identity identity;
  private final Identities ids;private final PrivateMediaStorage files;
  MediaApi(MediaFiles m,Listings l,Identity i,Identities ids,PrivateMediaStorage f){media=m;listings=l;identity=i;this.ids=ids;files=f;}
  public record MediaView(Long id,String purpose,String url,long bytes) {
    static MediaView of(ListingMedia m){return new MediaView(m.id,m.purpose,"/api/media/"+m.id,m.bytes);}
  }
  @GetMapping("/api/my/listings/{id}/media") @Transactional(readOnly=true)
  public List<MediaView> mine(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);Listing l=listings.findById(id).orElseThrow();
    if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return media.findByListing_IdOrderByIdAsc(id).stream().map(MediaView::of).toList();
  }
  @PostMapping(value="/api/my/listings/{id}/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
  public MediaView upload(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestParam String purpose,@RequestPart("file") MultipartFile file) throws IOException {
    User u=identity.current(jwt);Listing l=listings.findById(id).orElseThrow();
    if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    RulesV2.require(l.status==ListingStatus.DRAFT||l.status==ListingStatus.REJECTED||l.status==ListingStatus.AWAITING_PAYMENT||l.status==ListingStatus.NEEDS_UPDATE,"Chỉ thêm ảnh trước khi gửi duyệt");Rules.check(Set.of("PUBLIC","EVIDENCE").contains(purpose),"Loại ảnh không hợp lệ");
    Rules.check(media.countByListing_IdAndPurpose(id,purpose)<(purpose.equals("PUBLIC")?15:10),"Đã vượt giới hạn ảnh");
    byte[] bytes=file.getBytes();String name=files.store(bytes);
    ListingMedia m=new ListingMedia();m.listing=l;m.purpose=purpose;m.storageName=name;m.contentType=PrivateMediaStorage.detect(bytes);m.bytes=bytes.length;
    media.save(m);
    if("PUBLIC".equals(purpose) && (l.property.getImageUrl()==null || l.property.getImageUrl().isBlank()))l.property.setImageUrl("/api/media/"+m.id);
    return MediaView.of(m);
  }
  @DeleteMapping("/api/my/listings/{id}/media/{mediaId}") @Transactional
  public void remove(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long mediaId){
    User u=identity.current(jwt);Listing l=listings.findById(id).orElseThrow();
    if(!l.owner.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    RulesV2.require(l.status==ListingStatus.DRAFT||l.status==ListingStatus.REJECTED||l.status==ListingStatus.AWAITING_PAYMENT||l.status==ListingStatus.NEEDS_UPDATE,"Chỉ xóa ảnh trước khi gửi duyệt");ListingMedia m=media.findById(mediaId).orElseThrow();
    Rules.check(m.listing!=null&&m.listing.getId().equals(id),"Ảnh không thuộc bài đăng");
    media.delete(m);
    if("PUBLIC".equals(m.purpose) && ("/api/media/"+mediaId).equals(l.property.getImageUrl())) {
      var remaining=media.findByListing_IdOrderByIdAsc(id).stream().filter(x->x.purpose.equals("PUBLIC")&&!x.id.equals(mediaId)).findFirst();
      l.property.setImageUrl(remaining.map(x->"/api/media/"+x.id).orElse(""));
    }
  }
  @GetMapping("/api/media/{id}") @Transactional(readOnly=true)
  public ResponseEntity<byte[]> view(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    ListingMedia m=media.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    boolean allowed=false;
    User u=null;
    if(jwt!=null)try{u=identity.current(jwt);}catch(ResponseStatusException ignored){}
    if(m.identityVerification!=null){allowed=u!=null&&(m.identityVerification.user.getId().equals(u.id)||u.role==AccountRole.ADMIN||u.role==AccountRole.SUPER_ADMIN);}
    else if(m.listing!=null){
      Listing l=m.listing;
      boolean visible="PUBLIC".equals(m.purpose)&&Rules.visible(l.getStatus(),l.property.getAvailability(),l.expiresAt,Instant.now());
      boolean isOwner=u!=null&&l.owner.getId().equals(u.id);
      boolean mod=u!=null&&(u.role==AccountRole.MODERATOR||u.role==AccountRole.ADMIN||u.role==AccountRole.SUPER_ADMIN);
      allowed=visible||isOwner||mod;
    }
    if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Ảnh riêng tư");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(m.contentType))
      .header("Content-Security-Policy","default-src 'none'; sandbox").body(files.read(m.storageName));
  }
}
