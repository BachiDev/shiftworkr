package dev.bachi.shiftworkr.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/** Phase 0 shell: dark theme + WebSocket push through Caddy (see forward-headers-strategy). */
@Theme("shiftworkr")
@Push
@PWA(name = "ShiftWorkr", shortName = "ShiftWorkr")
public class ShiftworkrAppShell implements AppShellConfigurator {
}
