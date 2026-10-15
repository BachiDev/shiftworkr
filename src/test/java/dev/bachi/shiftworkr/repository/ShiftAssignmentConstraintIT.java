package dev.bachi.shiftworkr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.ShiftAssignment;
import dev.bachi.shiftworkr.domain.ShiftType;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.Ward;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Proves the partial unique index semantics from PLAN.md 4.1: one active row per
 * staff per day, while ABSENT audit traces may repeat.
 */
@DataJpaTest
@Testcontainers
class ShiftAssignmentConstraintIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired
  private WardRepository wards;
  @Autowired
  private ShiftTypeRepository shiftTypes;
  @Autowired
  private StaffMemberRepository staffMembers;
  @Autowired
  private ShiftAssignmentRepository assignments;

  private Ward ward;
  private StaffMember nurse;
  private ShiftType early;
  private LocalDate day;

  @BeforeEach
  void setUp() {
    ward = wards.save(new Ward("Test Ward", "TEST"));
    early = shiftTypes.save(new ShiftType("E", "Early",
        LocalTime.of(6, 0), LocalTime.of(14, 30), 0, 30, "violet"));
    nurse = staffMembers.save(new StaffMember(ward, "nurse@test.local", "hash",
        "Test", "Nurse", Role.NURSE, Qualification.RN, false, 38));
    day = LocalDate.of(2026, 10, 20);
  }

  @Test
  void duplicateScheduledShiftForSameDayIsRejected() {
    assignments.saveAndFlush(new ShiftAssignment(ward, nurse, early, day,
        AssignmentStatus.SCHEDULED));

    assertThatThrownBy(() -> assignments.saveAndFlush(
        new ShiftAssignment(ward, nurse, early, day, AssignmentStatus.SCHEDULED)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void absentRowsMayRepeatForSameDay() {
    assignments.saveAndFlush(new ShiftAssignment(ward, nurse, early, day,
        AssignmentStatus.ABSENT));
    ShiftAssignment second = assignments.saveAndFlush(
        new ShiftAssignment(ward, nurse, early, day, AssignmentStatus.ABSENT));

    assertThat(second.getId()).isNotNull();
  }
}
