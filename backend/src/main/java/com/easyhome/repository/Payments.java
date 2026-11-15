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

public interface Payments extends JpaRepository<PaymentTransaction,Long> {
  Optional<PaymentTransaction> findByReference(String reference);
  List<PaymentTransaction> findByMethodAndStatusOrderByCreatedAtAsc(String method,String status);
  List<PaymentTransaction> findByPayer_IdAndMethodOrderByCreatedAtDesc(Long userId,String method);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PaymentTransaction p where p.id=:id")
  Optional<PaymentTransaction> locked(@Param("id") Long id);
}
