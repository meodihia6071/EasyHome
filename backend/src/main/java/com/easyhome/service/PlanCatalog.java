package com.easyhome.service;
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

@Component
public class PlanCatalog {
  private final ServicePlans plans;private final ServicePlanHistories changes;private final Identity identity;
  public PlanCatalog(ServicePlans p,ServicePlanHistories h,Identity identity){plans=p;changes=h;this.identity=identity;}
  @EventListener(ApplicationReadyEvent.class) @Transactional
  public void seed(){
    for(ListingPlan code:ListingPlan.values()){
      if(code==ListingPlan.VIP)continue;
      if(plans.findByCode(code.name()).isPresent())continue;
      ServicePlan p=new ServicePlan();p.code=code.name();p.label=code.label;p.price=code.price;p.days=code.days;p.priority=code.priority;
      plans.save(p);
    }
  }
  public ServicePlan get(ListingPlan code){ServicePlan p=plans.findByCode(code.name()).orElseThrow(()->new Rules.BusinessException("Gói chưa được cấu hình"));
    RulesV2.require(p.active,"Gói dịch vụ không còn bán");return p;}
  @Transactional(readOnly=true)
  public List<BillingApi.PlanView> available(){return plans.findByActiveTrueOrderByPriorityAsc().stream().map(p->new BillingApi.PlanView(p.code,p.label,p.price,p.days,p.priority)).toList();}
  @Transactional
  public BillingApi.PlanView update(String code,User staff,int price,int days){
    RulesV2.require(price>=0&&price<=10_000_000&&days>=1&&days<=365,"Giá/thời hạn không hợp lệ");
    if(code.equals("FREE"))RulesV2.require(price==0&&days==7,"FREE phải là 0 đồng/7 ngày");
    ServicePlan p=plans.locked(code).orElseThrow();
    ServicePlanHistory h=new ServicePlanHistory();h.planCode=code;h.actorId=staff.id;h.oldPrice=p.price;h.oldDays=p.days;
    h.newPrice=price;h.newDays=days;changes.save(h);
    p.price=price;p.days=days;p.updatedAt=Instant.now();return new BillingApi.PlanView(p.code,p.label,p.price,p.days,p.priority);
  }
}
