package com.easyhome.api;
import com.easyhome.common.Rules;
import com.easyhome.common.AccountLockedException;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.Locale;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthApi {
  private final Users users; private final PasswordEncoder encoder; private final JwtEncoder jwt; private final Identity identity;
  private final EmailService emails;
  private final PasswordResetService resets;
  private final StaffLoginOtpService staffLoginOtp;
  AuthApi(Users users,PasswordEncoder encoder,JwtEncoder jwt,Identity identity,EmailService emails,PasswordResetService resets,StaffLoginOtpService staffLoginOtp){
    this.users=users;this.encoder=encoder;this.jwt=jwt;this.identity=identity;this.emails=emails;this.resets=resets;this.staffLoginOtp=staffLoginOtp;
  }
  public record Signup(@NotBlank @Size(min=2,max=120) String fullName,@Email @NotBlank String email,
    @NotBlank @Size(min=8,max=72) String password,@NotBlank String phone){}
  public record Login(@Email @NotBlank String email,@NotBlank String password){}
  public record Profile(Long id,String name,String email,String phone,String role,String publisherType,boolean verified,boolean emailVerified,String identityStatus){
    static Profile from(User u){return new Profile(u.id,u.fullName,u.email,u.phone,u.role.name(),u.publisherType.name(),u.isVerified(),u.emailVerified,u.identityStatus.name());}
  }
  public record Session(String token,Profile user){}
  public record EmailCode(String email,String code){}
  public record StaffOtpCode(String challengeId,String code){}
  public record StaffOtpResend(String challengeId){}
  public record LoginPending(boolean requiresOtp,String challengeId,String maskedEmail,String message,Instant resendAvailableAt){}
  public record EmailRequest(String email){}
  public record ResetRequest(String email){}
  public record ResetConfirmation(String token,String password){}
  public record SessionStatus(boolean active){}
  public record Pending(String message){}
  public static String normalizePhone(String phone){
    Rules.check(phone!=null,"Thiếu số điện thoại");
    String s=phone.trim().replace(" ","").replace("-","");
    if(s.startsWith("+84"))s="0"+s.substring(3);
    if(s.startsWith("84"))s="0"+s.substring(2);
    Rules.check(s.matches("0[35789][0-9]{8}"),"Số điện thoại Việt Nam phải có 10 chữ số");
    return "+84"+s.substring(1);
  }
  @PostMapping("/register") @Transactional public Pending register(@Valid @RequestBody Signup req){
    String email=req.email().trim().toLowerCase(Locale.ROOT),phone=normalizePhone(req.phone());
    Rules.check(!users.existsByEmailIgnoreCase(email),"Email đã được đăng ký");
    Rules.check(!users.existsByPhone(phone) && !users.existsByPhone("0"+phone.substring(3)),"Số điện thoại đã được đăng ký");
    User user=new User(req.fullName().trim(),email,encoder.encode(req.password()),phone,PublisherType.OWNER);
    user.registrationExpiresAt=Instant.now().plus(Duration.ofHours(24));users.save(user);
    emails.send(user);return new Pending("Đã gửi OTP. Xác thực email để kích hoạt tài khoản.");
  }
  @PostMapping("/verify-email") @Transactional(noRollbackFor=Rules.BusinessException.class) public Session verify(@RequestBody EmailCode req){
    Rules.check(req!=null && req.email()!=null,"Thiếu email");
    User u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new Rules.BusinessException("Email chưa được đăng ký"));
    Rules.check(u.registrationExpiresAt==null || u.registrationExpiresAt.isAfter(Instant.now()) || u.emailVerified,"Đăng ký đã quá hạn, vui lòng đăng ký lại");
    if(!u.emailVerified)emails.confirm(u,req.code());
    Rules.check(u.emailVerified,"OTP không đúng");
    u.registrationExpiresAt=null;return session(u);
  }
  @PostMapping("/resend-email") @Transactional public Pending resend(@RequestBody EmailRequest req){
    Rules.check(req!=null && req.email()!=null,"Thiếu email");
    users.findByEmailIgnoreCase(req.email().trim()).ifPresent(u->{if(!u.emailVerified && (u.registrationExpiresAt==null||u.registrationExpiresAt.isAfter(Instant.now())))emails.send(u);});
    return new Pending("Nếu email chưa xác thực và đủ điều kiện, OTP đã được gửi.");
  }
  @PostMapping("/login") public Object login(@Valid @RequestBody Login req){
    User u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu"));
    if(!encoder.matches(req.password(),u.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu");
    if(!u.emailVerified)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Email chưa xác thực, hãy nhập OTP");
    if(u.locked && (u.lockedUntil==null||u.lockedUntil.isAfter(Instant.now())))throw new AccountLockedException(u.lockReason,u.lockedUntil);
    if(java.util.Set.of(AccountRole.ADMIN,AccountRole.SUPER_ADMIN,AccountRole.MODERATOR,AccountRole.FINANCE,AccountRole.SUPPORT).contains(u.role)){
      StaffLoginOtpService.Issued challenge=staffLoginOtp.issue(u);
      return new LoginPending(true,challenge.challengeId(),challenge.maskedEmail(),"Mã OTP đăng nhập đã được gửi tới email công việc.",challenge.resendAvailableAt());
    }
    return session(u);
  }
  @PostMapping("/login/verify-otp") @Transactional(noRollbackFor=Rules.BusinessException.class)
  public Session verifyStaffLoginOtp(@RequestBody StaffOtpCode request){
    Rules.check(request!=null,"Yêu cầu OTP không hợp lệ");
    User user=staffLoginOtp.confirm(request.challengeId(),request.code());
    Rules.check(user!=null,"Mã OTP không đúng");
    return session(user);
  }
  @PostMapping("/login/resend-otp")
  public LoginPending resendStaffLoginOtp(@RequestBody StaffOtpResend request){
    Rules.check(request!=null,"Yêu cầu OTP không hợp lệ");
    StaffLoginOtpService.Issued challenge=staffLoginOtp.resend(request.challengeId());
    return new LoginPending(true,challenge.challengeId(),challenge.maskedEmail(),"Mã OTP mới đã được gửi.",challenge.resendAvailableAt());
  }
  @GetMapping("/me") public Profile me(@AuthenticationPrincipal Jwt jwt){return Profile.from(identity.current(jwt));}
  @GetMapping("/session-status") public SessionStatus sessionStatus(@AuthenticationPrincipal Jwt jwt){identity.current(jwt);return new SessionStatus(true);}
  @PostMapping("/password/forgot") public Pending forgotPassword(@RequestBody ResetRequest req){
    if(req!=null)resets.request(req.email());
    return new Pending("Nếu email tồn tại, liên kết đặt lại mật khẩu sẽ được gửi.");
  }
  @PostMapping("/password/reset") public Pending resetPassword(@RequestBody ResetConfirmation req){
    Rules.check(req!=null,"Dữ liệu không hợp lệ");resets.reset(req.token(),req.password());
    return new Pending("Mật khẩu đã được cập nhật. Bạn có thể đăng nhập.");
  }
  private Session session(User u){
    Instant now=Instant.now();
    JwtClaimsSet claims=JwtClaimsSet.builder().issuer("easyhome").subject(u.id.toString()).claim("av",u.authVersion).issuedAt(now).expiresAt(now.plus(Duration.ofHours(12))).build();
    JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
    return new Session(jwt.encode(JwtEncoderParameters.from(header,claims)).getTokenValue(),Profile.from(u));
  }
}
