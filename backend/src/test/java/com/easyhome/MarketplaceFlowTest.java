package com.easyhome;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration tests for the approved Phase-1 flow. Requires Maven dependencies. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:easyhome_v2;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.seed-demo=true", "app.mock-payments=true", "app.email-mode=console"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired Listings listings;
  @Autowired Wallets wallets;
  @Autowired Users users;
  private static final byte[] PNG=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGNcUJHCwMDAxMDAwMDAAAASNgGAoiIFpwAAAABJRU5ErkJggg==");
  private static final String VALID="""
      {"title":"Căn hộ kiểm thử hai phòng ngủ","description":"Căn hộ có nội thất cơ bản, ban công rộng, ảnh minh chứng thực tế và tiện ích đầy đủ.",
       "city":"Hà Nội","district":"Cầu Giấy","address":"Trần Thái Tông","type":"APARTMENT",
       "monthlyRent":8500000,"area":65,"bedrooms":2,"imageUrl":""}
      """;
  private String body(Map<String,Object> m)throws Exception{return mapper.writeValueAsString(m);}
  private JsonNode parse(String s)throws Exception{return mapper.readTree(s);}
  private String login(String email,String password)throws Exception{
    var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn();
    return parse(r.getResponse().getContentAsString()).path("token").asText();
  }
  private String admin()throws Exception{return login("admin@easyhome.vn","Admin@12345");}
  private String mod()throws Exception{return login("mod@easyhome.vn","Mod@123456");}
  private JsonNode postJson(String path,String token,Object content)throws Exception{
    var r=mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).header("Authorization","Bearer "+token)
      .content(mapper.writeValueAsBytes(content))).andExpect(status().isOk()).andReturn();
    return parse(r.getResponse().getContentAsString());
  }
  private JsonNode action(String path,String token)throws Exception{
    var r=mvc.perform(post(path).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn();
    return parse(r.getResponse().getContentAsString());
  }
  private String registerAndVerify()throws Exception{
    String id=UUID.randomUUID().toString().replace("-","");
    String email="t"+id+"@example.com";String phone="09"+String.format("%08d",Math.floorMod(id.hashCode(),100_000_000));
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    PrintStream previous=System.out;
    try{
      System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));
      mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("fullName","User Test","email",email,"phone",phone,"password","Test@12345","publisherType","OWNER"))))
        .andExpect(status().isOk());
    }finally{System.setOut(previous);}
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
       .content(body(Map.of("email",email,"password","Test@12345")))).andExpect(status().isForbidden());
    Matcher m=Pattern.compile("=> (\\d{6})").matcher(output.toString(StandardCharsets.UTF_8));
    assertTrue(m.find(),"Dev-only email OTP must appear in test output");
    mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
       .content(body(Map.of("email",email,"code",m.group(1))))).andExpect(status().isOk());
    return login(email,"Test@12345");
  }
  private void image(String token,long id,String purpose)throws Exception{
    mvc.perform(multipart("/api/my/listings/"+id+"/media").file(new MockMultipartFile("file","image.png","image/png",PNG))
       .param("purpose",purpose).header("Authorization","Bearer "+token)).andExpect(status().isOk());
  }
  private long create(String token)throws Exception{return postJson("/api/my/listings",token,parse(VALID)).path("id").asLong();}
  private long prepare(String token)throws Exception{
    long id=create(token);
    image(token,id,"PUBLIC");image(token,id,"PUBLIC");image(token,id,"EVIDENCE");return id;
  }
  private long order(String token,long listingId,String plan)throws Exception{
    return postJson("/api/orders",token,Map.of("listingId",listingId,"plan",plan)).path("id").asLong();
  }
  @Test void otpEmailVerifiedAndPhoneUnique()throws Exception{
    String id=UUID.randomUUID().toString().replace("-","");
    String email="check"+id+"@example.com";
    String phone="0901234567";
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("fullName","Otp user","email",email,"phone",phone,"password","Test@12345","publisherType","OWNER"))))
      .andExpect(status().isOk());
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("fullName","Other user","email","duplicate"+id+"@example.com","phone","+84901234567","password","Test@12345","publisherType","OWNER"))))
      .andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("email",email,"code","000000")))).andExpect(status().isBadRequest());
  }
  @Test void identityThenPayThenApproveAndPublicVisible()throws Exception{
    String token=registerAndVerify(),admin=admin(),moderator=mod();
    long initialId=create(token);
    mvc.perform(post("/api/orders").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("listingId",initialId,"plan","VIP_GOLD")))).andExpect(status().isBadRequest());
    long identityId=action("/api/identity/start",token).path("id").asLong();
    for(int i=0;i<2;i++)mvc.perform(multipart("/api/identity/"+identityId+"/upload").file(new MockMultipartFile("file","card.png","image/png",PNG))
      .header("Authorization","Bearer "+token)).andExpect(status().isOk());
    action("/api/identity/"+identityId+"/submit",token);
    postJson("/api/admin/identity/"+identityId+"/review",admin,Map.of("decision","VERIFIED","note","Đã kiểm tra giấy tờ thủ công"));
    long id=prepare(token);
    long order=order(token,id,"VIP_GOLD");
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    assertEquals("PAID",action("/api/dev/orders/"+order+"/confirm",token).path("status").asText());
    assertEquals("PAID",action("/api/dev/orders/"+order+"/confirm",token).path("status").asText());
    assertEquals("PENDING_REVIEW",action("/api/my/listings/"+id+"/submit",token).path("status").asText());
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/admin/listings/"+id+"/moderation-detail").header("Authorization","Bearer "+moderator)).andExpect(status().isOk())
      .andExpect(jsonPath("$.media.length()").value(3));
    assertEquals("ACTIVE",action("/api/admin/listings/"+id+"/approve",moderator).path("status").asText());
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("VIP_GOLD"));
  }
  @Test void rejectReturnsMockMoneyExactlyOnce()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"),moderator=mod();
    long id=prepare(owner),order=order(owner,id,"STANDARD");
    action("/api/dev/orders/"+order+"/confirm",owner);
    action("/api/my/listings/"+id+"/submit",owner);
    var rejected=postJson("/api/admin/listings/"+id+"/reject",moderator,Map.of("note","Ảnh minh chứng chưa chứng minh hiện trạng cho thuê"));
    assertEquals("REJECTED",rejected.path("status").asText());
    var bal=mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andReturn();
    assertEquals(29000,parse(bal.getResponse().getContentAsString()).path("demoAvailable").asInt());
    mvc.perform(post("/api/admin/listings/"+id+"/reject").header("Authorization","Bearer "+moderator)
      .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note","Thử lặp lại yêu cầu từ chối"))))
      .andExpect(status().isBadRequest());
    var again=mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andReturn();
    assertEquals(29000,parse(again.getResponse().getContentAsString()).path("demoAvailable").asInt());
  }
  @Test void freeOnceAfterActualApproval()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"),moderator=mod();
    long first=prepare(owner),second=prepare(owner);
    order(owner,first,"FREE");action("/api/my/listings/"+first+"/submit",owner);
    mvc.perform(post("/api/orders").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("listingId",second,"plan","FREE")))).andExpect(status().isBadRequest());
    action("/api/admin/listings/"+first+"/approve",moderator);
    mvc.perform(post("/api/orders").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("listingId",second,"plan","FREE")))).andExpect(status().isBadRequest());
  }
  @Test void locksHaveHistoryAndUnlockAllowsLogin()throws Exception{
    String owner=login("user@easyhome.vn","User@123456"),admin=admin();
    Long id=users.findByEmailIgnoreCase("user@easyhome.vn").orElseThrow().id;
    postJson("/api/admin/users/"+id+"/lock",admin,Map.of("reason","Có dấu hiệu gửi spam liên hệ nhiều lần","days",7));
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("email","user@easyhome.vn","password","User@123456")))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/users/"+id+"/lock-history").header("Authorization","Bearer "+admin)).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].action").value("LOCK"));
    postJson("/api/admin/users/"+id+"/unlock",admin,Map.of("reason","Đã xác minh và xử lý khiếu nại"));
    login("user@easyhome.vn","User@123456");
  }
}
