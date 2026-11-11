package com.easyhome.service;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PrivateMediaStorage {
  final Path base;
  public PrivateMediaStorage(@Value("${app.private-storage:./private-uploads}") String location){
    base=Path.of(location).toAbsolutePath().normalize();
    try{Files.createDirectories(base);}catch(IOException ex){throw new IllegalStateException("Không tạo được thư mục lưu trữ riêng tư",ex);}
  }
  public static String detect(byte[] b){
    if(b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255)return "image/jpeg";
    if(b.length>=8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71 && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10)return "image/png";
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ nhận ảnh JPEG/PNG hợp lệ");
  }
  public String store(byte[] b){
    Rules.check(b.length>0 && b.length<=6_000_000,"Ảnh phải từ 1 byte đến 6MB");detect(b);
    try (var stream=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(b))){
      var readers=javax.imageio.ImageIO.getImageReaders(stream);
      Rules.check(readers.hasNext(),"Ảnh JPEG/PNG không hợp lệ");
      var reader=readers.next();
      try{reader.setInput(stream);int width=reader.getWidth(0),height=reader.getHeight(0);
        Rules.check(width>0&&height>0&&width<=6000&&height<=6000&&((long)width*height)<=24_000_000L,"Kích thước ảnh vượt giới hạn");
        Rules.check(reader.read(0)!=null,"Tệp ảnh không thể đọc");
      }finally{reader.dispose();}
    }catch(IOException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tệp ảnh bị hỏng");}
    String name=UUID.randomUUID().toString().replace("-","");
    try{
      Path path=base.resolve(name);Files.write(path,b,StandardOpenOption.CREATE_NEW);
      try{Files.setPosixFilePermissions(path,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignore){}
      return name;
    }catch(IOException ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Không thể lưu ảnh");}
  }
  public byte[] read(String name){
    if(!name.matches("[a-f0-9]{32}"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy ảnh");
    try{return Files.readAllBytes(base.resolve(name));}
    catch(IOException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy ảnh");}
  }
}

