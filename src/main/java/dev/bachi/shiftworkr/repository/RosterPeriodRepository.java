package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.RosterPeriod;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.domain.Ward;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterPeriodRepository extends JpaRepository<RosterPeriod, UUID> {
  Optional<RosterPeriod> findByWardAndYearAndMonth(Ward ward, int year, int month);

  List<RosterPeriod> findByWardAndStatus(Ward ward, RosterStatus status);
}
