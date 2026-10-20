package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.Ward;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<Ward, UUID> {
  Optional<Ward> findByCode(String code);

  List<Ward> findByActiveTrueOrderByNameAsc();
}
