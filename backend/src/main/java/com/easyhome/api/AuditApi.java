package com.easyhome.api;

import com.easyhome.model.Audit;
import com.easyhome.model.User;
import com.easyhome.repository.Audits;
import com.easyhome.repository.Users;
import com.easyhome.common.Rules;
import com.easyhome.service.Identity;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditApi {
  private final Audits audits;
  private final Identity identity;
  private final Users users;

  AuditApi(Audits audits,Identity identity,Users users){this.audits=audits;this.identity=identity;this.users=users;}

  public record AuditView(Long id,Long actorId,String actorName,String action,String target,String details,Instant at) {
    static AuditView from(Audit audit,String actorName){return new AuditView(audit.id,audit.actorId,actorName,audit.action,audit.target,audit.details,audit.createdAt);}
  }

  @GetMapping("/api/admin/audit") @Transactional(readOnly=true)
  public Page<AuditView> recent(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long actorId,
      @RequestParam(required=false) String action,@RequestParam(required=false) String target,
      @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
    identity.admin(jwt);
    return search(actorId,action,target,from,to,page,size);
  }

  @GetMapping("/api/staff/audit") @Transactional(readOnly=true)
  public Page<AuditView> staffHistory(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long actorId,
      @RequestParam(required=false) String action,@RequestParam(required=false) String target,
      @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
    User staff=identity.staff(jwt);
    Long scopedActor=staff.role==com.easyhome.common.Rules.AccountRole.ADMIN||staff.role==com.easyhome.common.Rules.AccountRole.SUPER_ADMIN?actorId:staff.id;
    return search(scopedActor,action,target,from,to,page,size);
  }

  private Page<AuditView> search(Long actorId,String action,String target,Instant from,Instant to,int page,int size){
    Rules.check(page>=0&&page<=10000&&size>=1&&size<=100,"Thông số phân trang không hợp lệ");
    Rules.check(from==null||to==null||!from.isAfter(to),"Khoảng thời gian không hợp lệ");
    String actionFilter=action==null?"":action.trim().toLowerCase(Locale.ROOT);
    String targetFilter=target==null?"":target.trim().toLowerCase(Locale.ROOT);
    Specification<Audit> spec=(root,query,cb)->{
      List<Predicate> filters=new java.util.ArrayList<>();
      if(actorId!=null)filters.add(cb.equal(root.get("actorId"),actorId));
      if(!actionFilter.isEmpty())filters.add(cb.like(cb.lower(root.get("action")),"%"+actionFilter+"%"));
      if(!targetFilter.isEmpty())filters.add(cb.like(cb.lower(root.get("target")),"%"+targetFilter+"%"));
      if(from!=null)filters.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));
      if(to!=null)filters.add(cb.lessThanOrEqualTo(root.get("createdAt"),to));
      return cb.and(filters.toArray(Predicate[]::new));
    };
    Page<Audit> result=audits.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id")));
    Map<Long,String> names=new HashMap<>();
    for(User user:users.findAllById(result.getContent().stream().map(a->a.actorId).filter(id->id>0).distinct().toList()))names.put(user.id,user.fullName);
    return result.map(a->AuditView.from(a,a.actorId==0?"Hệ thống":names.getOrDefault(a.actorId,"Tài khoản #"+a.actorId)));
  }
}
