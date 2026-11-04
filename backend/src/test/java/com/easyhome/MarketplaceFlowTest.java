package com.easyhome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
  private static final String VALID="""
    {"title":"Căn hộ kiểm thử hai phòng ngủ", "description":"Căn hộ test có đầy đủ tiện ích cơ bản và cửa sổ rộng thoáng mát.",
     "city":"Hà Nội","district":"Cầu Giấy","address":"Đường Trần Thái Tông","type":"APARTMENT",
     "monthlyRent":8500000,"area":65,"bedrooms":2,"imageUrl":"https://example.com/home.jpg"}
    """;
  private MockHttpServletRequestBuilder req(String path,String token,String body){
    MockHttpServletRequestBuilder b=post(path).contentType(MediaType.APPLICATION_JSON).header("Authorization","Bearer "+token);
    return body==null?b:b.content(body);
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
    var response=mvc.perform(req("/api/my/listings",owner,VALID)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
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
    mvc.perform(get("/api/listings/"+id)).andExpect(status().isNotFound());
    mvc.perform(req("/api/dev/orders/"+orderId+"/confirm",owner,null)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
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
}
