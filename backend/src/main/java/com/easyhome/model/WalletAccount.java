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

@Entity @Table(name="wallet_accounts")
public class WalletAccount {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",unique=true) public User user;
  @Column(nullable=false) public long demoBalance=0;
  @Column(nullable=false) public long demoHeld=0;
  @Version public Long version;
  public WalletAccount() {}
}
