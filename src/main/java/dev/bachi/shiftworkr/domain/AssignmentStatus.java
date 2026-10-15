package dev.bachi.shiftworkr.domain;

/**
 * OFF / VACATION / SICK are explicit rows (not absence of a row) so the rule
 * engine can distinguish rest days from unscheduled days.
 */
public enum AssignmentStatus {
  SCHEDULED,
  SWAP_PENDING,
  COMPLETED,
  OFF,
  VACATION,
  SICK,
  ABSENT
}
