package dev.bachi.shiftworkr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shift_type")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftType {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 10)
  private String code;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  /** 1 when the shift ends the next calendar day (Night 21:30-06:30). */
  @Column(name = "end_day_offset", nullable = false)
  private int endDayOffset;

  @Column(name = "break_duration_minutes", nullable = false)
  private int breakDurationMinutes;

  @Column(name = "color_code", nullable = false, length = 20)
  private String colorCode;

  public ShiftType(String code, String name, LocalTime startTime, LocalTime endTime,
      int endDayOffset, int breakDurationMinutes, String colorCode) {
    this.code = code;
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.endDayOffset = endDayOffset;
    this.breakDurationMinutes = breakDurationMinutes;
    this.colorCode = colorCode;
  }
}
