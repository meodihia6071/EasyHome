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

@Entity @Table(name="service_plans",uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class ServicePlan {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false,length=32) public String code;
  @Column(nullable=false,length=100) public String label;
  @Column(nullable=false) public int price;
  @Column(nullable=false) public int days;
  @Column(nullable=false) public int priority;
  @Column(nullable=false) public boolean active=true;
  @Version public Long version;
  @Column(nullable=false) public Instant updatedAt=Instant.now();
  public ServicePlan() {}
}
