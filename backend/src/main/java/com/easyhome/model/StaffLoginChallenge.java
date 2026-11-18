package com.easyhome.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="staff_login_challenges",uniqueConstraints=@UniqueConstraint(columnNames="challenge_hash"),
  indexes=@Index(name="staff_login_user_created",columnList="user_id,created_at"))
public class StaffLoginChallenge {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User user;
  @Column(name="challenge_hash",nullable=false,length=64) public String challengeHash;
  @Column(nullable=false,length=64) public String codeHash;
  @Column(nullable=false,length=40) public String nonce;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  @Column(nullable=false) public Instant expiresAt;
  @Column(nullable=false) public Instant resendAt;
  @Column(nullable=false) public int attempts=0;
  @Column(nullable=false) public boolean consumed=false;
  public StaffLoginChallenge() {}
}
