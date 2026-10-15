-- Phase 0 baseline: proves Flyway wiring against local PG and Neon.
-- Full domain schema (wards, roster_periods, staff, shift_types, ...) lands in Phase 1 (V2+).
CREATE TABLE IF NOT EXISTS flyway_baseline_probe (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
