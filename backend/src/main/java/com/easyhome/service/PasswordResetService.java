package com.easyhome.service;

import com.easyhome.common.Rules;
import com.easyhome.model.PasswordResetToken;
import com.easyhome.model.User;
import com.easyhome.repository.PasswordResetTokens;
import com.easyhome.repository.Users;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  private final PasswordResetTokens tokens;
  private final Users users;
  private final PasswordEncoder encoder;
  private final EmailService email;
  private final String frontendUrl;
  private final SecureRandom random = new SecureRandom();

  public PasswordResetService(PasswordResetTokens tokens, Users users, PasswordEncoder encoder,
      EmailService email, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
    this.tokens=tokens;this.users=users;this.encoder=encoder;this.email=email;this.frontendUrl=frontendUrl.replaceAll("/$","");
  }

  @Transactional
  public void request(String address) {
    if(address==null || address.isBlank())return;
    users.findByEmailIgnoreCase(address.trim()).ifPresent(found->{
      User user=users.locked(found.id).orElseThrow();
      if(tokens.findFirstByUser_IdOrderByCreatedAtDesc(user.id).filter(t->t.createdAt.plusSeconds(60).isAfter(Instant.now())).isPresent())return;
      tokens.deleteByUser_Id(user.id);
      byte[] bytes=new byte[32];random.nextBytes(bytes);
      String token=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      PasswordResetToken reset=new PasswordResetToken();reset.user=user;reset.tokenHash=hash(token);reset.expiresAt=Instant.now().plusSeconds(1200);
      tokens.save(reset);
      email.sendPasswordReset(user,frontendUrl+"/reset-password?token="+token);
    });
  }

  @Transactional(noRollbackFor = Rules.BusinessException.class)
  public void reset(String token,String password) {
    Rules.check(token!=null && token.length()>=40 && token.length()<=60,"Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
    String hash=hash(token);
    PasswordResetToken reset=tokens.findLockedByTokenHash(hash).orElseThrow(()->new Rules.BusinessException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));
    Rules.check(!reset.consumed && reset.expiresAt.isAfter(Instant.now()),"Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
    if(reset.attempts>=5)throw new Rules.BusinessException("Liên kết đã bị vô hiệu hóa, hãy yêu cầu liên kết mới");
    reset.attempts++;
    Rules.check(password!=null && password.length()>=8 && password.length()<=72,"Mật khẩu phải có từ 8 đến 72 ký tự");
    User user=users.locked(reset.user.getId()).orElseThrow();
    user.setPasswordHash(encoder.encode(password));
    user.revokeSessions();
    users.saveAndFlush(user);
    reset.consumed=true;
  }

  private static String hash(String value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
