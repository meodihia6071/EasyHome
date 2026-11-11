package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlanAdminApi {
  private final PlanCatalog catalog;private final Identity identity;
  public PlanAdminApi(PlanCatalog c,Identity i){catalog=c;identity=i;}
  public record PriceChange(int price,int days){}
  @PostMapping("/api/admin/plans/{code}")
  public BillingApi.PlanView change(@AuthenticationPrincipal Jwt jwt,@PathVariable String code,@RequestBody PriceChange input){
    return catalog.update(code,identity.admin(jwt),input.price(),input.days());
  }
}
