package com.easyhome;
import com.easyhome.Rules.*;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class Errors {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<Map<String,String>> business(BusinessException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
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
