package com.easyhome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:easyhome_tests;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true", "app.mock-payments=true"})
@AutoConfigureMockMvc
class MarketplaceFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired Listings listings;
  private static final String VALID="""
    {"title":"Căn hộ kiểm thử hai phòng ngủ", "description":"Căn hộ test có đầy đủ tiện ích cơ bản và cửa sổ rộng thoáng mát.",
     "city":"Hà Nội","district":"Cầu Giấy","address":"Đường Trần Thái Tông","type":"APARTMENT",
     "monthlyRent":8500000,"area":65,"bedrooms":2,"imageUrl":"https://example.com/home.jpg"}
    """;
  private MockHttpServletRequestBuilder req(String path,String token,String body){
    MockHttpServletRequestBuilder b=post(path).contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8).header("Authorization","Bearer "+token);
    return body==null?b:b.content(body.getBytes(StandardCharsets.UTF_8));
  }
  private JsonNode parse(String content)throws Exception {return json.readTree(content);}
  private String login(String email,String password)throws Exception {
    var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
      .content(json.writeValueAsString(java.util.Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn();
    return parse(result.getResponse().getContentAsString()).path("token").asText();
  }
  private String register()throws Exception {
    String email="tester_"+UUID.randomUUID().toString().replace("-","")+"@example.com";
    String body=json.writeValueAsString(java.util.Map.of("fullName","User Test","email",email,"phone","0912345678","password","Test@12345","publisherType","OWNER"));
    var response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
    return parse(response.getResponse().getContentAsString()).path("token").asText();
  }
  private long create(String owner)throws Exception {
    var response=mvc.perform(req("/api/my/listings",owner,VALID)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"))
      .andExpect(jsonPath("$.city").value("Hà Nội")).andReturn();
    return parse(response.getResponse().getContentAsString()).path("id").asLong();
  }
  private void review(long id,String owner,String moderator)throws Exception {
    mvc.perform(req("/api/my/listings/"+id+"/submit",owner,null)).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    mvc.perform(req("/api/admin/listings/"+id+"/approve",moderator,null)).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"));
  }
  @Test void paidListingRequiresReviewAndPaymentAndReplayIsSafe()throws Exception {
    String owner=register(),moderator=login("mod@easyhome.vn","Mod@123456");
    long id=create(owner);
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    review(id,owner,moderator);
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    String body=json.writeValueAsString(java.util.Map.of("listingId",id,"plan","VIP"));
    var checkout=mvc.perform(req("/api/orders",owner,body)).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("PENDING")).andReturn();
    long orderId=parse(checkout.getResponse().getContentAsString()).path("id").asLong();
    // Verify the review status really survived its transaction before payment.
    assertEquals(Rules.ListingStatus.AWAITING_PAYMENT, listings.findById(id).orElseThrow().getStatus());
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    mvc.perform(req("/api/dev/orders/"+orderId+"/confirm",owner,null)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    assertEquals(Rules.ListingStatus.ACTIVE, listings.findById(id).orElseThrow().getStatus());
    mvc.perform(req("/api/dev/orders/"+orderId+"/confirm",owner,null)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("VIP"));
  }
  @Test void freeBenefitLimitedAndRelistAfterClose()throws Exception {
    String owner=register(),moderator=login("mod@easyhome.vn","Mod@123456");
    long one=create(owner),two=create(owner);review(one,owner,moderator);review(two,owner,moderator);
    mvc.perform(req("/api/orders",owner,"{\"listingId\":"+one+",\"plan\":\"FREE\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    mvc.perform(req("/api/orders",owner,"{\"listingId\":"+two+",\"plan\":\"FREE\"}"))
      .andExpect(status().isBadRequest());
    mvc.perform(req("/api/my/listings/"+one+"/close",owner,null)).andExpect(status().isOk());
    mvc.perform(get("/api/listings/"+one)).andExpect(status().isNotFound());
    mvc.perform(req("/api/my/listings/"+one+"/relist",owner,null)).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("DRAFT"));
  }
  @Test void crossAccountOperationsBlockedAndInvalidListingRejected()throws Exception {
    String owner=register(),other=register();long id=create(owner);
    mvc.perform(req("/api/my/listings/"+id+"/submit",other,null)).andExpect(status().isForbidden());
    mvc.perform(req("/api/my/listings",owner,"{\"title\":\"bad\"}"))
      .andExpect(status().isBadRequest());
    mvc.perform(post("/api/my/listings").contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isUnauthorized());
  }
  @Test void lazyRelationsKeepListingDetailsAndContactData()throws Exception {
    String owner=register(),moderator=login("mod@easyhome.vn","Mod@123456"),renter=register();
    long id=create(owner);
    mvc.perform(get("/api/my/listings").header("Authorization","Bearer "+owner))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].city").value("Hà Nội"))
      .andExpect(jsonPath("$[0].ownerName").value("User Test"));
    review(id,owner,moderator);
    mvc.perform(req("/api/orders",owner,"{\"listingId\":"+id+",\"plan\":\"FREE\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isOk())
      .andExpect(jsonPath("$.city").value("Hà Nội"))
      .andExpect(jsonPath("$.ownerName").value("User Test"));
    mvc.perform(req("/api/my/favorites/"+id,renter,null)).andExpect(status().isOk());
    mvc.perform(get("/api/my/favorites").header("Authorization","Bearer "+renter))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].ownerName").value("User Test"));
    mvc.perform(req("/api/listings/"+id+"/contact",renter,"{\"message\":\"Tôi muốn tìm hiểu về căn hộ này\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Căn hộ kiểm thử hai phòng ngủ"));
    mvc.perform(get("/api/my/leads").header("Authorization","Bearer "+owner))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].sender").value("User Test"));
  }
  @Test void editedDraftPropertyDetailsSurviveReloadFromDatabase()throws Exception {
    String owner=register(),moderator=login("mod@easyhome.vn","Mod@123456");
    long id=create(owner);
    String revised=VALID.replace("Hà Nội","Đà Nẵng").replace("8500000","9200000");
    mvc.perform(put("/api/my/listings/"+id).header("Authorization","Bearer "+owner)
      .contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8)
      .content(revised.getBytes(StandardCharsets.UTF_8)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Đà Nẵng"));
    mvc.perform(get("/api/my/listings/"+id).header("Authorization","Bearer "+owner))
      .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Đà Nẵng"))
      .andExpect(jsonPath("$.monthlyRent").value(9200000));
    review(id,owner,moderator);
  }
}
