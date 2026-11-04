package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="service_orders", indexes=@Index(name="orders_listing_status",columnList="listing_id,status"))
public class ServiceOrder {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="listing_id") public Listing listing;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User buyer;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public ListingPlan plan;
  @Column(nullable=false) public int priceSnapshot;
  @Column(nullable=false) public int durationDaysSnapshot;
  @Column(nullable=false) public int prioritySnapshot;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public PaymentStatus status=PaymentStatus.PENDING;
  @Column(nullable=false,unique=true,length=80) public String reference;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Instant paidAt;
  protected ServiceOrder() {}
}
