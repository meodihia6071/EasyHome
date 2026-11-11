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

public interface Orders extends JpaRepository<ServiceOrder,Long> {
  Optional<ServiceOrder> findFirstByListing_IdAndStatusOrderByIdDesc(Long listingId, PaymentStatus status);
  boolean existsByBuyer_IdAndPlanAndStatus(Long userId,ListingPlan plan,PaymentStatus status);
  List<ServiceOrder> findByBuyer_IdOrderByCreatedAtDesc(Long userId);
  List<ServiceOrder> findByBuyer_IdAndPlanAndStatus(Long userId,ListingPlan plan,PaymentStatus status);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from ServiceOrder o where o.id=:id") Optional<ServiceOrder> locked(@Param("id") Long id);
}
