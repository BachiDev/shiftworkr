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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_member")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id")
  private Ward ward;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Qualification qualification;

  @Column(name = "lead_qualified", nullable = false)
  private boolean leadQualified;

  @Column(name = "target_weekly_hours", nullable = false)
  private int targetWeeklyHours;

  @Column(nullable = false)
  private boolean active = true;

  public StaffMember(Ward ward, String email, String passwordHash, String firstName,
      String lastName, Role role, Qualification qualification, boolean leadQualified,
      int targetWeeklyHours) {
    this.ward = ward;
    this.email = email;
    this.passwordHash = passwordHash;
    this.firstName = firstName;
    this.lastName = lastName;
    this.role = role;
    this.qualification = qualification;
    this.leadQualified = leadQualified;
    this.targetWeeklyHours = targetWeeklyHours;
  }

  public String fullName() {
    return firstName + " " + lastName;
  }
}
