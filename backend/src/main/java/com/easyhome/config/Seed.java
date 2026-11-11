package com.easyhome.config;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Seed {
  @Bean CommandLineRunner seedDemoData(Users users,Properties properties,Listings listings,PasswordEncoder encoder,
      @Value("${app.seed-demo}") boolean enabled){
    return args->{
      if(!enabled)return;
      if(users.count()>0){
        // Only preserve access to existing demo accounts with matching seeded credentials.
        // Never blindly mark a real user's email/identity as verified.
        users.findByEmailIgnoreCase("admin@easyhome.vn").ifPresent(u->{
          if(u.role==AccountRole.ADMIN&&encoder.matches("Admin@12345",u.passwordHash)){
            u.emailVerified=true;u.verified=true;u.identityStatus=IdentityStatus.VERIFIED;users.save(u);
          }
        });
        users.findByEmailIgnoreCase("mod@easyhome.vn").ifPresent(u->{
          if(u.role==AccountRole.MODERATOR&&encoder.matches("Mod@123456",u.passwordHash)){
            u.emailVerified=true;u.verified=true;u.identityStatus=IdentityStatus.VERIFIED;users.save(u);
          }
        });
        users.findByEmailIgnoreCase("owner@easyhome.vn").ifPresent(u->{
          if(encoder.matches("Owner@12345",u.passwordHash)){
            u.emailVerified=true;u.verified=true;u.identityStatus=IdentityStatus.VERIFIED;users.save(u);
          }
        });
        users.findByEmailIgnoreCase("user@easyhome.vn").ifPresent(u->{
          if(encoder.matches("User@123456",u.passwordHash)){u.emailVerified=true;users.save(u);}
        });
        return;
      }
      User admin=new User("EasyHome Admin","admin@easyhome.vn",encoder.encode("Admin@12345"),"+84900000001",PublisherType.OWNER);admin.role=AccountRole.ADMIN;admin.verified=true;admin.identityStatus=IdentityStatus.VERIFIED;admin.emailVerified=true;users.save(admin);
      User moderator=new User("Bộ phận kiểm duyệt","mod@easyhome.vn",encoder.encode("Mod@123456"),"+84900000002",PublisherType.OWNER);moderator.role=AccountRole.MODERATOR;moderator.verified=true;moderator.identityStatus=IdentityStatus.VERIFIED;moderator.emailVerified=true;users.save(moderator);
      User owner=new User("Nguyễn Minh Anh","owner@easyhome.vn",encoder.encode("Owner@12345"),"+84900000003",PublisherType.OWNER);owner.verified=true;owner.identityStatus=IdentityStatus.VERIFIED;owner.emailVerified=true;users.save(owner);
      User guest=new User("Trần Gia Huy","user@easyhome.vn",encoder.encode("User@123456"),"+84900000004",PublisherType.OWNER);guest.emailVerified=true;users.save(guest);
      String[][] data={
        {"Căn hộ sáng thoáng cạnh Hồ Tây","Căn hộ thiết kế mở, ban công nhiều ánh sáng tự nhiên, có thang máy và nội thất đầy đủ.","Hà Nội","Tây Hồ","Xuân La, Tây Hồ","APARTMENT","9500000","68","2","https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=85"},
        {"Studio có gác và cửa sổ lớn","Studio khép kín yên tĩnh, có điều hòa và máy giặt riêng, đi bộ đến nhiều tiện ích khu vực.","Hà Nội","Cầu Giấy","Dịch Vọng, Cầu Giấy","STUDIO","4200000","30","1","https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=85"},
        {"Nhà nguyên căn ba phòng ngủ","Nhà riêng ba phòng ngủ, sân nhỏ, bếp rộng, khu dân cư yên tĩnh và thuận tiện đi lại.","Hà Nội","Thanh Xuân","Nhân Chính, Thanh Xuân","HOUSE","15500000","110","3","https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&q=85"},
        {"Căn hộ nội thất mới Mỹ Đình","Căn hộ tầng cao có hai phòng ngủ, đầy đủ đồ dùng cơ bản, gần bến xe và trung tâm mua sắm.","Hà Nội","Nam Từ Liêm","Mỹ Đình, Nam Từ Liêm","APARTMENT","11000000","75","2","https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?w=1000&q=85"},
        {"Phòng cho thuê trung tâm Quận 1","Phòng có cửa sổ và nhà vệ sinh riêng, vị trí thuận tiện, phù hợp người đi làm và sinh viên.","TP. Hồ Chí Minh","Quận 1","Đa Kao, Quận 1","ROOM","5800000","29","1","https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=1000&q=85"},
        {"Căn hộ view sông tại Đà Nẵng","Căn hộ phong cách tối giản, phòng khách rộng, ban công hướng sông và gần các tuyến đường chính.","Đà Nẵng","Sơn Trà","An Hải, Sơn Trà","APARTMENT","7900000","62","2","https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1000&q=85"}
      };
      for(int i=0;i<data.length;i++){
        String[] x=data[i];Property p=new Property();p.owner=owner;p.city=x[2];p.district=x[3];p.address=x[4];p.type=x[5];
        p.monthlyRent=new BigDecimal(x[6]);p.area=Double.valueOf(x[7]);p.bedrooms=Integer.valueOf(x[8]);p.imageUrl=x[9];properties.save(p);
        Listing l=new Listing();l.owner=owner;l.property=p;l.title=x[0];l.description=x[1];l.status=ListingStatus.ACTIVE;
        l.plan=i%3==0?ListingPlan.VIP:ListingPlan.STANDARD;l.priority=l.plan.priority;
        l.publishedAt=Instant.now().minus(i,ChronoUnit.DAYS);l.expiresAt=Instant.now().plus(18-i,ChronoUnit.DAYS);listings.save(l);
      }
    };
  }
}
