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

public interface MediaFiles extends JpaRepository<ListingMedia,Long> {
  List<ListingMedia> findByListing_IdOrderByIdAsc(Long listingId);
  List<ListingMedia> findByIdentityVerification_IdOrderByIdAsc(Long id);
  long countByListing_IdAndPurpose(Long id,String purpose);
}
