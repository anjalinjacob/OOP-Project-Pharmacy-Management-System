import gui.LoginFrame;

import javax.swing.*;

/**
 * Application entry point. Launches the LoginFrame on the
 * Swing Event Dispatch Thread.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
