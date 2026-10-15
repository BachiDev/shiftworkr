package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.ShiftAssignment;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.Ward;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, UUID> {
  List<ShiftAssignment> findByWardAndShiftDateBetweenOrderByShiftDateAsc(Ward ward,
      LocalDate from, LocalDate to);

  List<ShiftAssignment> findByStaffMemberAndShiftDateBetweenOrderByShiftDateAsc(
      StaffMember staffMember, LocalDate from, LocalDate to);

  List<ShiftAssignment> findByWardAndShiftDateAndStatus(Ward ward, LocalDate date,
      AssignmentStatus status);
}
