package dev.bachi.shiftworkr.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import dev.bachi.shiftworkr.ui.views.DashboardView;

/** Phase 0 responsive shell: sidebar nav + top bar + user profile pill placeholder. */
public class MainLayout extends AppLayout {

  public MainLayout() {
    DrawerToggle toggle = new DrawerToggle();

    H1 title = new H1("ShiftWorkr");
    title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

    Span profilePill = new Span("Phase 1 · secured");
    profilePill.addClassName("profile-pill");

    HorizontalLayout navbar = new HorizontalLayout(toggle, title, profilePill);
    navbar.setWidthFull();
    navbar.setAlignItems(FlexComponent.Alignment.CENTER);
    navbar.expand(title);
    addToNavbar(navbar);

    SideNav nav = new SideNav();
    nav.addItem(new SideNavItem("Dashboard", DashboardView.class));
    addToDrawer(nav);
  }
}
