package dev.bachi.shiftworkr.service;

import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.AuditLog;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.RosterPeriod;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.domain.ShiftAssignment;
import dev.bachi.shiftworkr.domain.ShiftType;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.Ward;
import dev.bachi.shiftworkr.repository.AuditLogRepository;
import dev.bachi.shiftworkr.repository.RosterPeriodRepository;
import dev.bachi.shiftworkr.repository.ShiftAssignmentRepository;
import dev.bachi.shiftworkr.repository.ShiftTypeRepository;
import dev.bachi.shiftworkr.repository.StaffMemberRepository;
import dev.bachi.shiftworkr.repository.WardRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/write access for the monthly ward schedule board (PLAN.md 5.1).
 *
 * <p>Reads return detached DTOs so the view never touches lazy associations
 * outside a transaction ({@code open-in-view} is off). Writes enforce the
 * role and {@code LOCKED}-month guards from PLAN.md 5.4 and append an
 * {@code AUDIT_LOG} entry on every change.</p>
 */
@Service
@Transactional(readOnly = true)
public class ScheduleBoardService {

  /** Status-only codes have no shift type; they are explicit rows, not gaps. */
  private static final Set<String> STATUS_CODES = Set.of("OFF", "VAC", "SICK");

  /** Preferred display order of the seeded shift types. */
  private static final List<String> SHIFT_TYPE_ORDER = List.of("E", "L", "N", "ONCALL");

  private final WardRepository wards;
  private final StaffMemberRepository staffMembers;
  private final ShiftAssignmentRepository assignments;
  private final ShiftTypeRepository shiftTypes;
  private final RosterPeriodRepository rosterPeriods;
  private final AuditLogRepository auditLogs;

  public ScheduleBoardService(WardRepository wards, StaffMemberRepository staffMembers,
      ShiftAssignmentRepository assignments, ShiftTypeRepository shiftTypes,
      RosterPeriodRepository rosterPeriods, AuditLogRepository auditLogs) {
    this.wards = wards;
    this.staffMembers = staffMembers;
    this.assignments = assignments;
    this.shiftTypes = shiftTypes;
    this.rosterPeriods = rosterPeriods;
    this.auditLogs = auditLogs;
  }

  public record WardOption(UUID id, String name, String code) {
  }

  public record ShiftCell(UUID assignmentId, String code, String label,
      AssignmentStatus status, String note) {
  }

  public record Row(UUID staffId, String firstName, String lastName, Qualification qualification,
      boolean leadQualified, Role role, Map<LocalDate, ShiftCell> cells) {

    public String fullName() {
      return firstName + " " + lastName;
    }
  }

  public record ShiftTypeOption(String code, String name, LocalTime startTime,
      LocalTime endTime, int endDayOffset) {
  }

  /** {@code status} is null while no roster period exists for the month yet. */
  public record Board(YearMonth month, RosterStatus status,
      List<ShiftTypeOption> shiftTypes, List<Row> rows) {
  }

  /** Wards the given user may inspect: all active wards for admins, home ward for nurses. */
  public List<WardOption> wardsFor(String email) {
    StaffMember me = requireUser(email);
    List<Ward> visible;
    if (me.getRole() == Role.ADMIN) {
      visible = wards.findByActiveTrueOrderByNameAsc();
    } else {
      visible = me.getWard() == null ? wards.findByActiveTrueOrderByNameAsc()
          : List.of(me.getWard());
    }
    return visible.stream()
        .map(ward -> new WardOption(ward.getId(), ward.getName(), ward.getCode()))
        .toList();
  }

  /** Loads one ward/month matrix with assignments already mapped into per-day cells. */
  public Board load(UUID wardId, YearMonth month) {
    Ward ward = requireWard(wardId);
    LocalDate from = month.atDay(1);
    LocalDate to = month.atEndOfMonth();

    Map<UUID, Map<LocalDate, ShiftCell>> cellsByStaff = new HashMap<>();
    for (ShiftAssignment assignment : assignments
        .findByWardAndShiftDateBetweenOrderByShiftDateAsc(ward, from, to)) {
      // ABSENT rows are audit traces only and may repeat per day.
      if (assignment.getStatus() == AssignmentStatus.ABSENT) {
        continue;
      }
      cellsByStaff.computeIfAbsent(assignment.getStaffMember().getId(), id -> new HashMap<>())
          .put(assignment.getShiftDate(), toCell(assignment));
    }

    List<Row> rows = staffMembers
        .findByWardAndActiveTrueOrderByLastNameAscFirstNameAsc(ward).stream()
        .map(member -> new Row(member.getId(), member.getFirstName(), member.getLastName(),
            member.getQualification(), member.isLeadQualified(), member.getRole(),
            Map.copyOf(cellsByStaff.getOrDefault(member.getId(), Map.of()))))
        .toList();

    RosterStatus status = rosterPeriods
        .findByWardAndYearAndMonth(ward, month.getYear(), month.getMonthValue())
        .map(RosterPeriod::getStatus)
        .orElse(null);

    return new Board(month, status, shiftTypeOptions(), rows);
  }

  /**
   * Creates or replaces the single active assignment for one staff member/day.
   * OFF/VAC/SICK become explicit rows with a null shift type (PLAN.md 4.1).
   */
  @Transactional
  public void saveAssignment(UUID wardId, UUID staffId, LocalDate date, String code,
      String note, String actorEmail) {
    StaffMember actor = requireUser(actorEmail);
    if (actor.getRole() != Role.ADMIN) {
      throw new AccessDeniedException("Only ward managers can edit the roster.");
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("Select a shift before saving.");
    }
    Ward ward = requireWard(wardId);
    StaffMember member = staffMembers.findById(staffId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown staff member: " + staffId));
    if (member.getWard() == null || !member.getWard().getId().equals(ward.getId())) {
      throw new IllegalArgumentException("Staff member does not belong to this ward.");
    }

    RosterPeriod period = rosterPeriods
        .findByWardAndYearAndMonth(ward, date.getYear(), date.getMonthValue())
        .orElse(null);
    if (period != null && period.getStatus() == RosterStatus.LOCKED) {
      throw new IllegalStateException("This roster month is locked and cannot be edited.");
    }

    AssignmentStatus status = statusFor(code);
    ShiftType shiftType = STATUS_CODES.contains(code) ? null
        : shiftTypes.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Unknown shift type: " + code));

    Optional<ShiftAssignment> existing = assignments
        .findByStaffMemberAndShiftDateBetweenOrderByShiftDateAsc(member, date, date).stream()
        .filter(row -> row.getStatus() != AssignmentStatus.ABSENT)
        .findFirst();
    boolean created = existing.isEmpty();
    ShiftAssignment assignment = existing
        .orElseGet(() -> new ShiftAssignment(ward, member, shiftType, date, status));
    assignment.setShiftType(shiftType);
    assignment.setStatus(status);
    assignment.setNote(note == null || note.isBlank() ? null : note.trim());
    assignments.saveAndFlush(assignment);

    if (period == null) {
      rosterPeriods.save(new RosterPeriod(ward, date.getYear(), date.getMonthValue()));
    }
    auditLogs.save(new AuditLog(actor,
        created ? "ASSIGNMENT_CREATED" : "ASSIGNMENT_UPDATED", "shift_assignment",
        assignment.getId(), Map.of(
            "ward", ward.getCode(),
            "staff", member.getEmail(),
            "date", date.toString(),
            "code", code)));
  }

  private StaffMember requireUser(String email) {
    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("Not authenticated.");
    }
    return staffMembers.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccessDeniedException("Unknown user: " + email));
  }

  private Ward requireWard(UUID wardId) {
    return wards.findById(wardId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown ward: " + wardId));
  }

  private List<ShiftTypeOption> shiftTypeOptions() {
    return shiftTypes.findAll().stream()
        .sorted(Comparator.comparingInt((ShiftType type) -> {
          int index = SHIFT_TYPE_ORDER.indexOf(type.getCode());
          return index < 0 ? SHIFT_TYPE_ORDER.size() : index;
        }))
        .map(type -> new ShiftTypeOption(type.getCode(), type.getName(),
            type.getStartTime(), type.getEndTime(), type.getEndDayOffset()))
        .toList();
  }

  private static AssignmentStatus statusFor(String code) {
    return switch (code) {
      case "OFF" -> AssignmentStatus.OFF;
      case "VAC" -> AssignmentStatus.VACATION;
      case "SICK" -> AssignmentStatus.SICK;
      default -> AssignmentStatus.SCHEDULED;
    };
  }

  private static ShiftCell toCell(ShiftAssignment assignment) {
    ShiftType type = assignment.getShiftType();
    if (type != null) {
      return new ShiftCell(assignment.getId(), type.getCode(), type.getName(),
          assignment.getStatus(), assignment.getNote());
    }
    record StatusLabel(String code, String label) {
    }
    StatusLabel label = switch (assignment.getStatus()) {
      case VACATION -> new StatusLabel("VAC", "Vacation");
      case SICK -> new StatusLabel("SICK", "Sick leave");
      case OFF -> new StatusLabel("OFF", "Day off");
      default -> new StatusLabel("ABSENT", "Absent");
    };
    return new ShiftCell(assignment.getId(), label.code(), label.label(),
        assignment.getStatus(), assignment.getNote());
  }
}
