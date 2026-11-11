package com.easyhome.job;
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

@Component
public class AutoUnlockJob {
  private final Users users;private final LockHistories history;private final Audits audits;
  public AutoUnlockJob(Users u,LockHistories h,Audits a){users=u;history=h;audits=a;}
  @Scheduled(fixedDelayString="PT1M") @Transactional
  public void autoUnlock(){
    Instant now=Instant.now();
    for(User current:users.findByLockedTrueAndLockedUntilBefore(now)){
      if(current.locked&&current.lockedUntil!=null&&!current.lockedUntil.isAfter(now)){
        User u=users.locked(current.id).orElseThrow();
        if(u.locked&&u.lockedUntil!=null&&!u.lockedUntil.isAfter(now)){
          u.locked=false;u.lockedUntil=null;u.lockReason=null;
          AccountLockHistory h=new AccountLockHistory();h.user=u;h.actorId=0L;h.action="AUTO_UNLOCK";h.reason="Đã hết thời hạn khóa";
          history.save(h);audits.save(new Audit(0L,"AUTO_UNLOCK","user:"+u.id));
        }
      }
    }
  }
}
