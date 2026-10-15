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
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "shift_assignment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ward_id", nullable = false)
  private Ward ward;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_member_id", nullable = false)
  private StaffMember staffMember;

  /** Null for OFF / VACATION / SICK rows. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shift_type_id")
  private ShiftType shiftType;

  @Column(name = "shift_date", nullable = false)
  private LocalDate shiftDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AssignmentStatus status;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Version
  private int version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ShiftAssignment(Ward ward, StaffMember staffMember, ShiftType shiftType,
      LocalDate shiftDate, AssignmentStatus status) {
    this.ward = ward;
    this.staffMember = staffMember;
    this.shiftType = shiftType;
    this.shiftDate = shiftDate;
    this.status = status;
  }
}
