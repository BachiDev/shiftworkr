package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.ShiftType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftTypeRepository extends JpaRepository<ShiftType, UUID> {
  Optional<ShiftType> findByCode(String code);
}
