package com.skytech.crm.dto.response;

import com.skytech.crm.enums.AutomationType;
import java.util.*;

public record AutomationOptionsResponse(
    List<TypeOption> types, List<String> channels, List<String> stepFields) {
  public record TypeOption(
      AutomationType type,
      boolean executable,
      String trigger,
      List<String> requiredTriggerFields) {}
}
