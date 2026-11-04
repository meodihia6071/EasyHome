package com.easyhome;
import com.easyhome.Rules.*;

import java.time.Instant;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@SpringBootApplication @EnableScheduling
public class EasyHomeApplication { public static void main(String[] args){SpringApplication.run(EasyHomeApplication.class,args);} }
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
