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

@Entity @Table(name="listing_media")
public class ListingMedia {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY) public Listing listing;
  @ManyToOne(fetch=FetchType.LAZY) public IdentityVerification identityVerification;
  @Column(nullable=false,length=20) public String purpose; // PUBLIC, EVIDENCE, IDENTITY
  @Column(nullable=false,length=100) public String storageName;
  @Column(nullable=false,length=50) public String contentType;
  @Column(nullable=false) public long bytes;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public ListingMedia() {}
}
