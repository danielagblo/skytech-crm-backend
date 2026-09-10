package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import com.skytech.crm.repository.*;
import com.skytech.crm.exception.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadViewService {
  private final LeadViewRepository repo;
  private final LeadRepository leads;
  private final UserRepository users;

  @Transactional
  public void markSeen(UUID leadId, UUID userId) {
    Lead lead = leads.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead"));
    var found = repo.findByLead_IdAndUser_Id(leadId, userId);
    if (found.isPresent()) {
      LeadView v = found.get();
      v.setSeenAt(OffsetDateTime.now());
      repo.save(v);
    } else {
      LeadView v = new LeadView();
      v.setLead(lead);
      var user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
      v.setUser(user);
      v.setSeenAt(OffsetDateTime.now());
      repo.save(v);
    }
  }

  @Transactional(readOnly = true)
  public List<UUID> seenForUser(UUID userId, Collection<UUID> leadIds) {
    if (leadIds == null || leadIds.isEmpty()) return List.of();
    return repo.findByUser_IdAndLead_IdIn(userId, leadIds).stream()
        .map(lv -> lv.getLead().getId())
        .toList();
  }
}
