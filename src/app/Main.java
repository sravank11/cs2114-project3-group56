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
    private static GameWindow current;

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(Main::startGame);
    }

    /** Opens a fresh game, closing the previous window if there is one. */
    private static void startGame()
    {
        if (current != null)
        {
            current.dispose();
        }
        Game game = new Game(new Random(), 6000);
        GameController controller = new GameController(game);
        current = new GameWindow(game, controller, Main::startGame);
        current.setVisible(true);
        game.spawnCustomer();
        controller.startClock();
    }
}
