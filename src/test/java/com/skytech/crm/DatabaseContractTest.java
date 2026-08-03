package com.skytech.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.skytech.crm.entity.Lead;
import jakarta.persistence.Column;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DatabaseContractTest {
  @Test
  void migrationsContainEveryRequiredTableAndUuidTimestampRules() throws Exception {
    String migration;
    try (var stream = getClass().getResourceAsStream("/db/migration/V1__init.sql")) {
      assertThat(stream).isNotNull();
      migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    for (String table :
        new String[] {
          "users",
          "leads",
          "deals",
          "deal_logs",
          "deal_log_comments",
          "tasks",
          "task_assignees",
          "sub_tasks",
          "task_comments",
          "activities",
          "calendar_events",
          "automations",
          "broadcast_messages"
        }) assertThat(migration).contains("create table " + table);
    assertThat(migration)
        .contains("uuid primary key default gen_random_uuid()")
        .contains("created_at timestamptz default now()")
        .contains("updated_at timestamptz default now()")
        .contains("refresh_token_hash varchar(255)")
        .contains("deleted_at timestamptz")
        .contains("version bigint not null default 0")
        .doesNotContain(" serial", "bigserial", "identity");
    assertThat(migration.split("company_id uuid", -1).length - 1).isGreaterThanOrEqualTo(12);
  }

  @Test
  void flywayVersionsAreSequential() {
    for (int version = 1; version <= 4; version++) {
      int current = version;
      assertThat(
              getClass()
                  .getResourceAsStream(
                      switch (current) {
                        case 1 -> "/db/migration/V1__init.sql";
                        case 2 -> "/db/migration/V2__enum_constraints.sql";
                        case 3 -> "/db/migration/V3__deal_call_metrics.sql";
                        default -> "/db/migration/V4__seed_default_settings.sql";
                      }))
          .isNotNull();
    }
  }

  @Test
  void leadPhoneFieldsMatchFlywayColumnNames() throws Exception {
    assertThat(Lead.class.getDeclaredField("phone1").getAnnotation(Column.class).name())
        .isEqualTo("phone_1");
    assertThat(Lead.class.getDeclaredField("phone2").getAnnotation(Column.class).name())
        .isEqualTo("phone_2");
  }
}
