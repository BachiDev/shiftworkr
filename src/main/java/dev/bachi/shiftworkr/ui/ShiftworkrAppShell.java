package dev.bachi.shiftworkr.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Phase 0 shell: Lumo + custom stylesheet (Vaadin 25 theming model, no @Theme),
 * dark tokens via CSS, WebSocket push through Caddy (see forward-headers-strategy).
 */
@StyleSheet(Lumo.STYLESHEET)
@CssImport("./shiftworkr-styles.css")
@Push
@PWA(name = "ShiftWorkr", shortName = "ShiftWorkr")
public class ShiftworkrAppShell implements AppShellConfigurator {
}
