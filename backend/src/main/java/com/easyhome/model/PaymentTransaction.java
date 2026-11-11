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

@Entity @Table(name="payment_transactions",uniqueConstraints=@UniqueConstraint(columnNames="reference"))
public class PaymentTransaction {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY) public ServiceOrder order;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User payer;
  @Column(nullable=false,length=90) public String reference;
  @Column(nullable=false) public long amount;
  @Column(nullable=false,length=30) public String method;
  @Column(nullable=false,length=30) public String status="PENDING";
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Instant confirmedAt;
  public PaymentTransaction() {}
}
