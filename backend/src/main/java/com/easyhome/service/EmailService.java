package com.easyhome.service;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailService {
  private final EmailOtps otps;
  private final ObjectProvider<JavaMailSender> sender;
  private final String mode;
  private final String from;
  private final org.springframework.core.env.Environment environment;
  private final SecureRandom rng=new SecureRandom();
  EmailService(EmailOtps otps,ObjectProvider<JavaMailSender> sender,
    @Value("${app.email-mode:smtp}") String mode,@Value("${app.email-from:no-reply@easyhome.vn}") String from,org.springframework.core.env.Environment env){
    this.otps=otps;this.sender=sender;this.mode=mode;this.from=from;this.environment=env;
  }
  private static String hash(String nonce,String code){
    try{
      byte[] sha=MessageDigest.getInstance("SHA-256").digest((nonce+":"+code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(sha);
    }catch(Exception e){throw new IllegalStateException(e);}
  }
  @Transactional
  public void send(User user){
    if(user.emailVerified)return;
    Instant now=Instant.now();
    if(user.otpWindowStart==null || user.otpWindowStart.plusSeconds(86400).isBefore(now)){
      user.otpWindowStart=now;user.dailyOtpSends=0;
    }
    Rules.check(user.dailyOtpSends<5,"Đã đạt giới hạn 5 email OTP trong 24 giờ");
    var old=otps.findFirstByUser_IdOrderByIdDesc(user.id);
    if(old.isPresent())Rules.check(!old.get().resendAt.isAfter(now),"Chỉ được gửi lại OTP sau 60 giây");
    String code=String.format("%06d",rng.nextInt(1_000_000));
    EmailOtp otp=new EmailOtp();otp.user=user;otp.nonce=UUID.randomUUID().toString();
    otp.codeHash=hash(otp.nonce,code);otp.expiresAt=now.plusSeconds(300);otp.resendAt=now.plusSeconds(60);
    if(old.isPresent())old.get().consumed=true;
    otps.save(otp);user.dailyOtpSends++;
    if("console".equals(mode)){
      if(!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev","test")))
        throw new IllegalStateException("OTP console chỉ được phép trong profile dev/test");
      // Only use this explicit mode in an isolated development environment.
      System.out.println("[EASYHOME DEV EMAIL OTP] "+user.email+" => "+code);
    } else if("smtp".equals(mode)) {
      JavaMailSender mail=sender.getIfAvailable();
      if(mail==null)throw new IllegalStateException("Chưa cấu hình SMTP để gửi OTP");
      SimpleMailMessage msg=new SimpleMailMessage();msg.setTo(user.email);msg.setFrom(from);
      msg.setSubject("EasyHome - Ma xac thuc email");msg.setText("Ma OTP: "+code+". Co hieu luc trong 5 phut. Khong chia se ma nay.");
      mail.send(msg);
    } else throw new IllegalStateException("Chế độ gửi OTP không hợp lệ");
  }
  @Transactional
  public void confirm(User u,String code){
    Rules.check(code!=null && code.matches("[0-9]{6}"),"Mã OTP phải có 6 chữ số");
    EmailOtp otp=otps.findFirstByUser_IdOrderByIdDesc(u.id).orElseThrow(()->new Rules.BusinessException("Không có mã OTP"));
    Rules.check(!otp.consumed && otp.expiresAt.isAfter(Instant.now()),"OTP đã hết hạn, hãy gửi lại");
    Rules.check(otp.attempts<5,"Đã nhập sai quá 5 lần, vui lòng gửi lại mã");
    otp.attempts++;
    boolean same=MessageDigest.isEqual(hash(otp.nonce,code).getBytes(java.nio.charset.StandardCharsets.UTF_8),otp.codeHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if(!same)return; // caller checks status. This method deliberately commits attempt counter.
    otp.consumed=true;u.emailVerified=true;
  }
  boolean verifiedByCode(User u){return u.emailVerified;}
}
