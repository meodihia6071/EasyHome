package com.easyhome.job;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistrationCleanupJob {
  private final Users users;private final EmailOtps otps;
  RegistrationCleanupJob(Users users,EmailOtps otps){this.users=users;this.otps=otps;}
  @Scheduled(fixedDelayString="PT10M") @Transactional
  public void cleanup(){
    for(User u:users.findByEmailVerifiedFalseAndRegistrationExpiresAtBefore(Instant.now())){
      User locked=users.locked(u.id).orElseThrow();
      if(!locked.emailVerified && locked.registrationExpiresAt!=null && locked.registrationExpiresAt.isBefore(Instant.now())){
        otps.deleteByUser_Id(locked.id);users.delete(locked);
      }
    }
  }
}
