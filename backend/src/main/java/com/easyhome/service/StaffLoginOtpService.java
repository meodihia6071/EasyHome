package com.easyhome.service;

import com.easyhome.common.Rules;
import com.easyhome.common.AccountLockedException;
import com.easyhome.model.StaffLoginChallenge;
import com.easyhome.model.User;
import com.easyhome.repository.StaffLoginChallenges;
import com.easyhome.repository.Users;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffLoginOtpService {
  private final StaffLoginChallenges challenges;
  private final Users users;
  private final EmailService email;
  private final SecureRandom random=new SecureRandom();
  private final int maxDailySends;

  StaffLoginOtpService(StaffLoginChallenges challenges,Users users,EmailService email,
      @Value("${app.staff-login-otp.max-daily-sends:5}") int maxDailySends){
    this.challenges=challenges;this.users=users;this.email=email;this.maxDailySends=maxDailySends;
  }

  public record Issued(String challengeId,String maskedEmail,Instant resendAvailableAt) {}

  @Transactional
  public Issued issue(User user){
    User current=users.locked(user.id).orElseThrow();
    return create(current);
  }

  @Transactional(noRollbackFor=Rules.BusinessException.class)
  public Issued resend(String challengeId){
    String challengeHash=hash(challengeId);
    StaffLoginChallenge old=challenges.lockedByHash(challengeHash).orElseThrow(StaffLoginOtpService::invalid);
    Rules.check(!old.consumed&&old.expiresAt.isAfter(Instant.now()),"Yêu cầu OTP đã hết hạn");
    Rules.check(!old.resendAt.isAfter(Instant.now()),"Chỉ được gửi lại mã sau 60 giây");
    User current=users.locked(old.user.id).orElseThrow(StaffLoginOtpService::invalid);
    requireStaff(current);checkQuota(current.id,Instant.now());
    old.consumed=true;
    return create(current);
  }

  @Transactional(noRollbackFor=Rules.BusinessException.class)
  public User confirm(String challengeId,String code){
    StaffLoginChallenge c=challenges.lockedByHash(hash(challengeId)).orElseThrow(StaffLoginOtpService::invalid);
    Rules.check(!c.consumed&&c.expiresAt.isAfter(Instant.now()),"Mã OTP đã hết hạn hoặc đã được sử dụng");
    Rules.check(c.attempts<5,"Đã nhập sai quá 5 lần, vui lòng yêu cầu mã mới");
    Rules.check(code!=null&&code.matches("[0-9]{6}"),"Mã OTP phải có 6 chữ số");
    c.attempts++;
    boolean matches=MessageDigest.isEqual(hash(c.nonce+":"+code).getBytes(StandardCharsets.UTF_8),c.codeHash.getBytes(StandardCharsets.UTF_8));
    if(!matches)return null;
    User current=users.locked(c.user.id).orElseThrow(StaffLoginOtpService::invalid);
    requireStaff(current);
    if(current.locked&&(current.lockedUntil==null||current.lockedUntil.isAfter(Instant.now())))
      throw new AccountLockedException(current.lockReason,current.lockedUntil);
    c.consumed=true;
    return current;
  }

  private Issued create(User user){
    Instant now=Instant.now();
    requireStaff(user);checkQuota(user.id,now);
    challenges.findByUser_IdAndConsumedFalse(user.id).forEach(c->c.consumed=true);
    String challengeId=Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
    String code=String.format("%06d",random.nextInt(1_000_000));
    StaffLoginChallenge c=new StaffLoginChallenge();c.user=user;c.challengeHash=hash(challengeId);c.nonce=UUID.randomUUID().toString();
    c.codeHash=hash(c.nonce+":"+code);c.createdAt=now;c.expiresAt=now.plusSeconds(300);c.resendAt=now.plusSeconds(60);
    challenges.save(c);email.sendStaffLoginOtp(user,code);
    return new Issued(challengeId,mask(user.email),c.resendAt);
  }

  private void checkQuota(Long userId,Instant now){
    Rules.check(challenges.countByUser_IdAndCreatedAtAfter(userId,now.minus(Duration.ofHours(24)))<maxDailySends,
      "Đã đạt giới hạn gửi OTP đăng nhập trong 24 giờ");
  }
  private static void requireStaff(User user){
    Rules.check(java.util.Set.of(com.easyhome.common.Rules.AccountRole.ADMIN,com.easyhome.common.Rules.AccountRole.SUPER_ADMIN,
      com.easyhome.common.Rules.AccountRole.MODERATOR,com.easyhome.common.Rules.AccountRole.FINANCE,
      com.easyhome.common.Rules.AccountRole.SUPPORT).contains(user.role),"Tài khoản không còn quyền nhân viên");
  }

  private byte[] randomBytes(int size){byte[] value=new byte[size];random.nextBytes(value);return value;}
  private static String hash(String value){
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
    catch(Exception e){throw new IllegalStateException(e);}
  }
  private static String mask(String email){int at=email.indexOf('@');if(at<1)return "***";return email.charAt(0)+"***"+email.substring(at);}
  private static Rules.BusinessException invalid(){return new Rules.BusinessException("Yêu cầu OTP không hợp lệ hoặc đã hết hạn");}

  @Scheduled(cron="0 23 3 * * *") @Transactional
  public void cleanup(){challenges.deleteByExpiresAtBefore(Instant.now().minus(Duration.ofDays(1)));}
}
