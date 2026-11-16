package kz.aitu.eventstudio.ui;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

public final class UiTheme {
    private UiTheme() {}

    public static void install() throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        var keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource("Dialog", Font.PLAIN, 13));
            }
        }
    }
}
