package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.Locale;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthApi {
  private final Users users; private final PasswordEncoder encoder; private final JwtEncoder jwt; private final Identity identity;
  AuthApi(Users users,PasswordEncoder encoder,JwtEncoder jwt,Identity identity){this.users=users;this.encoder=encoder;this.jwt=jwt;this.identity=identity;}
  public record Signup(@NotBlank @Size(min=2,max=120) String fullName,@Email @NotBlank String email,
    @NotBlank @Size(min=8,max=72) String password,@NotBlank @Pattern(regexp="[+0-9 ]{9,18}") String phone,
    PublisherType publisherType){}
  public record Login(@Email @NotBlank String email,@NotBlank String password){}
  public record Profile(Long id,String name,String email,String phone,String role,String publisherType,boolean verified){
    static Profile from(User u){return new Profile(u.id,u.fullName,u.email,u.phone,u.role.name(),u.publisherType.name(),u.verified);}
  }
  public record Session(String token,Profile user){}
  @PostMapping("/register") public Session register(@Valid @RequestBody Signup req){
    Rules.check(req!=null,"Thiếu thông tin đăng ký");
    String email=req.email().trim().toLowerCase(Locale.ROOT);
    Rules.check(!users.existsByEmailIgnoreCase(email),"Email đã được sử dụng");
    PublisherType type=req.publisherType()==null?PublisherType.OWNER:req.publisherType();
    User user=users.save(new User(req.fullName().trim(),email,encoder.encode(req.password()),req.phone().trim(),type));
    return session(user);
  }
  @PostMapping("/login") public Session login(@Valid @RequestBody Login req){
    Rules.check(req!=null,"Thiếu thông tin đăng nhập");
    User u=users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu"));
    if(!encoder.matches(req.password(),u.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sai email hoặc mật khẩu");
    if(u.locked)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản đã bị khóa");
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
@org.springframework.stereotype.Component
class Identity {
  private final Users users;
  Identity(Users users){this.users=users;}
  User current(Jwt jwt){
    if(jwt==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cần đăng nhập");
    try{
      User u=users.findById(Long.parseLong(jwt.getSubject())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Không tìm thấy tài khoản"));
      if(u.locked)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản bị khóa");
      return u;
    }catch(NumberFormatException ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token không hợp lệ");}
  }
  User staff(Jwt jwt){User u=current(jwt);if(u.role!=AccountRole.MODERATOR&&u.role!=AccountRole.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền kiểm duyệt");return u;}
  User admin(Jwt jwt){User u=current(jwt);if(u.role!=AccountRole.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ Admin được thao tác");return u;}
}
