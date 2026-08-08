package com.skytech.crm.repository;

import com.skytech.crm.entity.Rating;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
  Optional<Rating> findByToken(String token);

  Optional<Rating> findFirstByDealIdAndAgentIdAndRatedFalseOrderByCreatedAtDesc(
      UUID dealId, UUID agentId);

  List<Rating> findByAgentIdOrderByCreatedAtDesc(UUID agentId);

  List<Rating> findByDealIdOrderByCreatedAtDesc(UUID dealId);
}
