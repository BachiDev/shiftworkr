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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roster_period",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ward_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RosterPeriod {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ward_id", nullable = false)
  private Ward ward;

  @Column(name = "year", nullable = false)
  private int year;

  @Column(name = "month", nullable = false)
  private int month;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RosterStatus status = RosterStatus.DRAFT;

  @Column(name = "published_at")
  private Instant publishedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "published_by")
  private StaffMember publishedBy;

  public RosterPeriod(Ward ward, int year, int month) {
    this.ward = ward;
    this.year = year;
    this.month = month;
  }
}
