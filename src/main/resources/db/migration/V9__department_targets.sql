-- Department (team) targets: monthly targets for Calls, Deals Closed and
-- Revenue. Each metric can be toggled on/off by the manager.
create table department_targets (
  id uuid primary key,
  company_id uuid not null,
  period varchar(7) not null,
  metric_type varchar(20) not null,
  target_value numeric(15, 2) not null,
  enabled boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uq_department_targets_period_metric unique (company_id, period, metric_type)
);