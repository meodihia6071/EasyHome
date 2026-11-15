package com.easyhome.api;

import com.easyhome.model.Audit;
import com.easyhome.repository.Audits;
import com.easyhome.service.Identity;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditApi {
  private final Audits audits;
  private final Identity identity;

  AuditApi(Audits audits,Identity identity){this.audits=audits;this.identity=identity;}

  public record AuditView(Long id,Long actorId,String action,String target,Instant at) {
    static AuditView from(Audit audit){return new AuditView(audit.id,audit.actorId,audit.action,audit.target,audit.createdAt);}
  }

  @GetMapping("/api/admin/audit") @Transactional(readOnly=true)
  public List<AuditView> recent(@AuthenticationPrincipal Jwt jwt){
    identity.admin(jwt);
    return audits.findTop100ByOrderByCreatedAtDesc().stream().map(AuditView::from).toList();
  }
}
