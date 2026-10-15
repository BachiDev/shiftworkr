package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.LeaveRequest;
import dev.bachi.shiftworkr.domain.LeaveStatus;
import dev.bachi.shiftworkr.domain.StaffMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
  List<LeaveRequest> findByStatusOrderByStartDateAsc(LeaveStatus status);

  List<LeaveRequest> findByStaffOrderByStartDateDesc(StaffMember staff);
}
