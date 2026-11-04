package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="listings",indexes={@Index(name="listing_status_expiry",columnList="status,expires_at")})
public class Listing {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public Property property;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User owner;
  @Column(nullable=false,length=160) public String title;
  @Column(nullable=false,length=3000) public String description;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public ListingStatus status=ListingStatus.DRAFT;
  @Enumerated(EnumType.STRING) public ListingPlan plan;
  @Column(nullable=false) public int priority=0;
  @Column(length=500) public String reviewNote;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Instant publishedAt;
  @Column(name="expires_at") public Instant expiresAt;
  protected Listing() {}
}
