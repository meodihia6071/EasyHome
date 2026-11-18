package com.easyhome.repository;

import com.easyhome.model.StaffLoginChallenge;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffLoginChallenges extends JpaRepository<StaffLoginChallenge,Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from StaffLoginChallenge c join fetch c.user where c.challengeHash=:hash")
  Optional<StaffLoginChallenge> lockedByHash(@Param("hash") String hash);
  List<StaffLoginChallenge> findByUser_IdAndConsumedFalse(Long userId);
  long countByUser_IdAndCreatedAtAfter(Long userId,Instant after);
  void deleteByExpiresAtBefore(Instant before);
}
