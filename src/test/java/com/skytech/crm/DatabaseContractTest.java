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
    for (int version = 1; version <= 5; version++) {
      int current = version;
      assertThat(
              getClass()
                  .getResourceAsStream(
                      switch (current) {
                        case 1 -> "/db/migration/V1__init.sql";
                        case 2 -> "/db/migration/V2__enum_constraints.sql";
                        case 3 -> "/db/migration/V3__deal_call_metrics.sql";
                        case 4 -> "/db/migration/V4__seed_default_settings.sql";
                        default -> "/db/migration/V5__invoices.sql";
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

  @Test
  void invoiceMigrationProvidesLifecycleTablesAndNumbering() throws Exception {
    String migration;
    try (var stream = getClass().getResourceAsStream("/db/migration/V5__invoices.sql")) {
      assertThat(stream).isNotNull();
      migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    assertThat(migration)
        .contains("create sequence invoice_number_seq")
        .contains("create table invoices")
        .contains("create table invoice_items")
        .contains("create table invoice_payments")
        .contains("uuid primary key default gen_random_uuid()")
        .contains("company_id uuid")
        .contains("created_at timestamptz default now()")
        .contains("updated_at timestamptz default now()")
        .contains("invoices_updated_at")
        .doesNotContain("serial", "bigserial");
  }

  @Test
  void contractHardeningMigrationsAddUniquenessAndExecutionState() throws Exception {
    String leadMigration;
    try (var stream =
        getClass().getResourceAsStream("/db/migration/V12__lead_deal_contract.sql")) {
      assertThat(stream).isNotNull();
      leadMigration = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    String communicationMigration;
    try (var stream =
        getClass().getResourceAsStream(
            "/db/migration/V13__automation_broadcast_execution.sql")) {
      assertThat(stream).isNotNull();
      communicationMigration =
          new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    assertThat(leadMigration)
        .contains("create unique index uq_deals_lead_id")
        .contains("check (category is null or category in");
    assertThat(communicationMigration)
        .contains("add column contact_ids uuid[]")
        .contains("add column execution_state")
        .contains("add column next_run_at")
        .contains("add column failure_reason")
        .contains("add column failure_details");
  }

  @Test
  void presenceAssignmentAndDelayedAutomationMigrationMatchesApiContract() throws Exception {
    String migration;
    try (var stream =
        getClass()
            .getResourceAsStream(
                "/db/migration/V14__presence_sessions_assignment_and_automation_jobs.sql")) {
      assertThat(stream).isNotNull();
      migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    assertThat(migration)
        .contains("alter table users add column last_seen_at")
        .contains("create table user_sessions")
        .contains("started_at timestamptz not null")
        .contains("last_activity_at timestamptz not null")
        .contains("ended_at timestamptz")
        .contains("alter table settings add column assignment_cursor")
        .contains("create table automation_execution_jobs")
        .contains("automation_id uuid not null")
        .contains("lead_id uuid")
        .contains("deal_id uuid")
        .contains("step_index int not null")
        .contains("scheduled_at timestamptz not null")
        .contains("attempt_count int not null")
        .contains("last_error text")
        .contains("payment_received")
        .contains("payment_due")
        .contains("payment_overdue")
        .contains("payment_recovery");
  }

  @Test
  void legacyTenantMigrationMakesExistingAccountsUsableByTenantScopedFeatures() throws Exception {
    String migration;
    try (var stream =
        getClass()
            .getResourceAsStream("/db/migration/V15__backfill_legacy_tenant_ids.sql")) {
      assertThat(stream).isNotNull();
      migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
    }
    assertThat(migration)
        .contains("update users set company_id = legacy_company_id where company_id is null")
        .contains("update settings set company_id = legacy_company_id where company_id is null")
        .contains("alter table users alter column company_id set not null");
  }
}
