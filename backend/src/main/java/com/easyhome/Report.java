package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="reports")
public class Report {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User reporter;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public Listing listing;
  @Column(nullable=false,length=800) public String reason;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public ReportStatus status=ReportStatus.OPEN;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  protected Report() {}
}
