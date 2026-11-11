package com.easyhome.config;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration @EnableWebSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
  @Bean JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret,@Value("${app.seed-demo}") boolean demo){
    if(secret.length()<32 || (!demo && (secret.contains("local-demo-only")||secret.startsWith("replace-"))))throw new IllegalStateException("JWT_SECRET must be set before non-demo deployment");
    return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
  }
  @Bean JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret){
    SecretKey key=new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
  @Bean CorsConfigurationSource cors(){
    CorsConfiguration c=new CorsConfiguration();
    c.setAllowedOrigins(List.of("http://localhost:5173","http://127.0.0.1:5173"));
    c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type"));
    UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
  }
  @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
    return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a
        .requestMatchers("/api/auth/register","/api/auth/login","/api/auth/verify-email","/api/auth/resend-email","/error").permitAll()
        .requestMatchers(HttpMethod.GET,"/api/listings","/api/listings/{id}","/api/plans","/api/media/{id}").permitAll()
        .anyRequest().authenticated())
      .oauth2ResourceServer(o->o.jwt(j->{})).build();
  }
}
