package dev.bachi.shiftworkr.seed;

import dev.bachi.shiftworkr.domain.ShiftType;
import dev.bachi.shiftworkr.repository.ShiftTypeRepository;
import java.time.LocalTime;
import java.util.List;

/** Shared reference data (shift types E/L/N/ONCALL) for demo and prod bootstraps. */
final class ReferenceData {

  private ReferenceData() {
  }

  static List<ShiftType> shiftTypes(ShiftTypeRepository repo) {
    List<ShiftType> existing = repo.findAll();
    if (!existing.isEmpty()) {
      return existing;
    }
    List<ShiftType> types = List.of(
        new ShiftType("E", "Early", LocalTime.of(6, 0), LocalTime.of(14, 30), 0, 30, "violet"),
        new ShiftType("L", "Late", LocalTime.of(14, 0), LocalTime.of(22, 0), 0, 30, "sky"),
        new ShiftType("N", "Night", LocalTime.of(21, 30), LocalTime.of(6, 30), 1, 45, "fuchsia"),
        new ShiftType("ONCALL", "On-Call", LocalTime.MIDNIGHT, LocalTime.of(23, 59), 0, 0, "zinc"));
    return repo.saveAll(types);
  }
}
