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

public interface Withdrawals extends JpaRepository<WithdrawalRequest,Long> {
  List<WithdrawalRequest> findByUser_IdOrderByCreatedAtDesc(Long id);
  List<WithdrawalRequest> findByStatus(String status);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from WithdrawalRequest w where w.id=:id") Optional<WithdrawalRequest> locked(@Param("id") Long id);
}
