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

public interface ServicePlans extends JpaRepository<ServicePlan,Long> {
  Optional<ServicePlan> findByCode(String code);
  List<ServicePlan> findByActiveTrueOrderByPriorityAsc();
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from ServicePlan p where p.code=:code") Optional<ServicePlan> locked(@Param("code") String code);
}
