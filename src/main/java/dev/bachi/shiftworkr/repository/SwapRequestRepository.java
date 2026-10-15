package dev.bachi.shiftworkr.repository;

import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.SwapRequest;
import dev.bachi.shiftworkr.domain.SwapStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, UUID> {
  List<SwapRequest> findByStatusOrderByCreatedAtDesc(SwapStatus status);

  List<SwapRequest> findByRequesterOrderByCreatedAtDesc(StaffMember requester);
}
