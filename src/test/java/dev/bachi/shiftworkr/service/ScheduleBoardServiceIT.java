package dev.bachi.shiftworkr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.RosterPeriod;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.domain.ShiftType;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.Ward;
import dev.bachi.shiftworkr.repository.RosterPeriodRepository;
import dev.bachi.shiftworkr.repository.ShiftAssignmentRepository;
import dev.bachi.shiftworkr.repository.ShiftTypeRepository;
import dev.bachi.shiftworkr.repository.StaffMemberRepository;
import dev.bachi.shiftworkr.repository.WardRepository;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Board;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Row;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftCell;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Covers the write guards and the detached board model behind the monthly
 * schedule grid (PLAN.md 5.1 / 5.4).
 */
@SpringBootTest
@Testcontainers
@Transactional
class ScheduleBoardServiceIT {

  @SuppressWarnings("unused") // started reflectively by the Testcontainers JUnit extension
  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired
  private ScheduleBoardService service;
  @Autowired
  private WardRepository wards;
  @Autowired
  private StaffMemberRepository staffMembers;
  @Autowired
  private ShiftTypeRepository shiftTypes;
  @Autowired
  private ShiftAssignmentRepository assignments;
  @Autowired
  private RosterPeriodRepository rosterPeriods;

  private Ward ward;
  private StaffMember admin;
  private StaffMember nurse;
  private LocalDate day;

  @BeforeEach
  void setUp() {
    ward = wards.save(new Ward("Test Ward", "TEST"));
    ShiftType early = shiftTypes.save(new ShiftType("E", "Early",
        LocalTime.of(6, 0), LocalTime.of(14, 30), 0, 30, "violet"));
    assertThat(early.getCode()).isEqualTo("E");
    admin = staffMembers.save(new StaffMember(ward, "admin@test.local", "hash",
        "Ada", "Admin", Role.ADMIN, Qualification.RN, true, 38));
    nurse = staffMembers.save(new StaffMember(ward, "nurse@test.local", "hash",
        "Nina", "Nurse", Role.NURSE, Qualification.RN, false, 38));
    day = LocalDate.of(2026, 10, 20);
  }

  @Test
  void savingShiftCreatesDraftPeriodAndBoardCell() {
    service.saveAssignment(ward.getId(), nurse.getId(), day, "E", "Handover",
        admin.getEmail());

    Board board = service.load(ward.getId(), YearMonth.from(day));
    assertThat(board.status()).isEqualTo(RosterStatus.DRAFT);

    Row row = board.rows().stream()
        .filter(candidate -> candidate.staffId().equals(nurse.getId()))
        .findFirst()
        .orElseThrow();
    assertThat(row.cells()).containsKey(day);
    ShiftCell cell = row.cells().get(day);
    assertThat(cell.code()).isEqualTo("E");
    assertThat(cell.status()).isEqualTo(AssignmentStatus.SCHEDULED);
    assertThat(cell.note()).isEqualTo("Handover");
  }

  @Test
  void vacationReplacesShiftInsteadOfAddingASecondRow() {
    service.saveAssignment(ward.getId(), nurse.getId(), day, "E", null, admin.getEmail());
    service.saveAssignment(ward.getId(), nurse.getId(), day, "VAC", "Family trip",
        admin.getEmail());

    assertThat(assignments
        .findByStaffMemberAndShiftDateBetweenOrderByShiftDateAsc(nurse, day, day))
        .hasSize(1);

    Board board = service.load(ward.getId(), YearMonth.from(day));
    ShiftCell cell = board.rows().stream()
        .filter(candidate -> candidate.staffId().equals(nurse.getId()))
        .findFirst()
        .orElseThrow()
        .cells()
        .get(day);
    assertThat(cell.code()).isEqualTo("VAC");
    assertThat(cell.status()).isEqualTo(AssignmentStatus.VACATION);
  }

  @Test
  void lockedMonthRejectsEdits() {
    RosterPeriod period = new RosterPeriod(ward, day.getYear(), day.getMonthValue());
    period.setStatus(RosterStatus.LOCKED);
    rosterPeriods.saveAndFlush(period);

    assertThatThrownBy(() -> service.saveAssignment(ward.getId(), nurse.getId(), day, "E",
        null, admin.getEmail()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("locked");
  }

  @Test
  void nurseCannotEditTheRoster() {
    assertThatThrownBy(() -> service.saveAssignment(ward.getId(), nurse.getId(), day, "E",
        null, nurse.getEmail()))
        .isInstanceOf(AccessDeniedException.class);
  }
}
