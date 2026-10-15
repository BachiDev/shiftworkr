package dev.bachi.shiftworkr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shift_requirement")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftRequirement {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ward_id", nullable = false)
  private Ward ward;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shift_type_id", nullable = false)
  private ShiftType shiftType;

  /** Null when this row is a specific-date (holiday) override. */
  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", length = 10)
  private DayOfWeek dayOfWeek;

  /** Takes precedence over the weekly pattern when set. */
  @Column(name = "specific_date")
  private LocalDate specificDate;

  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;

  @Column(name = "min_staff_count", nullable = false)
  private int minStaffCount;

  @Column(name = "min_lead_count", nullable = false)
  private int minLeadCount;

  public ShiftRequirement(Ward ward, ShiftType shiftType, DayOfWeek dayOfWeek,
      LocalDate specificDate, int minStaffCount, int minLeadCount) {
    this.ward = ward;
    this.shiftType = shiftType;
    this.dayOfWeek = dayOfWeek;
    this.specificDate = specificDate;
    this.minStaffCount = minStaffCount;
    this.minLeadCount = minLeadCount;
  }
}
