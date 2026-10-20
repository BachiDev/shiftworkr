package dev.bachi.shiftworkr.ui.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import dev.bachi.shiftworkr.domain.AssignmentStatus;
import dev.bachi.shiftworkr.domain.Qualification;
import dev.bachi.shiftworkr.domain.Role;
import dev.bachi.shiftworkr.domain.RosterStatus;
import dev.bachi.shiftworkr.service.ScheduleBoardService;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Board;
import dev.bachi.shiftworkr.service.ScheduleBoardService.Row;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftCell;
import dev.bachi.shiftworkr.service.ScheduleBoardService.ShiftTypeOption;
import dev.bachi.shiftworkr.service.ScheduleBoardService.WardOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Headless smoke test for the grid wiring (no Vaadin UI needed for construction). */
class ScheduleBoardViewTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rendersFrozenStaffColumnAndOneColumnPerDay() {
    ScheduleBoardService service = mock(ScheduleBoardService.class);
    UUID wardId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    YearMonth month = YearMonth.now(ZONE);
    LocalDate firstDay = month.atDay(1);

    when(service.wardsFor("admin@test.local"))
        .thenReturn(List.of(new WardOption(wardId, "Cardiology Ward 4B", "W4B")));
    when(service.load(wardId, month)).thenReturn(new Board(month, RosterStatus.PUBLISHED,
        List.of(new ShiftTypeOption("E", "Early", LocalTime.of(6, 0), LocalTime.of(14, 30), 0)),
        List.of(
            new Row(staffId, "Ada", "Admin", Qualification.RN, true, Role.ADMIN,
                Map.of(firstDay, new ShiftCell(UUID.randomUUID(), "E", "Early",
                    AssignmentStatus.SCHEDULED, null))),
            new Row(UUID.randomUUID(), "Nina", "Nurse", Qualification.RN, false, Role.NURSE,
                Map.of()))));

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("admin@test.local", "n/a",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

    ScheduleBoardView view = new ScheduleBoardView(service);

    Grid<?> grid = view.getChildren()
        .filter(Grid.class::isInstance)
        .map(Grid.class::cast)
        .findFirst()
        .orElseThrow();
    assertThat(grid.getColumns()).hasSize(month.lengthOfMonth() + 1);
    assertThat(grid.getColumns().getFirst().isFrozen()).isTrue();
    assertThat(grid.getListDataView().getItems().count()).isEqualTo(2);

    TextField nameFilter = view.getChildren()
        .filter(HorizontalLayout.class::isInstance)
        .map(HorizontalLayout.class::cast)
        .flatMap(Component::getChildren)
        .filter(TextField.class::isInstance)
        .map(TextField.class::cast)
        .filter(field -> "Staff".equals(field.getLabel()))
        .findFirst()
        .orElseThrow();
    nameFilter.setValue("Nina");

    assertThat(grid.getListDataView().getItems().count()).isEqualTo(1);
  }
}
