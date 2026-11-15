package com.easyhome.repository;
import com.easyhome.common.Rules;
import com.easyhome.common.Rules.*;
import com.easyhome.common.RulesV2;
import com.easyhome.model.*;
import com.easyhome.repository.*;
import com.easyhome.repository.Properties;
import com.easyhome.service.*;
import com.easyhome.api.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;

public interface Listings extends JpaRepository<Listing,Long>,JpaSpecificationExecutor<Listing> {
  List<Listing> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
  List<Listing> findByStatusOrderByCreatedAtAsc(ListingStatus status);
  List<Listing> findByStatusAndExpiresAtBefore(ListingStatus status,Instant now);
  List<Listing> findByStatusAndNeedsUpdateDeadlineBefore(ListingStatus status,Instant now);
  @Query("select distinct p.city from Listing l join l.property p where l.status=:status and l.expiresAt>:now and p.availability=:availability order by p.city")
  List<String> findActiveCities(@Param("status") ListingStatus status,@Param("availability") Availability availability,@Param("now") Instant now);
  @Query("select distinct p.district from Listing l join l.property p where l.status=:status and l.expiresAt>:now and p.availability=:availability order by p.district")
  List<String> findActiveDistricts(@Param("status") ListingStatus status,@Param("availability") Availability availability,@Param("now") Instant now);
  boolean existsByProperty_IdAndStatusIn(Long propertyId, Collection<ListingStatus> statuses);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from Listing l where l.id=:id") Optional<Listing> locked(@Param("id") Long id);
}
