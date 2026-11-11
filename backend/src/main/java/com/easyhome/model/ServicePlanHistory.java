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

@Entity @Table(name="service_plan_history")
public class ServicePlanHistory {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false,length=32) public String planCode;
  @Column(nullable=false) public Long actorId;
  @Column(nullable=false) public int oldPrice;
  @Column(nullable=false) public int newPrice;
  @Column(nullable=false) public int oldDays;
  @Column(nullable=false) public int newDays;
  @Column(nullable=false) public Instant at=Instant.now();
  public ServicePlanHistory() {}
}
