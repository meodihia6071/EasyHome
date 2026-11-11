package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountManagementApi {
  private final Identity identity;private final Users users;private final LockHistories history;private final Audits audits;
  AccountManagementApi(Identity i,Users u,LockHistories h,Audits a){identity=i;users=u;history=h;audits=a;}
  public record LockInput(String reason,Integer days){}
  public record UnlockInput(String reason){}
  public record HistoryView(String action,String reason,Long actor,Instant at,Instant until){
    static HistoryView of(AccountLockHistory e){return new HistoryView(e.action,e.reason,e.actorId,e.createdAt,e.untilAt);}
  }
  @GetMapping("/api/admin/users/{id}/lock-history") @Transactional(readOnly=true)
  public List<HistoryView> history(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
    identity.admin(jwt);return history.findByUser_IdOrderByCreatedAtDesc(id).stream().map(HistoryView::of).toList();
  }
  @PostMapping("/api/admin/users/{id}/lock") @Transactional
  public AdminApi.UserView lock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody LockInput input){
    User staff=identity.admin(jwt);RulesV2.require(!staff.id.equals(id),"Không thể tự khóa tài khoản");
    RulesV2.require(input!=null&&input.reason()!=null&&input.reason().trim().length()>=10&&input.reason().length()<=500,"Cần lý do khóa từ 10-500 ký tự");
    RulesV2.require(input.days()==null||(input.days()>=1&&input.days()<=365),"Chọn khóa vĩnh viễn hoặc 1-365 ngày");
    User u=users.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    RulesV2.require(!u.locked,"Tài khoản đang bị khóa, hãy mở khóa trước");
    RulesV2.require(u.role!=AccountRole.SUPER_ADMIN || staff.role==AccountRole.SUPER_ADMIN,"Không được khóa Super Admin");
    u.locked=true;u.lockReason=input.reason().trim();u.lockedUntil=input.days()==null?null:Instant.now().plus(Duration.ofDays(input.days()));
    addLog(u,staff.id,"LOCK",u.lockReason,u.lockedUntil);return AdminApi.UserView.from(u);
  }
  @PostMapping("/api/admin/users/{id}/unlock") @Transactional
  public AdminApi.UserView unlock(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody UnlockInput input){
    User staff=identity.admin(jwt);RulesV2.require(input!=null&&input.reason()!=null&&input.reason().trim().length()>=10,"Cần lý do mở khóa từ 10 ký tự");
    User u=users.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    RulesV2.require(u.locked,"Tài khoản không bị khóa");u.locked=false;u.lockReason=null;u.lockedUntil=null;
    addLog(u,staff.id,"UNLOCK",input.reason().trim(),null);return AdminApi.UserView.from(u);
  }
  void addLog(User u,Long actor,String action,String reason,Instant until){
    AccountLockHistory h=new AccountLockHistory();h.user=u;h.actorId=actor;h.action=action;h.reason=reason;h.untilAt=until;
    history.save(h);audits.save(new Audit(actor,"ACCOUNT_"+action,"user:"+u.id));
  }
}
