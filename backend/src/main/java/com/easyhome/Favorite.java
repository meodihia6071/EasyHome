package com.easyhome;
import com.easyhome.Rules.*;

import jakarta.persistence.*;
@Entity @Table(name="favorites",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","listing_id"}))
public class Favorite {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") public User user;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="listing_id") public Listing listing;
  protected Favorite() {}
}
