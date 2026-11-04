package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="properties")
public class Property {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) public User owner;
  @Column(nullable=false,length=100) public String city;
  @Column(nullable=false,length=100) public String district;
  @Column(nullable=false,length=240) public String address;
  @Column(nullable=false,length=25) public String type;
  @Column(nullable=false,precision=14,scale=0) public BigDecimal monthlyRent;
  @Column(nullable=false) public Double area;
  @Column(nullable=false) public Integer bedrooms;
  @Column(length=1000) public String imageUrl;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public Availability availability=Availability.AVAILABLE;
  protected Property() {}
}
