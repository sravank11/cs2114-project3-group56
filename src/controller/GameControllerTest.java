package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.Game;
import model.MenuItem;
import model.ServeResult;

/**
 * Tests that user actions flow through GameController into the Game model
 * and that listeners (the view's role) hear about the changes. No Swing
 * window is created, and the controller's clock is never started.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class GameControllerTest
    extends student.TestCase
{
    private static final int NO_SPAWN = 1000000;

    private Game game;
    private GameController controller;
    private List<String> messages;

    /**
     * Creates a seeded game with one customer and a controller for it.
     */
    public void setUp()
    {
        game = new Game(new Random(42), NO_SPAWN);
        messages = new ArrayList<String>();
        game.addListener(messages::add);
        controller = new GameController(game);
        game.spawnCustomer();
        messages.clear();
    }


    /**
     * Puts the front customer's exact order on the tray via the controller.
     */
    private void fillTray()
    {
        for (Map.Entry<MenuItem, Integer> entry : game.getFrontCustomer()
            .getOrder().getItems().entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                controller.foodClicked(entry.getKey());
            }
        }
    }


    /**
     * A null game is rejected.
     */
    public void testConstructorNullGame()
    {
        Exception thrown = null;
        try
        {
            new GameController(null);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
    }


    /**
     * Clicking food changes the model's tray and notifies listeners.
     */
    public void testFoodClicked()
    {
        controller.foodClicked(MenuItem.BURGER);
        controller.foodClicked(MenuItem.BURGER);
        assertEquals(2, (int) game.getTray().get(MenuItem.BURGER));
        assertEquals(2, messages.size());
    }


    /**
     * Clicking a null food does nothing and does not crash.
     */
    public void testFoodClickedNull()
    {
        controller.foodClicked(null);
        assertTrue(game.getTray().isEmpty());
        assertTrue(messages.isEmpty());
    }


    /**
     * Clear empties the model's tray; clearing an empty tray is harmless.
     */
    public void testClearClicked()
    {
        controller.foodClicked(MenuItem.FRIES);
        controller.clearClicked();
        assertTrue(game.getTray().isEmpty());
        controller.clearClicked();
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Serving the correct order scores through the model.
     */
    public void testServeClickedCorrect()
    {
        int id = game.getFrontCustomer().getId();
        fillTray();
        assertEquals(ServeResult.SERVED_OK, controller.serveClicked(id));
        assertEquals(Game.POINTS_CORRECT, game.getScore());
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Serving a wrong order adds a strike; an unknown id is rejected with
     * nothing changed.
     */
    public void testServeClickedBadInput()
    {
        int id = game.getFrontCustomer().getId();
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            controller.serveClicked(id + 99));
        assertEquals(0, game.getStrikes());
        assertEquals(ServeResult.FAILED_ORDER, controller.serveClicked(id));
        assertEquals(1, game.getStrikes());
    }


    /**
     * Serving free a patient customer is rejected; unknown id too.
     */
    public void testServeFreeClicked()
    {
        int id = game.getFrontCustomer().getId();
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT,
            controller.serveFreeClicked(id));
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            controller.serveFreeClicked(-5));
        assertEquals(1, game.getCustomers().size());
    }


    /**
     * Serving free works once the customer has run out of patience.
     */
    public void testServeFreeClickedImpatient()
    {
        int id = game.getFrontCustomer().getId();
        game.tick(Game.MAX_PATIENCE_MS + 1);
        assertEquals(ServeResult.SERVED_OK, controller.serveFreeClicked(id));
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Refund removes an impatient customer and costs points; refunding a
     * patient or unknown customer returns false.
     */
    public void testRefundClicked()
    {
        int id = game.getFrontCustomer().getId();
        assertFalse(controller.refundClicked(id));
        assertFalse(controller.refundClicked(id + 99));
        game.tick(Game.MAX_PATIENCE_MS + 1);
        assertTrue(controller.refundClicked(id));
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * After game over, every click is rejected without crashing, and the
     * clock cannot be restarted.
     */
    public void testActionsAfterGameOver()
    {
        game.tick(Game.MAX_PATIENCE_MS + 1);
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.tick(Game.MAX_PATIENCE_MS + 1);
        }
        assertTrue(game.isGameOver());
        int id = game.getCustomers().isEmpty() ? 0
            : game.getFrontCustomer().getId();
        controller.foodClicked(MenuItem.BURGER);
        assertTrue(game.getTray().isEmpty());
        assertEquals(ServeResult.REJECTED_GAME_OVER,
            controller.serveClicked(id));
        assertEquals(ServeResult.REJECTED_GAME_OVER,
            controller.serveFreeClicked(id));
        assertFalse(controller.refundClicked(id));
        controller.startClock();
        controller.stopClock();
    }
}
