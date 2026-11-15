package com.easyhome.repository;

import com.easyhome.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokens extends JpaRepository<PasswordResetToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from PasswordResetToken t where t.tokenHash=:hash")
  Optional<PasswordResetToken> findLockedByTokenHash(@Param("hash") String hash);
  Optional<PasswordResetToken> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
  void deleteByUser_Id(Long userId);
}
