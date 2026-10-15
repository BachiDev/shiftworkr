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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "leave_request")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private StaffMember staff;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private LeaveType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LeaveStatus status;

  @Column(length = 500)
  private String note;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  public LeaveRequest(StaffMember staff, LocalDate startDate, LocalDate endDate,
      LeaveType type, String note) {
    this.staff = staff;
    this.startDate = startDate;
    this.endDate = endDate;
    this.type = type;
    this.note = note;
    this.status = LeaveStatus.PENDING;
  }
}
