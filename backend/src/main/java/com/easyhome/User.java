package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="users",uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class User {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false,length=120) public String fullName;
  @Column(nullable=false,length=190) public String email;
  @Column(nullable=false) public String passwordHash;
  @Column(nullable=false,length=25) public String phone;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public AccountRole role=AccountRole.USER;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public PublisherType publisherType=PublisherType.OWNER;
  @Column(nullable=false) public boolean verified=false;
  @Column(nullable=false) public boolean locked=false;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  protected User() {}
  User(String name,String email,String hash,String phone,PublisherType publisherType) {
    this.fullName=name;this.email=email;this.passwordHash=hash;this.phone=phone;this.publisherType=publisherType;
  }
}
