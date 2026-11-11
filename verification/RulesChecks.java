package com.easyhome;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
public class RulesChecks {
  static int passed=0;
  static void ok(boolean truth,String name){if(!truth)throw new AssertionError(name);passed++;}
  static void bad(Runnable r,String name){try{r.run();throw new AssertionError(name+" did not reject");}catch(BusinessException expected){passed++;}}
  static void valid(){Rules.content("Căn hộ nhiều ánh sáng","Căn hộ thoáng mát, ban công rộng có nội thất cơ bản.","Hà Nội","Cầu Giấy","Dịch Vọng","APARTMENT",new BigDecimal(4500000),44d,2,"https://example.com/house.jpg");}
  public static void main(String[] args){
    valid();passed++;
    String title="Căn hộ nhiều ánh sáng",desc="Căn hộ thoáng mát, ban công rộng có nội thất cơ bản.",city="Hà Nội",district="Cầu Giấy",address="Dịch Vọng",type="APARTMENT",image="https://example.com/house.jpg";
    BigDecimal price=new BigDecimal(4500000);
    bad(()->Rules.content("          ",desc,city,district,address,type,price,44d,2,image),"title blank");
    bad(()->Rules.content("short",desc,city,district,address,type,price,44d,2,image),"title short");
    bad(()->Rules.content("x".repeat(161),desc,city,district,address,type,price,44d,2,image),"title long");
    bad(()->Rules.content(title,"too short",city,district,address,type,price,44d,2,image),"desc short");
    bad(()->Rules.content(title,"d".repeat(3001),city,district,address,type,price,44d,2,image),"desc long");
    bad(()->Rules.content(title,desc,"",district,address,type,price,44d,2,image),"city required");
    bad(()->Rules.content(title,desc,city,"",address,type,price,44d,2,image),"district required");
    bad(()->Rules.content(title,desc,city,district,"",type,price,44d,2,image),"address required");
    bad(()->Rules.content(title,desc,city,district,address,"LAND",price,44d,2,image),"unsupported type");
    bad(()->Rules.content(title,desc,city,district,address,type,BigDecimal.ZERO,44d,2,image),"zero price");
    bad(()->Rules.content(title,desc,city,district,address,type,new BigDecimal(99999),44d,2,image),"min price");
    bad(()->Rules.content(title,desc,city,district,address,type,new BigDecimal("1000000001"),44d,2,image),"max price");
    bad(()->Rules.content(title,desc,city,district,address,type,price,-1d,2,image),"negative area");
    bad(()->Rules.content(title,desc,city,district,address,type,price,4.99,2,image),"min area");
    bad(()->Rules.content(title,desc,city,district,address,type,price,Double.NaN,2,image),"area nan");
    bad(()->Rules.content(title,desc,city,district,address,type,price,Double.POSITIVE_INFINITY,2,image),"area infinity");
    bad(()->Rules.content(title,desc,city,district,address,type,price,10001d,2,image),"area max");
    bad(()->Rules.content(title,desc,city,district,address,type,price,44d,-1,image),"bedrooms negative");
    bad(()->Rules.content(title,desc,city,district,address,type,price,44d,31,image),"bedrooms max");
    bad(()->Rules.content(title,desc,city,district,address,type,price,44d,2,"javascript:alert(1)"),"image unsafe scheme");
    Rules.content(title,desc,city,district,address,type,price,44d,2,"");passed++; // draft permits uploading images after saving
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.AWAITING_PAYMENT||status==ListingStatus.NEEDS_UPDATE)ok(Rules.submit(status)==ListingStatus.PENDING_REVIEW,"submit "+status);
      else bad(()->Rules.submit(status),"submit "+status);
    }
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.PENDING_REVIEW){ok(Rules.review(status,true)==ListingStatus.ACTIVE,"approve");ok(Rules.review(status,false)==ListingStatus.REJECTED,"reject");}
      else{bad(()->Rules.review(status,true),"review approved "+status);bad(()->Rules.review(status,false),"review rejected "+status);}
    }
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.DRAFT||status==ListingStatus.REJECTED){Rules.checkout(status);passed++;}
      else bad(()->Rules.checkout(status),"checkout "+status);
    }
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.DRAFT||status==ListingStatus.REJECTED||status==ListingStatus.NEEDS_UPDATE){Rules.edit(status);passed++;}
      else bad(()->Rules.edit(status),"edit "+status);
    }
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.ACTIVE)ok(Rules.pause(status)==ListingStatus.PAUSED,"pause active");
      else bad(()->Rules.pause(status),"pause "+status);
    }
    Instant now=Instant.parse("2026-11-04T12:00:00Z");Instant tomorrow=now.plus(1,ChronoUnit.DAYS);Instant yesterday=now.minus(1,ChronoUnit.DAYS);
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.PAUSED){ok(Rules.resume(status,Availability.AVAILABLE,tomorrow,now)==ListingStatus.ACTIVE,"resume paused");}
      else bad(()->Rules.resume(status,Availability.AVAILABLE,tomorrow,now),"resume "+status);
    }
    bad(()->Rules.resume(ListingStatus.PAUSED,Availability.RENTED,tomorrow,now),"resume rented");
    bad(()->Rules.resume(ListingStatus.PAUSED,Availability.UNAVAILABLE,tomorrow,now),"resume unavailable");
    bad(()->Rules.resume(ListingStatus.PAUSED,Availability.AVAILABLE,yesterday,now),"resume expired");
    bad(()->Rules.resume(ListingStatus.PAUSED,Availability.AVAILABLE,now,now),"resume exact expiry");
    bad(()->Rules.resume(ListingStatus.PAUSED,Availability.AVAILABLE,null,now),"resume unissued");
    for(ListingStatus status:ListingStatus.values()){
      if(status==ListingStatus.ACTIVE||status==ListingStatus.PAUSED||status==ListingStatus.EXPIRED)ok(Rules.close(status)==ListingStatus.CLOSED,"close "+status);
      else bad(()->Rules.close(status),"close "+status);
    }
    for(ListingStatus status:ListingStatus.values())for(Availability a:Availability.values()){
      boolean expected=status==ListingStatus.ACTIVE&&a==Availability.AVAILABLE;
      ok(Rules.visible(status,a,tomorrow,now)==expected,"visibility " +status+"/"+a);
      ok(!Rules.visible(status,a,yesterday,now),"expired visibility "+status+"/"+a);
    }
    ok(!Rules.visible(ListingStatus.ACTIVE,Availability.AVAILABLE,now,now),"expiry boundary");
    ok(!Rules.visible(ListingStatus.ACTIVE,Availability.AVAILABLE,null,now),"null expiry");
    ok(Rules.activate(ListingStatus.PENDING_REVIEW,Availability.AVAILABLE)==ListingStatus.ACTIVE,"activate valid");
    for(Availability a:Availability.values())if(a!=Availability.AVAILABLE)bad(()->Rules.activate(ListingStatus.PENDING_REVIEW,a),"activate "+a);
    for(ListingStatus status:ListingStatus.values())if(status!=ListingStatus.PENDING_REVIEW)bad(()->Rules.activate(status,Availability.AVAILABLE),"activate "+status);
    for(ListingPlan plan:ListingPlan.values()){
      ok(Rules.expiry(now,plan).equals(now.plus(plan.days,ChronoUnit.DAYS)),"plan duration "+plan);
      ok(plan.price>=0 && plan.days>0 && plan.priority>=0,"plan rule "+plan);
    }
    bad(()->Rules.expiry(now,null),"null plan");bad(()->Rules.expiry(null,ListingPlan.STANDARD),"null payment instant");
    ok(ListingPlan.FREE.price==0,"free 0 price");ok(ListingPlan.STANDARD.price>0,"paid standard");ok(ListingPlan.VIP_DIAMOND.priority>ListingPlan.STANDARD.priority,"vip priority");
    System.out.println("PASS: "+passed+" business rule checks");
  }
}
