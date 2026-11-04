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
  // JPA/Hibernate proxies intercept methods, not direct public-field reads or writes.
  public Long getId() { return id; }
  public String getCity() { return city; }
  public String getDistrict() { return district; }
  public String getAddress() { return address; }
  public String getType() { return type; }
  public BigDecimal getMonthlyRent() { return monthlyRent; }
  public Double getArea() { return area; }
  public Integer getBedrooms() { return bedrooms; }
  public String getImageUrl() { return imageUrl; }
  public Availability getAvailability() { return availability; }
  public void setAvailability(Availability value) { this.availability = value; }
  public void updateDetails(String city, String district, String address, String type,
                            BigDecimal monthlyRent, Double area, Integer bedrooms, String imageUrl) {
    this.city = city; this.district = district; this.address = address; this.type = type;
    this.monthlyRent = monthlyRent; this.area = area; this.bedrooms = bedrooms; this.imageUrl = imageUrl;
  }
  protected Property() {}
}
