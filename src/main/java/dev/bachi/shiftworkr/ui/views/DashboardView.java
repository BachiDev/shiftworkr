package dev.bachi.shiftworkr.ui.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.bachi.shiftworkr.ui.MainLayout;

/** Phase 0 placeholder — schedule board lands in Phase 2. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | ShiftWorkr")
public class DashboardView extends VerticalLayout {

  public DashboardView() {
    add(new H2("Cardiology Ward 4B — Dashboard"),
        new Paragraph("Phase 0 scaffold OK. Schedule board (Phase 2), swaps (Phase 3), rules (Phase 4)."));
  }
}
