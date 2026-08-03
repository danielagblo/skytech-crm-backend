package com.skytech.crm.repository;

import com.skytech.crm.entity.CalendarEvent;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
  @Query("select e from CalendarEvent e where e.startTime <= :to and e.endTime >= :from")
  List<CalendarEvent> findInRange(OffsetDateTime from, OffsetDateTime to);

  List<CalendarEvent> findByStartTimeBetween(OffsetDateTime from, OffsetDateTime to);

  @Query("select e from CalendarEvent e where e.startTime <= :to and e.endTime >= :from")
  org.springframework.data.domain.Page<CalendarEvent> findInRange(
      OffsetDateTime from, OffsetDateTime to, org.springframework.data.domain.Pageable pageable);
}
