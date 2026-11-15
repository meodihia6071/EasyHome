package com.easyhome.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "password_reset_tokens", indexes = @Index(name = "password_reset_hash", columnList = "token_hash", unique = true))
public class PasswordResetToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) public User user;
  @Column(name = "token_hash", nullable = false, length = 64, unique = true) public String tokenHash;
  @Column(nullable = false) public Instant expiresAt;
  @Column(nullable = false) public Instant createdAt = Instant.now();
  @Column(nullable = false) public int attempts = 0;
  @Column(nullable = false) public boolean consumed = false;
  public PasswordResetToken() {}
}
