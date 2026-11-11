package com.easyhome.service;
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

@org.springframework.stereotype.Component
public class Identity {
  private final Users users;
  public Identity(Users users){this.users=users;}
  public User current(Jwt jwt){
    if(jwt==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cần đăng nhập");
    try{
      User u=users.findById(Long.parseLong(jwt.getSubject())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Không tìm thấy tài khoản"));
      if(!u.emailVerified)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Email chưa xác thực");
      if(u.locked && (u.lockedUntil==null||u.lockedUntil.isAfter(Instant.now())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản đang bị khóa");
      return u;
    }catch(NumberFormatException ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token không hợp lệ");}
  }
  public User staff(Jwt jwt){User u=current(jwt);if(u.role!=AccountRole.MODERATOR&&u.role!=AccountRole.ADMIN&&u.role!=AccountRole.SUPER_ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền kiểm duyệt");return u;}
  public User admin(Jwt jwt){User u=current(jwt);if(u.role!=AccountRole.ADMIN&&u.role!=AccountRole.SUPER_ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ Admin được thao tác");return u;}
  User finance(Jwt jwt){User u=current(jwt);if(u.role!=AccountRole.FINANCE&&u.role!=AccountRole.ADMIN&&u.role!=AccountRole.SUPER_ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền tài chính");return u;}
}
