package com.easyhome.common;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(AccountLockedException.class)
  ResponseEntity<Map<String,Object>> locked(AccountLockedException e){
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
      "code","ACCOUNT_LOCKED","message",e.getMessage(),"reason",e.reason()==null?"Không có lý do được ghi nhận":e.reason(),
      "until",e.until()==null?"Vĩnh viễn":e.until().toString()));
  }
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<Map<String,Object>> business(BusinessException e){
    String message=e.getMessage()==null?"Yêu cầu không hợp lệ":e.getMessage();
    boolean identityRequired=message.toLowerCase(java.util.Locale.ROOT).contains("cần xác thực")||message.toLowerCase(java.util.Locale.ROOT).contains("chỉ tài khoản được xác thực");
    return ResponseEntity.badRequest().body(identityRequired?Map.of("message",message,"code","IDENTITY_REQUIRED"):Map.of("message",message));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException e){
    String message=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Dữ liệu không hợp lệ");
    return ResponseEntity.badRequest().body(Map.of("message",message));
  }
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String,String>> malformed(HttpMessageNotReadableException e){return ResponseEntity.badRequest().body(Map.of("message","Dữ liệu JSON hoặc giá trị lựa chọn không hợp lệ"));}
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String,String>> conflict(DataIntegrityViolationException e){return ResponseEntity.status(409).body(Map.of("message","Dữ liệu bị trùng hoặc vi phạm ràng buộc"));}
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String,String>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Yêu cầu không hợp lệ":e.getReason()));}
}
