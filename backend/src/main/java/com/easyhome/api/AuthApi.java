package com.easyhome.api;
import com.easyhome.common.Rules;
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
  AuthApi(Users users,PasswordEncoder encoder,JwtEncoder jwt,Identity identity,EmailService emails){
    this.users=users;this.encoder=encoder;this.jwt=jwt;this.identity=identity;this.emails=emails;
  }
  public record Signup(@NotBlank @Size(min=2,max=120) String fullName,@Email @NotBlank String email,
    @NotBlank @Size(min=8,max=72) String password,@NotBlank String phone,PublisherType publisherType){}
  public record Login(@Email @NotBlank String email,@NotBlank String password){}
  public record Profile(Long id,String name,String email,String phone,String role,String publisherType,boolean verified,boolean emailVerified,String identityStatus){
    static Profile from(User u){return new Profile(u.id,u.fullName,u.email,u.phone,u.role.name(),u.publisherType.name(),u.isVerified(),u.emailVerified,u.identityStatus.name());}
  }
  public record Session(String token,Profile user){}
  public record EmailCode(String email,String code){}
  public record EmailRequest(String email){}
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
    PublisherType type=req.publisherType()==null?PublisherType.OWNER:req.publisherType();
    User user=new User(req.fullName().trim(),email,encoder.encode(req.password()),phone,type);
    user.registrationExpiresAt=Instant.now().plus(Duration.ofHours(24));users.save(user);
    emails.send(user);return new Pending("Đã gửi OTP. Xác thực email để kích hoạt tài khoản.");
  }
  @PostMapping("/verify-email") @Transactional(noRollbackFor=Rules.BusinessException.class) public Pending verify(@RequestBody EmailCode req){
    Rules.check(req!=null && req.email()!=null,"Thiếu email");
    User u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new Rules.BusinessException("Email chưa được đăng ký"));
    Rules.check(u.registrationExpiresAt==null || u.registrationExpiresAt.isAfter(Instant.now()) || u.emailVerified,"Đăng ký đã quá hạn, vui lòng đăng ký lại");
    if(!u.emailVerified)emails.confirm(u,req.code());
    Rules.check(u.emailVerified,"OTP không đúng");
    u.registrationExpiresAt=null;return new Pending("Đã xác thực email. Bạn có thể đăng nhập.");
  }
  @PostMapping("/resend-email") @Transactional public Pending resend(@RequestBody EmailRequest req){
    Rules.check(req!=null && req.email()!=null,"Thiếu email");
    users.findByEmailIgnoreCase(req.email().trim()).ifPresent(u->{if(!u.emailVerified && (u.registrationExpiresAt==null||u.registrationExpiresAt.isAfter(Instant.now())))emails.send(u);});
    return new Pending("Nếu email chưa xác thực và đủ điều kiện, OTP đã được gửi.");
  }
  @PostMapping("/login") public Session login(@Valid @RequestBody Login req){
    User u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu"));
    if(!encoder.matches(req.password(),u.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu");
    if(!u.emailVerified)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Email chưa xác thực, hãy nhập OTP");
    if(u.locked && (u.lockedUntil==null||u.lockedUntil.isAfter(Instant.now())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản đang bị khóa");
    return session(u);
  }
  @GetMapping("/me") public Profile me(@AuthenticationPrincipal Jwt jwt){return Profile.from(identity.current(jwt));}
  private Session session(User u){
    Instant now=Instant.now();
    JwtClaimsSet claims=JwtClaimsSet.builder().issuer("easyhome").subject(u.id.toString()).issuedAt(now).expiresAt(now.plus(Duration.ofHours(12))).build();
    JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
    return new Session(jwt.encode(JwtEncoderParameters.from(header,claims)).getTokenValue(),Profile.from(u));
  }
}
