package dev.bachi.shiftworkr.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import dev.bachi.shiftworkr.service.ScheduleBoardService;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Row;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftCell;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftTypeOption;
import dev.bachi.shiftworkr.service.ScheduleBoardService.WardOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single-tap / double-click editor for one cell of the schedule board.
 *
 * <p>Live compliance preview is intentionally absent until the rule engine
 * lands in Phase 4; the lead-qualification hint is already in place so the
 * preview can extend this dialog.</p>
 */
public class ShiftAssignmentDialog extends Dialog {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private record ShiftOption(String code, String label) {
  }

  private final ScheduleBoardService service;
  private final WardOption ward;
  private final Row row;
  private final LocalDate date;
  private final String actorEmail;
  private final Runnable onSaved;

  private final ComboBox<ShiftOption> shiftSelect = new ComboBox<>("Shift");
  private final TextArea noteField = new TextArea("Note");

  public ShiftAssignmentDialog(ScheduleBoardService service, WardOption ward, Row row,
      LocalDate date, ShiftCell cell, List<ShiftTypeOption> shiftTypes, boolean editable,
      String actorEmail, Runnable onSaved) {
    this.service = service;
    this.ward = ward;
    this.row = row;
    this.date = date;
    this.actorEmail = actorEmail;
    this.onSaved = onSaved;

    setHeaderTitle(row.fullName());
    setWidth("440px");

    Span subtitle = new Span(date.format(DATE_FORMAT) + " · " + ward.name());
    subtitle.addClassName("dialog-subtitle");

    configureShiftSelect(cell, shiftTypes, editable);
    configureNoteField(cell, editable);

    VerticalLayout content = new VerticalLayout(buildLeadHint(), shiftSelect, noteField);
    content.setPadding(false);
    content.setSpacing(true);
    add(subtitle, content);

    Button close = new Button(editable ? "Cancel" : "Close", event -> close());
    getFooter().add(close);
    if (editable) {
      Button save = new Button("Save", event -> save());
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      getFooter().add(save);
    }
  }

  private Span buildLeadHint() {
    Span hint = new Span(row.leadQualified()
        ? "Lead-qualified — can cover the lead requirement"
        : "Not lead-qualified — lead coverage needs a colleague");
    hint.addClassName("lead-hint");
    hint.addClassName(row.leadQualified() ? "lead-yes" : "lead-no");
    return hint;
  }

  private void configureShiftSelect(ShiftCell cell, List<ShiftTypeOption> shiftTypes,
      boolean editable) {
    List<ShiftOption> options = new ArrayList<>();
    for (ShiftTypeOption type : shiftTypes) {
      String suffix = type.endDayOffset() == 1 ? " (+1)" : "";
      options.add(new ShiftOption(type.code(),
          type.name() + " · " + type.startTime().format(TIME_FORMAT) + "–"
              + type.endTime().format(TIME_FORMAT) + suffix));
    }
    options.add(new ShiftOption("OFF", "Day off"));
    options.add(new ShiftOption("VAC", "Vacation"));
    options.add(new ShiftOption("SICK", "Sick leave"));

    shiftSelect.setItems(options);
    shiftSelect.setItemLabelGenerator(ShiftOption::label);
    shiftSelect.setWidthFull();
    shiftSelect.setReadOnly(!editable);
    shiftSelect.setRequiredIndicatorVisible(editable);
    String currentCode = cell == null ? "E" : cell.code();
    shiftSelect.setValue(options.stream()
        .filter(option -> option.code().equals(currentCode))
        .findFirst()
        .orElse(options.getFirst()));
  }

  private void configureNoteField(ShiftCell cell, boolean editable) {
    noteField.setWidthFull();
    noteField.setMaxLength(500);
    noteField.setValue(cell == null || cell.note() == null ? "" : cell.note());
    noteField.setReadOnly(!editable);
  }

  private void save() {
    ShiftOption option = shiftSelect.getValue();
    if (option == null) {
      shiftSelect.setInvalid(true);
      shiftSelect.setErrorMessage("Choose a shift before saving.");
      return;
    }
    try {
      service.saveAssignment(ward.id(), row.staffId(), date, option.code(),
          noteField.getValue(), actorEmail);
      onSaved.run();
      close();
      Notification.show("Shift saved", 2500, Notification.Position.BOTTOM_END);
    } catch (RuntimeException ex) {
      String message = ex.getMessage() == null ? "Could not save the shift." : ex.getMessage();
      Notification.show(message, 5000, Notification.Position.BOTTOM_END);
    }
  }
}
