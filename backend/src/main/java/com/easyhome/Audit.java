package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_logs")
public class Audit {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false) public Long actorId;
  @Column(nullable=false,length=70) public String action;
  @Column(nullable=false,length=80) public String target;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  protected Audit() {}
  Audit(Long actorId,String action,String target){this.actorId=actorId;this.action=action;this.target=target;}
}
