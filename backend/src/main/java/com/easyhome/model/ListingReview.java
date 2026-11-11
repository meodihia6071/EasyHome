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

@Entity @Table(name="listing_reviews")
public class ListingReview {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public Listing listing;
  @Column(nullable=false) public int revision;
  public Long reviewerId;
  @Column(nullable=false,length=40) public String decision;
  @Column(length=500) public String note;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public ListingReview() {}
}
