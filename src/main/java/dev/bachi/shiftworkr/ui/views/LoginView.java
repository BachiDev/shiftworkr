package dev.bachi.shiftworkr.ui.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Form login target for {@code SecurityConfig}; demo quick-logins arrive in Phase 5. */
@Route("login")
@PageTitle("Login | ShiftWorkr")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

  public LoginView() {
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    LoginForm login = new LoginForm();
    login.setAction("login");

    add(new H2("ShiftWorkr"),
        new Paragraph("Cardiology Ward 4B · staff login"),
        login);
  }
}
