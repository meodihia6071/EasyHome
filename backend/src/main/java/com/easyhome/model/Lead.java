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

@Entity @Table(name="contact_leads")
public class Lead {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User requester;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public Listing listing;
  @Column(nullable=false,length=800) public String message;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Lead() {}
}
