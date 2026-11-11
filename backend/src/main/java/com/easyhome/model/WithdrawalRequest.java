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

@Entity @Table(name="withdrawal_requests")
public class WithdrawalRequest {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User user;
  @Column(nullable=false) public long amount;
  @Column(nullable=false,length=80) public String bankName;
  @Column(nullable=false,length=50) public String accountNumber;
  @Column(nullable=false,length=120) public String accountHolder;
  @Column(nullable=false,length=30) public String status="PENDING";
  @Column(length=500) public String note;
  public Long reviewerId;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Instant decidedAt;
  public WithdrawalRequest() {}
}
