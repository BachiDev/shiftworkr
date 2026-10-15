package dev.bachi.shiftworkr.seed;

import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.AuditLog;
import dev.bachi.shiftworkr.domain.LeaveRequest;
import dev.bachi.shiftworkr.domain.LeaveStatus;
import dev.bachi.shiftworkr.domain.LeaveType;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.RosterPeriod;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.domain.ShiftAssignment;
import dev.bachi.shiftworkr.domain.ShiftRequirement;
import dev.bachi.shiftworkr.domain.ShiftType;
import dev.bachi.shiftworkr.domain.StaffMember;
import dev.bachi.shiftworkr.domain.SwapRequest;
import dev.bachi.shiftworkr.domain.Ward;
import dev.bachi.shiftworkr.repository.AuditLogRepository;
import dev.bachi.shiftworkr.repository.LeaveRequestRepository;
import dev.bachi.shiftworkr.repository.RosterPeriodRepository;
import dev.bachi.shiftworkr.repository.ShiftAssignmentRepository;
import dev.bachi.shiftworkr.repository.ShiftRequirementRepository;
import dev.bachi.shiftworkr.repository.ShiftTypeRepository;
import dev.bachi.shiftworkr.repository.StaffMemberRepository;
import dev.bachi.shiftworkr.repository.SwapRequestRepository;
import dev.bachi.shiftworkr.repository.WardRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo-mode seed: Cardiology Ward 4B with 16 staff, shift types, requirements,
 * a published current-month roster (incl. one intentional 11h-rest violation),
 * leave examples, one direct + one OPEN swap request. Idempotent — skips when the
 * ward already exists. Active on the {@code demo} profile only, never in production.
 */
@Service
@Profile("demo")
public class DemoSeedService implements ApplicationRunner {

  static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  static final String WARD_CODE = "CARD-4B";

  private final WardRepository wards;
  private final ShiftTypeRepository shiftTypes;
  private final StaffMemberRepository staffMembers;
  private final RosterPeriodRepository rosterPeriods;
  private final ShiftRequirementRepository requirements;
  private final ShiftAssignmentRepository assignments;
  private final LeaveRequestRepository leaves;
  private final SwapRequestRepository swaps;
  private final AuditLogRepository auditLogs;
  private final PasswordEncoder passwordEncoder;

  public DemoSeedService(WardRepository wards, ShiftTypeRepository shiftTypes,
      StaffMemberRepository staffMembers, RosterPeriodRepository rosterPeriods,
      ShiftRequirementRepository requirements, ShiftAssignmentRepository assignments,
      LeaveRequestRepository leaves, SwapRequestRepository swaps,
      AuditLogRepository auditLogs, PasswordEncoder passwordEncoder) {
    this.wards = wards;
    this.shiftTypes = shiftTypes;
    this.staffMembers = staffMembers;
    this.rosterPeriods = rosterPeriods;
    this.requirements = requirements;
    this.assignments = assignments;
    this.leaves = leaves;
    this.swaps = swaps;
    this.auditLogs = auditLogs;
    this.passwordEncoder = passwordEncoder;
  }

  private record Seed(String first, String last, Role role, Qualification qual,
      boolean lead, int hours) {
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (wards.findByCode(WARD_CODE).isPresent()) {
      return;
    }
    Ward ward = wards.save(new Ward("Cardiology Ward 4B", WARD_CODE));
    Map<String, ShiftType> types = new HashMap<>();
    for (ShiftType type : ReferenceData.shiftTypes(shiftTypes)) {
      types.put(type.getCode(), type);
    }

    String hash = passwordEncoder.encode("demo1234");
    Map<String, StaffMember> staff = seedStaff(ward, hash);
    seedRequirements(ward, types);
    YearMonth month = YearMonth.now(ZONE);
    RosterPeriod period = rosterPeriods.save(new RosterPeriod(ward, month.getYear(),
        month.getMonthValue()));
    seedRoster(ward, types, staff, month);
    seedLeaveExamples(staff, month);
    seedSwapExamples(staff, month);

    period.setStatus(RosterStatus.PUBLISHED);
    period.setPublishedAt(Instant.now());
    period.setPublishedBy(staff.get("sarah.miller@shiftworkr.local"));
    auditLogs.save(new AuditLog(staff.get("sarah.miller@shiftworkr.local"), "ROSTER_PUBLISHED",
        "roster_period", period.getId(),
        Map.of("ward", WARD_CODE, "month", month.toString())));
    YearMonth next = month.plusMonths(1);
    rosterPeriods.save(new RosterPeriod(ward, next.getYear(), next.getMonthValue()));
  }

  private Map<String, StaffMember> seedStaff(Ward ward, String hash) {
    List<Seed> seeds = List.of(
        new Seed("Sarah", "Miller", Role.ADMIN, Qualification.RN, true, 38),
        new Seed("Alex", "Rivera", Role.NURSE, Qualification.RN, false, 38),
        new Seed("Maria", "Schneider", Role.NURSE, Qualification.RN, true, 30),
        new Seed("Jonas", "Weber", Role.NURSE, Qualification.ICU_SPECIALIST, true, 38),
        new Seed("Aisha", "Khan", Role.NURSE, Qualification.ICU_SPECIALIST, false, 38),
        new Seed("Tom", "Berger", Role.NURSE, Qualification.ASSISTANT, false, 30),
        new Seed("Lena", "Fischer", Role.NURSE, Qualification.RN, false, 20),
        new Seed("David", "Hoffmann", Role.NURSE, Qualification.ASSISTANT, false, 38),
        new Seed("Emma", "Wagner", Role.NURSE, Qualification.RN, true, 38),
        new Seed("Finn", "Becker", Role.NURSE, Qualification.ASSISTANT, false, 20),
        new Seed("Clara", "Neumann", Role.NURSE, Qualification.ICU_SPECIALIST, false, 30),
        new Seed("Paul", "Zimmermann", Role.NURSE, Qualification.RN, false, 38),
        new Seed("Sofia", "Rossi", Role.NURSE, Qualification.ASSISTANT, false, 30),
        new Seed("Leon", "Krause", Role.NURSE, Qualification.RN, true, 38),
        new Seed("Hannah", "Wolf", Role.NURSE, Qualification.ASSISTANT, false, 20),
        new Seed("Max", "Braun", Role.NURSE, Qualification.RN, false, 30));
    Map<String, StaffMember> staff = new HashMap<>();
    for (Seed s : seeds) {
      StaffMember saved = staffMembers.save(new StaffMember(ward,
          s.first().toLowerCase() + "." + s.last().toLowerCase() + "@shiftworkr.local",
          hash, s.first(), s.last(), s.role(), s.qual(), s.lead(), s.hours()));
      staff.put(saved.getEmail(), saved);
    }
    return staff;
  }

  private void seedRequirements(Ward ward, Map<String, ShiftType> types) {
    for (DayOfWeek dow : DayOfWeek.values()) {
      requirements.save(new ShiftRequirement(ward, types.get("E"), dow, null, 4, 1));
      requirements.save(new ShiftRequirement(ward, types.get("L"), dow, null, 3, 1));
      requirements.save(new ShiftRequirement(ward, types.get("N"), dow, null, 2, 1));
    }
  }

  private void seedRoster(Ward ward, Map<String, ShiftType> types,
      Map<String, StaffMember> staff, YearMonth month) {
    Random random = new Random(42);
    List<StaffMember> all = new ArrayList<>(staff.values());
    for (int day = 1; day <= month.lengthOfMonth(); day++) {
      LocalDate date = month.atDay(day);
      List<StaffMember> pool = new ArrayList<>(all);
      Collections.shuffle(pool, random);
      List<ShiftAssignment> saved = new ArrayList<>();
      saved.addAll(assignTeam(ward, types.get("E"), pool, 4, date));
      saved.addAll(assignTeam(ward, types.get("L"), pool, 3, date));
      saved.addAll(assignTeam(ward, types.get("N"), pool, 2, date));
      for (StaffMember rest : pool) {
        saved.add(new ShiftAssignment(ward, rest, null, date, AssignmentStatus.OFF));
      }
      assignments.saveAll(saved);
    }
    // Intentional 11h-rest violation for the demo tour: Late ends 22:00, next Early
    // starts 06:00 -> 8h rest. The Phase 4 rule engine must flag this pair.
    forceShift(staff.get("alex.rivera@shiftworkr.local"), month.atDay(10), types.get("L"));
    forceShift(staff.get("alex.rivera@shiftworkr.local"), month.atDay(11), types.get("E"));
    // Approved vacation: assignments + leave request stay consistent.
    StaffMember maria = staff.get("maria.schneider@shiftworkr.local");
    for (int day = 15; day <= 17; day++) {
      forceOff(maria, month.atDay(day), AssignmentStatus.VACATION);
    }
    // Sick day with matching approved sick leave.
    forceOff(staff.get("tom.berger@shiftworkr.local"), month.atDay(20), AssignmentStatus.SICK);
  }

  /** Takes the first {@code size} staff from the pool and guarantees a lead aboard. */
  private List<ShiftAssignment> assignTeam(Ward ward, ShiftType type, List<StaffMember> pool,
      int size, LocalDate date) {
    List<StaffMember> team = new ArrayList<>(pool.subList(0, size));
    pool.subList(0, size).clear();
    if (!containsLead(team)) {
      StaffMember lead = firstLead(pool);
      if (lead == null) {
        throw new IllegalStateException("No lead left for " + type.getCode());
      }
      pool.remove(lead);
      StaffMember swapped = firstNonLead(team);
      if (swapped == null) {
        throw new IllegalStateException("No non-lead to swap in " + type.getCode());
      }
      team.remove(swapped);
      team.add(lead);
      pool.add(swapped);
    }
    List<ShiftAssignment> result = new ArrayList<>();
    for (StaffMember member : team) {
      result.add(new ShiftAssignment(ward, member, type, date, AssignmentStatus.SCHEDULED));
    }
    return result;
  }

  private static boolean containsLead(List<StaffMember> team) {
    for (StaffMember member : team) {
      if (member.isLeadQualified()) {
        return true;
      }
    }
    return false;
  }

  private static StaffMember firstLead(List<StaffMember> pool) {
    for (StaffMember member : pool) {
      if (member.isLeadQualified()) {
        return member;
      }
    }
    return null;
  }

  private static StaffMember firstNonLead(List<StaffMember> team) {
    for (StaffMember member : team) {
      if (!member.isLeadQualified()) {
        return member;
      }
    }
    return null;
  }

  private void forceShift(StaffMember staff, LocalDate date, ShiftType type) {
    ShiftAssignment row = singleRow(staff, date);
    row.setShiftType(type);
    row.setStatus(AssignmentStatus.SCHEDULED);
  }

  private void forceOff(StaffMember staff, LocalDate date, AssignmentStatus status) {
    ShiftAssignment row = singleRow(staff, date);
    row.setShiftType(null);
    row.setStatus(status);
  }

  private ShiftAssignment singleRow(StaffMember staff, LocalDate date) {
    List<ShiftAssignment> rows = assignments
        .findByStaffMemberAndShiftDateBetweenOrderByShiftDateAsc(staff, date, date);
    if (rows.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one row for " + staff.getEmail() + " on " + date);
    }
    return rows.get(0);
  }

  private void seedLeaveExamples(Map<String, StaffMember> staff, YearMonth month) {
    LeaveRequest vacation = new LeaveRequest(staff.get("maria.schneider@shiftworkr.local"),
        month.atDay(15), month.atDay(17), LeaveType.VAC, "Summer holidays");
    vacation.setStatus(LeaveStatus.APPROVED);
    vacation.setDecidedAt(Instant.now());
    leaves.save(vacation);

    LeaveRequest sick = new LeaveRequest(staff.get("tom.berger@shiftworkr.local"),
        month.atDay(20), month.atDay(20), LeaveType.SICK, "Flu");
    sick.setStatus(LeaveStatus.APPROVED);
    sick.setDecidedAt(Instant.now());
    leaves.save(sick);

    leaves.save(new LeaveRequest(staff.get("jonas.weber@shiftworkr.local"),
        month.atDay(25), month.atDay(26), LeaveType.VAC, "Family weekend"));
  }

  private void seedSwapExamples(Map<String, StaffMember> staff, YearMonth month) {
    StaffMember alex = staff.get("alex.rivera@shiftworkr.local");
    StaffMember maria = staff.get("maria.schneider@shiftworkr.local");
    StaffMember leon = staff.get("leon.krause@shiftworkr.local");
    LocalDate today = LocalDate.now(ZONE);
    LocalDate from = today.isAfter(month.atDay(1)) ? today : month.atDay(1);

    ShiftAssignment offered = firstShiftOfType(alex, "N", month, from);
    ShiftAssignment requested = firstShiftOfType(maria, "E", month, from);
    if (offered != null && requested != null) {
      swaps.save(new SwapRequest(alex, maria, offered, requested, "Family appointment"));
    }
    ShiftAssignment open = firstShiftOfType(leon, "L", month, from);
    if (open != null) {
      swaps.save(new SwapRequest(leon, null, open, null, "Anyone want my late shift?"));
    }
  }

  private ShiftAssignment firstShiftOfType(StaffMember staff, String typeCode,
      YearMonth month, LocalDate from) {
    return assignments
        .findByStaffMemberAndShiftDateBetweenOrderByShiftDateAsc(staff, from,
            month.atEndOfMonth())
        .stream()
        .filter(a -> a.getStatus() == AssignmentStatus.SCHEDULED
            && a.getShiftType() != null
            && typeCode.equals(a.getShiftType().getCode()))
        .findFirst()
        .orElse(null);
  }
}
