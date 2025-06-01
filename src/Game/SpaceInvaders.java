package Game;

import javax.swing.*;

public class SpaceInvaders {
    public static void main(String[] args) {
        // create the game window
        JFrame frame = new JFrame("Space Invaders");

        // add the game screen
        GamePanel panel = new GamePanel();

        // close the window when user clicks X
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set window size
        frame.setSize(800, 600);

        // don't allow resizing
        frame.setResizable(false);

        // add the panel to the window
        frame.add(panel);

        // show the window
        frame.setVisible(true);

        // focus on the panel for keyboard input
        panel.requestFocusInWindow();
    }
}
