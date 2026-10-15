package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.StaffMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {
  Optional<StaffMember> findByEmailIgnoreCase(String email);

  List<StaffMember> findByActiveTrueOrderByLastNameAscFirstNameAsc();
}
