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

@Entity @Table(name="identity_verifications")
public class IdentityVerification {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User user;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public IdentityStatus status=IdentityStatus.PENDING;
  @Column(length=500) public String note;
  @Column(nullable=false) public Instant submittedAt=Instant.now();
  public Instant reviewedAt;
  public Long reviewerId;
  public User getUser() { return user; }
  public IdentityVerification() {}
}
