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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ExpiryJob {
  private final Listings listings;
  ExpiryJob(Listings listings){this.listings=listings;}
  @Scheduled(fixedDelayString="PT1M") @Transactional
  public void expire(){
    Instant now=Instant.now();
    for (Listing l:listings.findByStatusAndExpiresAtBefore(ListingStatus.ACTIVE,now)) l.status=ListingStatus.EXPIRED;
    for (Listing l:listings.findByStatusAndExpiresAtBefore(ListingStatus.PAUSED,now)) l.status=ListingStatus.EXPIRED;
  }
}

