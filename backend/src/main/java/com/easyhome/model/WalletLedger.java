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

@Entity @Table(name="wallet_ledger",uniqueConstraints=@UniqueConstraint(columnNames="reference"))
public class WalletLedger {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public WalletAccount wallet;
  @Column(nullable=false) public long delta;
  @Column(nullable=false,length=36) public String action;
  @Column(nullable=false,length=110) public String reference;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public WalletLedger() {}
}
