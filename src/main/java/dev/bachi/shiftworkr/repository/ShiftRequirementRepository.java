package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.ShiftRequirement;
import dev.bachi.shiftworkr.domain.Ward;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, UUID> {
  List<ShiftRequirement> findByWard(Ward ward);
}
