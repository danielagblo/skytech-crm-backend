package com.skytech.crm.repository;

import com.skytech.crm.entity.LeadView;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadViewRepository extends JpaRepository<LeadView, UUID> {
  Optional<LeadView> findByLead_IdAndUser_Id(UUID leadId, UUID userId);
  List<LeadView> findByUser_IdAndLead_IdIn(UUID userId, Collection<UUID> leadIds);
}
