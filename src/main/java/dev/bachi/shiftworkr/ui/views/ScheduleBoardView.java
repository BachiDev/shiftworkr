package dev.bachi.shiftworkr.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.service.ScheduleBoardService;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Board;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Row;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftCell;
import dev.bachi.shiftworkr.service.ScheduleBoardService.WardOption;
import dev.bachi.shiftworkr.ui.MainLayout;
import dev.bachi.shiftworkr.ui.component.ShiftAssignmentDialog;
import jakarta.annotation.security.RolesAllowed;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Monthly ward schedule board (PLAN.md 5.1): frozen staff column, virtual-scroll
 * day columns, and a single-tap / double-click assignment dialog per cell.
 *
 * <p>Admins edit until the roster month is {@code LOCKED}; nurses get a
 * read-only dialog. Live coverage bar and compliance preview arrive with the
 * Phase 4 rule engine.</p>
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Schedule | ShiftWorkr")
@RolesAllowed({"ADMIN", "NURSE"})
public class ScheduleBoardView extends VerticalLayout {

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private static final DateTimeFormatter MONTH_FORMAT =
      DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
  private static final List<String> STATUS_CODES = List.of("OFF", "VAC", "SICK");

  private final ScheduleBoardService service;
  private final String email;
  private final boolean admin;

  private final Grid<Row> grid = new Grid<>();
  private final ComboBox<WardOption> wardSelect = new ComboBox<>("Ward");
  private final TextField nameFilter = new TextField("Staff");
  private final ComboBox<Qualification> qualificationFilter = new ComboBox<>("Qualification");
  private final ComboBox<String> shiftFilter = new ComboBox<>("Shift");
  private final Checkbox leadFilter = new Checkbox("Lead-qualified only");
  private final Span monthLabel = new Span();
  private final Span statusBadge = new Span();
  private final Span countLabel = new Span();

  private YearMonth month = YearMonth.now(ZONE);
  private Board board;
  private boolean editable;

  public ScheduleBoardView(ScheduleBoardService service) {
    this.service = service;
    this.email = currentEmail();
    this.admin = isAdmin();

    setSizeFull();
    setPadding(true);
    setSpacing(true);

    add(buildTopBar(), buildMonthBar(), buildFilterBar(), configureGrid());
    setFlexGrow(1, grid);

    List<WardOption> wards = service.wardsFor(email);
    wardSelect.setItems(wards);
    if (wards.isEmpty()) {
      Notification.show("No ward is assigned to your account yet.", 5000,
          Notification.Position.BOTTOM_END);
    } else {
      wardSelect.setValue(wards.getFirst());
      wardSelect.addValueChangeListener(event -> reload());
      reload();
    }
  }

  private HorizontalLayout buildTopBar() {
    H2 heading = new H2("Ward schedule");
    heading.addClassName("board-heading");
    Span subtitle = new Span(admin
        ? "Monthly matrix · tap or double-click a cell to edit"
        : "Monthly matrix · tap a shift to see its details");
    subtitle.addClassName("board-subtitle");
    VerticalLayout title = new VerticalLayout(heading, subtitle);
    title.setPadding(false);
    title.setSpacing(false);

    wardSelect.setItemLabelGenerator(WardOption::name);
    wardSelect.setWidth("260px");

    HorizontalLayout topBar = new HorizontalLayout(title, wardSelect);
    topBar.setWidthFull();
    topBar.setAlignItems(FlexComponent.Alignment.END);
    topBar.expand(title);
    return topBar;
  }

  private HorizontalLayout buildMonthBar() {
    Button previous = new Button(VaadinIcon.CHEVRON_LEFT.create(), event -> changeMonth(-1));
    previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    previous.setAriaLabel("Previous month");

    Button next = new Button(VaadinIcon.CHEVRON_RIGHT.create(), event -> changeMonth(1));
    next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    next.setAriaLabel("Next month");

    Button today = new Button("Today", event -> {
      month = YearMonth.now(ZONE);
      reload();
    });
    today.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    monthLabel.addClassName("board-month");
    statusBadge.addClassName("roster-status");
    countLabel.addClassName("board-count");

    HorizontalLayout monthBar =
        new HorizontalLayout(previous, monthLabel, next, today, statusBadge, countLabel);
    monthBar.setAlignItems(FlexComponent.Alignment.CENTER);
    return monthBar;
  }

  private HorizontalLayout buildFilterBar() {
    nameFilter.setPlaceholder("Search name");
    nameFilter.setClearButtonVisible(true);
    nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
    nameFilter.setWidth("200px");
    nameFilter.addValueChangeListener(event -> applyFilters());

    qualificationFilter.setItems(Qualification.values());
    qualificationFilter.setItemLabelGenerator(ScheduleBoardView::qualificationLabel);
    qualificationFilter.setClearButtonVisible(true);
    qualificationFilter.setWidth("170px");
    qualificationFilter.addValueChangeListener(event -> applyFilters());

    shiftFilter.setItemLabelGenerator(ScheduleBoardView::shiftFilterLabel);
    shiftFilter.setClearButtonVisible(true);
    shiftFilter.setWidth("180px");
    shiftFilter.addValueChangeListener(event -> applyFilters());

    leadFilter.addValueChangeListener(event -> applyFilters());

    HorizontalLayout filterBar =
        new HorizontalLayout(nameFilter, qualificationFilter, shiftFilter, leadFilter);
    filterBar.setWrap(true);
    filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);
    return filterBar;
  }

  private Grid<Row> configureGrid() {
    grid.setSizeFull();
    grid.setColumnReorderingAllowed(false);
    grid.setEmptyStateText("No staff match the current filters.");
    grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER,
        GridVariant.LUMO_ROW_STRIPES);
    grid.addClassName("schedule-grid");
    return grid;
  }

  private void reload() {
    WardOption ward = wardSelect.getValue();
    if (ward == null) {
      return;
    }
    try {
      board = service.load(ward.id(), month);
    } catch (RuntimeException ex) {
      String text = ex.getMessage() == null || ex.getMessage().isBlank()
          ? "Could not load the schedule."
          : ex.getMessage();
      Notification.show(text, 5000, Notification.Position.BOTTOM_END);
      return;
    }
    editable = admin && board.status() != RosterStatus.LOCKED;
    monthLabel.setText(month.format(MONTH_FORMAT));
    updateStatusBadge();
    updateShiftFilter();
    rebuildColumns();
    applyFilters();
  }

  private void updateStatusBadge() {
    RosterStatus status = board.status();
    statusBadge.setClassName("roster-status");
    if (status == null) {
      statusBadge.setText("No roster period");
      statusBadge.addClassName("none");
      statusBadge.getElement().setAttribute("title",
          "Saving an assignment creates a draft roster period for this month.");
    } else {
      statusBadge.setText(status.name());
      statusBadge.addClassName(status.name().toLowerCase(Locale.ENGLISH));
      statusBadge.getElement().setAttribute("title", statusTooltip(status));
    }
  }

  private void updateShiftFilter() {
    List<String> codes = new ArrayList<>();
    board.shiftTypes().forEach(type -> codes.add(type.code()));
    codes.addAll(STATUS_CODES);
    String selected = shiftFilter.getValue();
    shiftFilter.setItems(codes);
    if (selected != null) {
      if (codes.contains(selected)) {
        shiftFilter.setValue(selected);
      } else {
        shiftFilter.clear();
      }
    }
  }

  private void rebuildColumns() {
    grid.removeAllColumns();
    grid.addComponentColumn(this::staffCell)
        .setHeader("Staff")
        .setWidth("240px")
        .setFlexGrow(0)
        .setFrozen(true);

    int days = board.month().lengthOfMonth();
    for (int day = 1; day <= days; day++) {
      LocalDate date = board.month().atDay(day);
      grid.addComponentColumn(row -> shiftCell(row, date))
          .setHeader(dayHeader(date))
          .setWidth("60px")
          .setFlexGrow(0)
          .setResizable(false);
    }
  }

  private void applyFilters() {
    if (board == null) {
      return;
    }
    String name = nameFilter.getValue() == null
        ? ""
        : nameFilter.getValue().trim().toLowerCase(Locale.ENGLISH);
    Qualification qualification = qualificationFilter.getValue();
    String shift = shiftFilter.getValue();
    boolean leadOnly = leadFilter.getValue();

    List<Row> filtered = board.rows().stream()
        .filter(row -> name.isEmpty()
            || row.fullName().toLowerCase(Locale.ENGLISH).contains(name))
        .filter(row -> qualification == null || row.qualification() == qualification)
        .filter(row -> !leadOnly || row.leadQualified())
        .filter(row -> shift == null
            || row.cells().values().stream().anyMatch(cell -> cell.code().equals(shift)))
        .toList();

    grid.setItems(filtered);
    countLabel.setText(filtered.size() + " / " + board.rows().size() + " staff");
  }

  private Component staffCell(Row row) {
    Div cell = new Div();
    cell.addClassName("staff-cell");

    Span name = new Span(row.fullName());
    name.addClassName("staff-name");

    Span meta = new Span(qualificationLabel(row.qualification())
        + (row.leadQualified() ? " · Lead" : ""));
    meta.addClassName("staff-meta");

    cell.add(name, meta);
    return cell;
  }

  private Component shiftCell(Row row, LocalDate date) {
    ShiftCell cell = row.cells().get(date);
    Span badge = new Span();
    badge.addClassName("shift-badge");
    if (isWeekend(date)) {
      badge.addClassName("weekend");
    }
    if (cell == null) {
      badge.addClassName("shift-empty");
      badge.setText(editable ? "+" : "");
    } else {
      badge.addClassName(badgeClass(cell));
      badge.setText(cell.code());
      badge.getElement().setAttribute("title", tooltip(cell));
    }
    if (editable || cell != null) {
      badge.addClassName("clickable");
      badge.addClickListener(event -> openDialog(row, date, cell));
    }
    return badge;
  }

  private void openDialog(Row row, LocalDate date, ShiftCell cell) {
    WardOption ward = wardSelect.getValue();
    if (ward == null || board == null) {
      return;
    }
    new ShiftAssignmentDialog(service, ward, row, date, cell, board.shiftTypes(),
        editable, email, this::reload).open();
  }

  private void changeMonth(long delta) {
    month = month.plusMonths(delta);
    reload();
  }

  private static Component dayHeader(LocalDate date) {
    VerticalLayout header = new VerticalLayout();
    header.setPadding(false);
    header.setSpacing(false);
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.addClassName("day-header");
    if (isWeekend(date)) {
      header.addClassName("weekend");
    }

    Span dayOfWeek = new Span(
        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
    dayOfWeek.addClassName("day-dow");
    Span dayNumber = new Span(Integer.toString(date.getDayOfMonth()));
    dayNumber.addClassName("day-num");
    header.add(dayOfWeek, dayNumber);
    return header;
  }

  private static boolean isWeekend(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
  }

  private static String badgeClass(ShiftCell cell) {
    return switch (cell.code()) {
      case "E" -> "shift-early";
      case "L" -> "shift-late";
      case "N" -> "shift-night";
      case "ONCALL" -> "shift-oncall";
      case "OFF" -> "shift-off";
      case "VAC" -> "shift-vacation";
      case "SICK" -> "shift-sick";
      default -> "shift-empty";
    };
  }

  private static String tooltip(ShiftCell cell) {
    String base = cell.label() + " · " + cell.status().name().replace('_', ' ');
    return cell.note() == null || cell.note().isBlank() ? base : base + " · " + cell.note();
  }

  private static String qualificationLabel(Qualification qualification) {
    return switch (qualification) {
      case RN -> "RN";
      case ICU_SPECIALIST -> "ICU specialist";
      case ASSISTANT -> "Assistant";
    };
  }

  private static String shiftFilterLabel(String code) {
    return switch (code) {
      case "E" -> "Early (E)";
      case "L" -> "Late (L)";
      case "N" -> "Night (N)";
      case "ONCALL" -> "On-call";
      case "OFF" -> "Day off";
      case "VAC" -> "Vacation";
      case "SICK" -> "Sick leave";
      default -> code;
    };
  }

  private static String statusTooltip(RosterStatus status) {
    return switch (status) {
      case DRAFT -> "Draft — only managers can see this version.";
      case PUBLISHED -> "Published — staff can see this version; managers can still edit it.";
      case LOCKED -> "Locked — the month is frozen and cannot be edited.";
    };
  }

  private static String currentEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new IllegalStateException("No authenticated user for the schedule board.");
    }
    return authentication.getName();
  }

  private static boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getAuthorities().stream()
        .anyMatch(granted -> "ROLE_ADMIN".equals(granted.getAuthority()));
  }
}
