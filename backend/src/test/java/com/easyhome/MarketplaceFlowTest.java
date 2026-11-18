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
import com.fasterxml.jackson.databind.node.ObjectNode;
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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:easyhome_v2;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.seed-demo=true", "app.mock-payments=true", "app.email-mode=console", "app.staff-login-otp.max-daily-sends=100"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired Listings listings;
  @Autowired Wallets wallets;
  @Autowired Users users;
  @Autowired StaffLoginChallenges staffLoginChallenges;
  private static final byte[] PNG=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGNcUJHCwMDAxMDAwMDAAAASNgGAoiIFpwAAAABJRU5ErkJggg==");
  private static final String VALID="""
      {"title":"Căn hộ kiểm thử hai phòng ngủ","description":"Căn hộ có nội thất cơ bản, ban công rộng, ảnh minh chứng thực tế và tiện ích đầy đủ.",
       "city":"Hà Nội","district":"Cầu Giấy","address":"Trần Thái Tông","type":"APARTMENT",
       "monthlyRent":8500000,"area":65,"bedrooms":2,"imageUrl":""}
      """;
  private String body(Map<String,Object> m)throws Exception{return mapper.writeValueAsString(m);}
  private JsonNode parse(String s)throws Exception{return mapper.readTree(s);}
  private String login(String email,String password)throws Exception{
    ByteArrayOutputStream output=new ByteArrayOutputStream();PrintStream previous=System.out;String response;
    try{
      System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));
      var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
          .content(body(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn();
      response=r.getResponse().getContentAsString();
    }finally{System.setOut(previous);}
    JsonNode result=parse(response);
    if(!result.path("requiresOtp").asBoolean())return result.path("token").asText();
    Matcher otp=Pattern.compile("EASYHOME STAFF LOGIN OTP.*?=> (\\d{6})").matcher(output.toString(StandardCharsets.UTF_8));
    assertTrue(otp.find(),"Staff login must send a one-time code");
    var verified=mvc.perform(post("/api/auth/login/verify-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",result.path("challengeId").asText(),"code",otp.group(1))))).andExpect(status().isOk()).andReturn();
    return parse(verified.getResponse().getContentAsString()).path("token").asText();
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
    var verified=mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
       .content(body(Map.of("email",email,"code",m.group(1))))).andExpect(status().isOk()).andReturn();
    String session=parse(verified.getResponse().getContentAsString()).path("token").asText();
    assertFalse(session.isBlank(),"Email verification should sign the user in");
    return session;
  }
  private long image(String token,long id,String purpose)throws Exception{
    var r=mvc.perform(multipart("/api/my/listings/"+id+"/media").file(new MockMultipartFile("file","image.png","image/png",PNG))
       .param("purpose",purpose).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn();
    return parse(r.getResponse().getContentAsString()).path("id").asLong();
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
  @Test void staffLoginRequiresOneTimeEmailOtp()throws Exception{
    ByteArrayOutputStream output=new ByteArrayOutputStream();PrintStream previous=System.out;JsonNode pending;
    try{
      System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));
      var response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("email","admin@easyhome.vn","password","Admin@12345")))).andExpect(status().isOk()).andReturn();
      pending=parse(response.getResponse().getContentAsString());
    }finally{System.setOut(previous);}
    assertTrue(pending.path("requiresOtp").asBoolean());assertTrue(pending.path("token").isMissingNode());
    Matcher otp=Pattern.compile("EASYHOME STAFF LOGIN OTP.*?=> (\\d{6})").matcher(output.toString(StandardCharsets.UTF_8));assertTrue(otp.find());
    String challenge=pending.path("challengeId").asText();
    String wrongCode=otp.group(1).equals("000000")?"000001":"000000";
    mvc.perform(post("/api/auth/login/verify-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",challenge,"code",wrongCode)))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/login/resend-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",challenge)))).andExpect(status().isBadRequest());
    StaffLoginChallenge stored=staffLoginChallenges.findByUser_IdAndConsumedFalse(users.findByEmailIgnoreCase("admin@easyhome.vn").orElseThrow().id).get(0);
    stored.resendAt=java.time.Instant.now().minusSeconds(1);staffLoginChallenges.save(stored);
    ByteArrayOutputStream resentOutput=new ByteArrayOutputStream();PrintStream resendPrevious=System.out;JsonNode resent;
    try{
      System.setOut(new PrintStream(resentOutput,true,StandardCharsets.UTF_8));
      var response=mvc.perform(post("/api/auth/login/resend-otp").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("challengeId",challenge)))).andExpect(status().isOk()).andReturn();
      resent=parse(response.getResponse().getContentAsString());
    }finally{System.setOut(resendPrevious);}
    Matcher resentOtp=Pattern.compile("EASYHOME STAFF LOGIN OTP.*?=> (\\d{6})").matcher(resentOutput.toString(StandardCharsets.UTF_8));assertTrue(resentOtp.find());
    assertNotEquals(challenge,resent.path("challengeId").asText());
    mvc.perform(post("/api/auth/login/verify-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",challenge,"code",otp.group(1))))).andExpect(status().isBadRequest());
    var verified=mvc.perform(post("/api/auth/login/verify-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",resent.path("challengeId").asText(),"code",resentOtp.group(1))))).andExpect(status().isOk()).andReturn();
    assertFalse(parse(verified.getResponse().getContentAsString()).path("token").asText().isBlank());
    mvc.perform(post("/api/auth/login/verify-otp").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("challengeId",resent.path("challengeId").asText(),"code",resentOtp.group(1))))).andExpect(status().isBadRequest());
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
    mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk())
      .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.identityStatus").value("VERIFIED"));
    long id=prepare(token);
    long order=order(token,id,"VIP_GOLD");
    assertEquals(ListingStatus.AWAITING_PAYMENT,listings.findById(id).orElseThrow().getStatus());
    mvc.perform(get("/api/my/listings").header("Authorization","Bearer "+token)).andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.id=="+id+")].status").value(org.hamcrest.Matchers.hasItem("AWAITING_PAYMENT")));
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
  @Test void identityNeedsUpdateIsVisibleAndPresentInAdminAudit()throws Exception{
    String owner=registerAndVerify(),admin=admin();long id=action("/api/identity/start",owner).path("id").asLong();
    for(int i=0;i<2;i++)mvc.perform(multipart("/api/identity/"+id+"/upload").file(new MockMultipartFile("file","card.png","image/png",PNG))
      .header("Authorization","Bearer "+owner)).andExpect(status().isOk());
    action("/api/identity/"+id+"/submit",owner);
    postJson("/api/admin/identity/"+id+"/review",admin,Map.of("decision","NEEDS_UPDATE","note","Ảnh mặt sau chưa đủ rõ, vui lòng chụp lại"));
    mvc.perform(get("/api/identity/me").header("Authorization","Bearer "+owner)).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("NEEDS_UPDATE")).andExpect(jsonPath("$.note").value("Ảnh mặt sau chưa đủ rõ, vui lòng chụp lại"));
    mvc.perform(get("/api/admin/audit").param("action","IDENTITY_NEEDS_UPDATE").param("target","identity:"+id)
      .header("Authorization","Bearer "+admin)).andExpect(status().isOk())
      .andExpect(jsonPath("$.content[?(@.action=='IDENTITY_NEEDS_UPDATE' && @.target=='identity:"+id+"')].details")
        .value(org.hamcrest.Matchers.hasItem("Ảnh mặt sau chưa đủ rõ, vui lòng chụp lại")));
  }
  @Test void listingReviewHistoryIncludesStaffNoteAndOwnerScope()throws Exception{
    String owner=registerAndVerify(),admin=admin(),moderator=mod();long identityId=action("/api/identity/start",owner).path("id").asLong();
    for(int i=0;i<2;i++)mvc.perform(multipart("/api/identity/"+identityId+"/upload").file(new MockMultipartFile("file","card.png","image/png",PNG))
      .header("Authorization","Bearer "+owner)).andExpect(status().isOk());
    action("/api/identity/"+identityId+"/submit",owner);
    postJson("/api/admin/identity/"+identityId+"/review",admin,Map.of("decision","VERIFIED","note","Đã đối chiếu thông tin hồ sơ"));
    long id=prepare(owner),order=order(owner,id,"STANDARD");action("/api/dev/orders/"+order+"/confirm",owner);action("/api/my/listings/"+id+"/submit",owner);
    String note="Ảnh minh chứng chưa thể hiện rõ địa chỉ bất động sản";
    postJson("/api/admin/listings/"+id+"/needs-update",moderator,Map.of("note",note));
    mvc.perform(get("/api/my/listings/"+id+"/history").header("Authorization","Bearer "+owner)).andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.action=='LISTING_NEEDS_UPDATE')].details").value(org.hamcrest.Matchers.hasItem(note)))
      .andExpect(jsonPath("$[?(@.action=='LISTING_NEEDS_UPDATE')].actorName").value(org.hamcrest.Matchers.hasItem("Bộ phận kiểm duyệt")));
    String other=registerAndVerify();
    mvc.perform(get("/api/my/listings/"+id+"/history").header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
    mvc.perform(get("/api/staff/audit").param("action","LISTING_NEEDS_UPDATE").header("Authorization","Bearer "+moderator))
      .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.target=='listing:"+id+"')].details").value(org.hamcrest.Matchers.hasItem(note)));
    mvc.perform(get("/api/staff/audit").param("actorId","1").param("action","IDENTITY_NEEDS_UPDATE").header("Authorization","Bearer "+moderator))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  }
  @Test void rejectRefundsApprovedWalletPaymentExactlyOnce()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"),moderator=mod(),admin=admin();
    mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk());
    WalletAccount testWallet=wallets.findByUser_Id(users.findByEmailIgnoreCase("owner@easyhome.vn").orElseThrow().id).orElseThrow();
    testWallet.availableBalance=0;wallets.saveAndFlush(testWallet);
    long id=prepare(owner),order=order(owner,id,"STANDARD");
    assertEquals(ListingStatus.AWAITING_PAYMENT,listings.findById(id).orElseThrow().getStatus());
    JsonNode deposit=postJson("/api/wallet/deposits",owner,Map.of("amount",29000));
    postJson("/api/wallet/deposits/"+deposit.path("id").asLong()+"/submitted",owner,Map.of());
    postJson("/api/admin/wallet/deposits/"+deposit.path("id").asLong()+"/approve",admin,Map.of());
    action("/api/orders/"+order+"/wallet-pay",owner);
    action("/api/my/listings/"+id+"/submit",owner);
    var rejected=postJson("/api/admin/listings/"+id+"/reject",moderator,Map.of("note","Ảnh minh chứng chưa chứng minh hiện trạng cho thuê"));
    assertEquals("REJECTED",rejected.path("status").asText());
    var bal=mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andReturn();
    assertEquals(29000,parse(bal.getResponse().getContentAsString()).path("available").asInt());
    mvc.perform(post("/api/admin/listings/"+id+"/reject").header("Authorization","Bearer "+moderator)
      .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note","Thử lặp lại yêu cầu từ chối"))))
      .andExpect(status().isBadRequest());
    var again=mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andReturn();
    assertEquals(29000,parse(again.getResponse().getContentAsString()).path("available").asInt());
  }
  @Test void rejectingLegacyMockPaymentDoesNotCreateWithdrawableFunds()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"),moderator=mod();
    mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk());
    WalletAccount account=wallets.findByUser_Id(users.findByEmailIgnoreCase("owner@easyhome.vn").orElseThrow().id).orElseThrow();
    account.availableBalance=0;wallets.saveAndFlush(account);
    long id=prepare(owner),order=order(owner,id,"STANDARD");
    action("/api/dev/orders/"+order+"/confirm",owner);action("/api/my/listings/"+id+"/submit",owner);
    postJson("/api/admin/listings/"+id+"/reject",moderator,Map.of("note","Từ chối khoản thanh toán giả lập cũ"));
    assertEquals(0,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
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
    mvc.perform(get("/api/auth/session-status").header("Authorization","Bearer "+owner)).andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
      .andExpect(jsonPath("$.reason").value("Có dấu hiệu gửi spam liên hệ nhiều lần"))
      .andExpect(jsonPath("$.until").isNotEmpty());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("email","user@easyhome.vn","password","User@123456")))).andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    mvc.perform(get("/api/admin/users/"+id+"/lock-history").header("Authorization","Bearer "+admin)).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].action").value("LOCK"));
    postJson("/api/admin/users/"+id+"/unlock",admin,Map.of("reason","Đã xác minh và xử lý khiếu nại"));
    login("user@easyhome.vn","User@123456");
  }

  @Test void privateEvidenceRequiresOwnerOrAuthorizedStaff()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"),other=login("user@easyhome.vn","User@123456");
    long id=create(owner),privateImage=image(owner,id,"EVIDENCE");
    mvc.perform(get("/api/media/"+privateImage)).andExpect(status().isForbidden());
    mvc.perform(get("/api/media/"+privateImage).header("Authorization","Bearer "+other))
      .andExpect(status().isForbidden());
    mvc.perform(get("/api/media/"+privateImage).header("Authorization","Bearer "+owner))
      .andExpect(status().isOk());
    mvc.perform(get("/api/media/"+privateImage).header("Authorization","Bearer "+mod()))
      .andExpect(status().isOk());
  }
  @Test void publicMediaBecomesAnonymousReadableOnlyAfterApproval()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345"), moderator=mod();
    long listingId=create(owner);
    long publicImage=image(owner,listingId,"PUBLIC");
    image(owner,listingId,"PUBLIC");
    image(owner,listingId,"EVIDENCE");
    mvc.perform(get("/api/media/"+publicImage)).andExpect(status().isForbidden());
    long orderId=order(owner,listingId,"STANDARD");
    action("/api/dev/orders/"+orderId+"/confirm",owner);
    action("/api/my/listings/"+listingId+"/submit",owner);
    mvc.perform(get("/api/media/"+publicImage)).andExpect(status().isForbidden());
    action("/api/admin/listings/"+listingId+"/approve",moderator);
    mvc.perform(get("/api/media/"+publicImage)).andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }
  @Test void onlyStaffMayApproveListings()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345");
    long id=prepare(owner),orderId=order(owner,id,"STANDARD");
    action("/api/dev/orders/"+orderId+"/confirm",owner);
    action("/api/my/listings/"+id+"/submit",owner);
    mvc.perform(post("/api/admin/listings/"+id+"/approve")
      .header("Authorization","Bearer "+owner)).andExpect(status().isForbidden());
    assertEquals(ListingStatus.PENDING_REVIEW,listings.findById(id).orElseThrow().getStatus());
  }
  @Test void mockTopupDoesNotExposeDemoLedgerEntry()throws Exception{
    String user=registerAndVerify();
    postJson("/api/dev/wallet/topup",user,Map.of("amount",32000));
    var r=mvc.perform(get("/api/wallet").header("Authorization","Bearer "+user))
      .andExpect(status().isOk()).andReturn();
    assertEquals(32000,parse(r.getResponse().getContentAsString()).path("available").asInt());
    mvc.perform(get("/api/wallet/ledger").header("Authorization","Bearer "+user))
      .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
  }

  @SuppressWarnings("deprecation")
  @Test void VietQrDepositAndWithdrawalChangeBalanceOnlyAfterAdminDecision()throws Exception{
    String unique=UUID.randomUUID().toString().replace("-","");String email="wallet"+unique+"@example.com";
    ByteArrayOutputStream output=new ByteArrayOutputStream();PrintStream previous=System.out;
    try{System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));
      mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("fullName","Wallet Test","email",email,"phone","09"+String.format("%08d",Math.floorMod(unique.hashCode(),100_000_000)),"password","Test@12345")))).andExpect(status().isOk());
      Matcher otp=Pattern.compile("=> (\\d{6})").matcher(output.toString(StandardCharsets.UTF_8));assertTrue(otp.find());
      mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email",email,"code",otp.group(1))))).andExpect(status().isOk());
    }finally{System.setOut(previous);}
    User walletUser=users.findByEmailIgnoreCase(email).orElseThrow();walletUser.setIdentityStatus(IdentityStatus.VERIFIED);users.saveAndFlush(walletUser);
    String owner=login(email,"Test@12345"),admin=admin();
    mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andExpect(status().isOk());
    WalletAccount legacy=wallets.findByUser_Id(walletUser.id).orElseThrow();legacy.demoBalance=90000;wallets.saveAndFlush(legacy);
    assertEquals(0,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
    JsonNode deposit=postJson("/api/wallet/deposits",owner,Map.of("amount",50000));long depositId=deposit.path("id").asLong();
    assertEquals("PENDING",deposit.path("status").asText());
    assertEquals(0,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
    mvc.perform(get("/api/admin/wallet/deposits").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    postJson("/api/wallet/deposits/"+depositId+"/submitted",owner,Map.of());
    mvc.perform(get("/api/admin/wallet/deposits").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(depositId));
    postJson("/api/admin/wallet/deposits/"+depositId+"/approve",admin,Map.of());
    mvc.perform(post("/api/admin/wallet/deposits/"+depositId+"/approve").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
    assertEquals(50000,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
    JsonNode withdrawal=postJson("/api/wallet/withdrawals",owner,Map.of("amount",20000,"bankName","MB Bank","accountNumber","0919098606","accountHolder","NGUYEN HA KHOA"));
    assertEquals("PENDING",withdrawal.path("status").asText());
    assertEquals(50000,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
    postJson("/api/finance/withdrawals/"+withdrawal.path("id").asLong()+"/decision",admin,Map.of("action","APPROVE","reason","Admin đã xác nhận chuyển khoản"));
    assertEquals(30000,parse(mvc.perform(get("/api/wallet").header("Authorization","Bearer "+owner)).andReturn().getResponse().getContentAsString()).path("available").asInt());
  }

  @Test void publisherTypeBelongsToListingAndActiveLocationsAreSuggested()throws Exception{
    String owner=login("owner@easyhome.vn","Owner@12345");
    String city="Integration City "+UUID.randomUUID();
    ObjectNode listing=(ObjectNode)parse(VALID).deepCopy();listing.put("publisherType","BROKER");listing.put("city",city);
    JsonNode created=postJson("/api/my/listings",owner,listing);long id=created.path("id").asLong();
    assertEquals("BROKER",created.path("ownerType").asText());
    mvc.perform(get("/api/my/listings/"+id).header("Authorization","Bearer "+owner))
      .andExpect(status().isOk()).andExpect(jsonPath("$.publisherType").value("BROKER"));
    Listing active=listings.findById(id).orElseThrow();active.status=ListingStatus.ACTIVE;
    active.expiresAt=java.time.Instant.now().plusSeconds(86400);
    listings.save(active);
    JsonNode locations=parse(mvc.perform(get("/api/listings/locations")).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
    String[] cities=mapper.convertValue(locations.path("cities"),String[].class);
    assertTrue(Arrays.asList(cities).contains(city));
  }

  @Test void passwordResetLinkChangesPasswordAndCanOnlyBeUsedOnce()throws Exception{
    String id=UUID.randomUUID().toString().replace("-","");String email="reset"+id+"@example.com";
    ByteArrayOutputStream output=new ByteArrayOutputStream();PrintStream previous=System.out;String[] oldSession={null};
    try{
      System.setOut(new PrintStream(output,true,StandardCharsets.UTF_8));
      mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("fullName","Reset User","email",email,"phone","09"+String.format("%08d",Math.floorMod(id.hashCode(),100_000_000)),"password","Test@12345"))))
        .andExpect(status().isOk());
      Matcher otp=Pattern.compile("=> (\\d{6})").matcher(output.toString(StandardCharsets.UTF_8));
      assertTrue(otp.find());
      var verified=mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("email",email,"code",otp.group(1))))).andExpect(status().isOk()).andReturn();
      oldSession[0]=parse(verified.getResponse().getContentAsString()).path("token").asText();
      output.reset();
      mvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
        .content(body(Map.of("email",email)))).andExpect(status().isOk());
    }finally{System.setOut(previous);}
    Matcher link=Pattern.compile("/reset-password\\?token=([A-Za-z0-9_-]{40,})").matcher(output.toString(StandardCharsets.UTF_8));
    assertTrue(link.find(),"Dev mode should deliver the one-time reset link");String token=link.group(1);
    mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("token",token,"password","Changed@12345")))).andExpect(status().isOk());
    assertEquals(1,users.findByEmailIgnoreCase(email).orElseThrow().authVersion);
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("email",email,"password","Changed@12345")))).andExpect(status().isOk());
    mvc.perform(get("/api/auth/session-status").header("Authorization","Bearer "+oldSession[0])).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
      .content(body(Map.of("token",token,"password","Changed@54321")))).andExpect(status().isBadRequest());
  }
}
