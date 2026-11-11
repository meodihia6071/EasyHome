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

public interface Users extends JpaRepository<User,Long> {
  Optional<User> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
  boolean existsByPhone(String phone);
  List<User> findByLockedTrueAndLockedUntilBefore(Instant now);
  List<User> findByEmailVerifiedFalseAndRegistrationExpiresAtBefore(Instant now);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:id") Optional<User> locked(@Param("id") Long id);
}
