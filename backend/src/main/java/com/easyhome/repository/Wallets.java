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

public interface Wallets extends JpaRepository<WalletAccount,Long> {
  Optional<WalletAccount> findByUser_Id(Long userId);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from WalletAccount w where w.user.id=:id") Optional<WalletAccount> lockedByUser(@Param("id") Long userId);
}
