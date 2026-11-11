package controller;

import javax.swing.Timer;
import model.Game;
import model.MenuItem;
import model.ServeResult;

/** Connects the Swing interface to the game model.
 * @author Sravan Surapaneni
 * @version November 2026
 */
public class GameController
{
    public static final int TICK_MS = 100;
    private final Game game;
    private final Timer clock;

    public GameController(Game game)
    {
        if (game == null)
        {
            throw new IllegalArgumentException("game must not be null");
        }
        this.game = game;
        clock = new Timer(TICK_MS, event -> {
            game.tick(TICK_MS);
            if (game.isGameOver())
            {
                stopClock();
            }
        });
    }

    public void startClock()
    {
        if (!game.isGameOver() && !clock.isRunning())
        {
            clock.start();
        }
    }

    public void stopClock()
    {
        clock.stop();
    }

    public void foodClicked(MenuItem item)
    {
        game.addFood(item);
    }

    public void clearClicked()
    {
        game.clearTray();
    }

    public ServeResult serveClicked(int customerId)
    {
        return game.serveOrder(customerId);
    }

    public ServeResult serveFreeClicked(int customerId)
    {
        return game.serveFree(customerId);
    }

    public boolean refundClicked(int customerId)
    {
        return game.removeImpatientCustomer(customerId);
    }
}
