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

@Entity @Table(name="audit_logs")
public class Audit {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false) public Long actorId;
  @Column(nullable=false,length=70) public String action;
  @Column(nullable=false,length=80) public String target;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  public Audit() {}
  public Audit(Long actorId,String action,String target){this.actorId=actorId;this.action=action;this.target=target;}
}
