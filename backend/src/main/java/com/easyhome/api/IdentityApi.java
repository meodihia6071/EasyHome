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
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class IdentityApi {
  private final Identity identity;private final Identities requests;private final MediaFiles media;
  private final PrivateMediaStorage storage;private final Audits audits;private final Users users;
  IdentityApi(Identity i,Identities r,MediaFiles m,PrivateMediaStorage s,Audits a,Users users){identity=i;requests=r;media=m;storage=s;audits=a;this.users=users;}
  public record IdentityView(Long id,Long userId,String status,String note,Instant submittedAt,Instant reviewedAt,List<MediaApi.MediaView> files){
    static IdentityView of(IdentityVerification v,List<ListingMedia> files){return new IdentityView(v.id,v.user.getId(),v.status.name(),v.note,v.submittedAt,v.reviewedAt,files.stream().map(MediaApi.MediaView::of).toList());}
  }
  @GetMapping("/api/identity/me") @Transactional(readOnly=true)
  public IdentityView mine(@AuthenticationPrincipal Jwt jwt){
    User u=identity.current(jwt);return requests.findFirstByUser_IdOrderByIdDesc(u.id).map(v->IdentityView.of(v,media.findByIdentityVerification_IdOrderByIdAsc(v.id))).orElse(null);
  }
  @PostMapping("/api/identity/start") @Transactional
  public IdentityView start(@AuthenticationPrincipal Jwt jwt){
    User u=identity.current(jwt);Rules.check(u.identityStatus!=IdentityStatus.VERIFIED,"Đã xác minh danh tính");
    var old=requests.findFirstByUser_IdOrderByIdDesc(u.id);
    if(old.isPresent() && old.get().status==IdentityStatus.PENDING)throw new Rules.BusinessException("Hồ sơ đang chờ duyệt");
    if(old.isPresent() && old.get().status==IdentityStatus.NEEDS_UPDATE)return IdentityView.of(old.get(),media.findByIdentityVerification_IdOrderByIdAsc(old.get().id));
    IdentityVerification v=new IdentityVerification();v.user=u;v.status=IdentityStatus.NEEDS_UPDATE;requests.save(v);u.identityStatus=IdentityStatus.NEEDS_UPDATE;
    return IdentityView.of(v,List.of());
  }
  @PostMapping(value="/api/identity/{id}/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
  public MediaApi.MediaView upload(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestPart("file") MultipartFile file) throws IOException{
    User u=identity.current(jwt);IdentityVerification v=requests.findById(id).orElseThrow();
    if(!v.user.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Rules.check(v.status==IdentityStatus.NEEDS_UPDATE,"Hồ sơ hiện không được sửa");
    Rules.check(media.findByIdentityVerification_IdOrderByIdAsc(id).size()<2,"Chỉ nhận 2 ảnh giấy tờ cần thiết");
    byte[] b=file.getBytes();String name=storage.store(b);
    ListingMedia m=new ListingMedia();m.identityVerification=v;m.purpose="IDENTITY";m.storageName=name;m.contentType=PrivateMediaStorage.detect(b);m.bytes=b.length;
    media.save(m);return MediaApi.MediaView.of(m);
  }
  @PostMapping("/api/identity/{id}/submit") @Transactional
  public IdentityView submit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    User u=identity.current(jwt);IdentityVerification v=requests.findById(id).orElseThrow();
    if(!v.user.getId().equals(u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Rules.check(v.status==IdentityStatus.NEEDS_UPDATE,"Hồ sơ không trong trạng thái bổ sung");
    var list=media.findByIdentityVerification_IdOrderByIdAsc(id);
    Rules.check(list.size()==2,"Cần 2 ảnh xác thực danh tính");
    v.status=IdentityStatus.PENDING;u.identityStatus=IdentityStatus.PENDING;v.submittedAt=Instant.now();
    audits.save(new Audit(u.id,"IDENTITY_SUBMIT","identity:"+id));return IdentityView.of(v,list);
  }
  @GetMapping("/api/admin/identity/pending") @Transactional(readOnly=true)
  public List<IdentityView> pending(@AuthenticationPrincipal Jwt jwt){identity.admin(jwt);return requests.findByStatusOrderBySubmittedAtAsc(IdentityStatus.PENDING)
     .stream().map(v->IdentityView.of(v,media.findByIdentityVerification_IdOrderByIdAsc(v.id))).toList();}
  @PostMapping("/api/admin/identity/{id}/review") @Transactional
  public IdentityView review(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody ReviewDecision req){
    User staff=identity.admin(jwt);IdentityVerification v=requests.findById(id).orElseThrow();
    Rules.check(v.status==IdentityStatus.PENDING,"Hồ sơ không chờ duyệt");
    Rules.check(req!=null&&Set.of("VERIFIED","NEEDS_UPDATE","REJECTED").contains(req.decision()),"Quyết định không hợp lệ");
    if(!req.decision().equals("VERIFIED"))Rules.check(req.note()!=null&&req.note().trim().length()>=10,"Phải ghi lý do từ 10 ký tự");
    v.status=IdentityStatus.valueOf(req.decision());User owner=users.locked(v.user.getId()).orElseThrow();owner.identityStatus=v.status;owner.verified=v.status==IdentityStatus.VERIFIED;
    v.note=req.note();v.reviewerId=staff.id;v.reviewedAt=Instant.now();
    audits.save(new Audit(staff.id,"IDENTITY_"+v.status,"identity:"+id));
    return IdentityView.of(v,media.findByIdentityVerification_IdOrderByIdAsc(id));
  }
  public record ReviewDecision(String decision,String note){}
}
