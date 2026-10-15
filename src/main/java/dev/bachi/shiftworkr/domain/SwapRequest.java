package dev.bachi.shiftworkr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "swap_request")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SwapRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_id", nullable = false)
  private StaffMember requester;

  /** Null for OPEN board offers until claimed. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_colleague_id")
  private StaffMember targetColleague;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offered_shift_id", nullable = false)
  private ShiftAssignment offeredShift;

  /** Null for OPEN board offers (single-claim reassignment). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_shift_id")
  private ShiftAssignment requestedShift;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SwapStatus status;

  @Column(length = 500)
  private String reason;

  @Column(name = "admin_note", length = 500)
  private String adminNote;

  @Column(name = "compliance_override", nullable = false)
  private boolean complianceOverride;

  @Column(name = "override_reason", length = 500)
  private String overrideReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by")
  private StaffMember decidedBy;

  public SwapRequest(StaffMember requester, StaffMember targetColleague,
      ShiftAssignment offeredShift, ShiftAssignment requestedShift, String reason) {
    this.requester = requester;
    this.targetColleague = targetColleague;
    this.offeredShift = offeredShift;
    this.requestedShift = requestedShift;
    this.reason = reason;
    this.status = SwapStatus.PENDING_COLLEAGUE;
  }
}
