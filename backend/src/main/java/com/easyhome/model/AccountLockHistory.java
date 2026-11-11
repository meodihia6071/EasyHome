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

@Entity @Table(name="account_lock_history")
public class AccountLockHistory {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User user;
  @Column(nullable=false) public Long actorId;
  @Column(nullable=false,length=18) public String action;
  @Column(nullable=false,length=500) public String reason;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Instant untilAt;
  public AccountLockHistory() {}
}
