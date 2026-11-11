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

@Entity @Table(name="users",uniqueConstraints={@UniqueConstraint(columnNames="email"),@UniqueConstraint(columnNames="phone")})
public class User {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false,length=120) public String fullName;
  @Column(nullable=false,length=190) public String email;
  @Column(nullable=false) public String passwordHash;
  @Column(nullable=false,length=25) public String phone;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public AccountRole role=AccountRole.USER;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public PublisherType publisherType=PublisherType.OWNER;
  @Column(nullable=false) public boolean verified=false; // backward compatibility, synchronized with identityStatus
  @Enumerated(EnumType.STRING) @Column(nullable=false,columnDefinition="varchar(32) default 'UNVERIFIED'") public IdentityStatus identityStatus=IdentityStatus.UNVERIFIED;
  @Column(nullable=false) public boolean emailVerified=false;
  @Column(nullable=false) public int dailyOtpSends=0;
  public Instant otpWindowStart;
  public Instant registrationExpiresAt;
  @Column(nullable=false) public boolean phoneVerified=false; // SMS verification reserved for later phase
  @Column(nullable=false) public boolean locked=false;
  public Instant lockedUntil;
  @Column(length=500) public String lockReason;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  // Access through methods when referenced by Hibernate lazy proxies.
  public Long getId() { return id; }
  public String getFullName() { return fullName; }
  public String getEmail() { return email; }
  public String getPhone() { return phone; }
  public PublisherType getPublisherType() { return publisherType; }
  // identityStatus is the authoritative business state; verified is retained for legacy data.
  public boolean isVerified() { return identityStatus==IdentityStatus.VERIFIED; }
  public IdentityStatus getIdentityStatus() { return identityStatus; }
  /** Use an entity method for state changes: safe even when Hibernate supplies a lazy proxy. */
  public void setIdentityStatus(IdentityStatus next) {
    this.identityStatus=java.util.Objects.requireNonNull(next);
    this.verified=next==IdentityStatus.VERIFIED;
  }
  public boolean isEmailVerified() { return emailVerified; }
  public User() {}
  public User(String name,String email,String hash,String phone,PublisherType publisherType) {
    this.fullName=name;this.email=email;this.passwordHash=hash;this.phone=phone;this.publisherType=publisherType;
  }
}
