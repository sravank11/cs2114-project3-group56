package app;

import java.util.Random;
import javax.swing.SwingUtilities;
import controller.GameController;
import model.Game;
import view.GameWindow;

/** Starts the Order Rush game.
 * @author Sravan Surapaneni
 * @version November 2026
 */
public class Main
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game(new Random(), 6000);
            GameController controller = new GameController(game);
            GameWindow window = new GameWindow(game, controller);
            window.setVisible(true);
            game.spawnCustomer();
            controller.startClock();
        });
    }
}
