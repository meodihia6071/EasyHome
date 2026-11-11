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
  public Instant reviewRequestedAt;
  public Instant needsUpdateDeadline;
  @Column(nullable=false) public int reviewRevision=0;
  @Column(name="expires_at") public Instant expiresAt;
  // Consistent access from Hibernate proxies (favorites, orders, leads, reports).
  public Long getId() { return id; }
  public String getTitle() { return title; }
  public Property getProperty() { return property; }
  public User getOwner() { return owner; }
  public String getDescription() { return description; }
  public ListingStatus getStatus() { return status; }
  /** Proxy-safe update. Access via method, not direct field write from controllers. */
  public void setStatus(ListingStatus next) { this.status=java.util.Objects.requireNonNull(next); }
  public ListingPlan getPlan() { return plan; }
  public int getPriority() { return priority; }
  public Instant getPublishedAt() { return publishedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public String getReviewNote() { return reviewNote; }
  public Instant getCreatedAt() { return createdAt; }
  /**
   * Apply a paid listing package to the managed entity. Invoking a method instead
   * of writing fields directly works when Hibernate returns a lazy proxy.
   * The order snapshots (not today's catalogue price/duration) are authoritative.
   */
  public void activatePackage(ListingPlan packagePlan, int packagePriority, Instant approvedAt, int days) {
    this.status = Rules.activate(this.status, this.property.getAvailability());
    this.plan = packagePlan;
    this.priority = packagePriority;
    this.publishedAt = approvedAt;
    this.expiresAt = approvedAt.plus(days, java.time.temporal.ChronoUnit.DAYS);
  }
  public Listing() {}
}
