package com.easyhome;
import com.easyhome.Rules.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;

interface Users extends JpaRepository<User,Long> {
  Optional<User> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:id") Optional<User> locked(@Param("id") Long id);
}
interface Properties extends JpaRepository<Property,Long> {}
interface Listings extends JpaRepository<Listing,Long>,JpaSpecificationExecutor<Listing> {
  List<Listing> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
  List<Listing> findByStatusOrderByCreatedAtAsc(ListingStatus status);
  List<Listing> findByStatusAndExpiresAtBefore(ListingStatus status,Instant now);
  boolean existsByProperty_IdAndStatusIn(Long propertyId, Collection<ListingStatus> statuses);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from Listing l where l.id=:id") Optional<Listing> locked(@Param("id") Long id);
}
interface Orders extends JpaRepository<ServiceOrder,Long> {
  Optional<ServiceOrder> findFirstByListing_IdAndStatusOrderByIdDesc(Long listingId, PaymentStatus status);
  boolean existsByBuyer_IdAndPlanAndStatus(Long userId,ListingPlan plan,PaymentStatus status);
  List<ServiceOrder> findByBuyer_IdOrderByCreatedAtDesc(Long userId);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from ServiceOrder o where o.id=:id") Optional<ServiceOrder> locked(@Param("id") Long id);
}
interface Favorites extends JpaRepository<Favorite,Long> {
  Optional<Favorite> findByUser_IdAndListing_Id(Long userId,Long listingId);
  List<Favorite> findByUser_Id(Long userId);
}
interface Leads extends JpaRepository<Lead,Long> {
  boolean existsByRequester_IdAndListing_IdAndCreatedAtAfter(Long userId,Long listingId,Instant since);
  List<Lead> findByListing_Owner_IdOrderByCreatedAtDesc(Long ownerId);
}
interface Reports extends JpaRepository<Report,Long> {
  List<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status);
  boolean existsByReporter_IdAndListing_IdAndStatus(Long reporterId,Long listingId,ReportStatus status);
}
interface Audits extends JpaRepository<Audit,Long> { }
