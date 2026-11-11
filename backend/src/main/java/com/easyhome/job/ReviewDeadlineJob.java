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
class ReviewDeadlineJob {
  private final Listings listings;private final ModerationService moderation;
  ReviewDeadlineJob(Listings listings,ModerationService moderation){this.listings=listings;this.moderation=moderation;}
  @Scheduled(fixedDelayString="PT30M")
  public void expireSupplement(){
    for(Listing l:listings.findByStatusAndNeedsUpdateDeadlineBefore(ListingStatus.NEEDS_UPDATE,Instant.now()))
      moderation.expireNeedsUpdate(l.id);
  }
}
