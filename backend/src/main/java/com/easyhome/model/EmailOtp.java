package com.easyhome.model;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="email_otp")
public class EmailOtp {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) public User user;
  @Column(nullable=false,length=64) public String codeHash;
  @Column(nullable=false,length=40) public String nonce;
  @Column(nullable=false) public Instant expiresAt;
  @Column(nullable=false) public Instant resendAt;
  @Column(nullable=false) public int attempts=0;
  @Column(nullable=false) public boolean consumed=false;
  public EmailOtp() {}
}
