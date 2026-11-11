package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tests the Game class.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class GameTest
    extends student.TestCase
{
    // Long enough that tick() never spawns anyone unless a test wants it to
    private static final int NO_SPAWN = 1000000;

    private Game game;
    private List<String> messages;

    /**
     * Creates a seeded game that records every listener message.
     */
    public void setUp()
    {
        game = new Game(new Random(42), NO_SPAWN);
        messages = new ArrayList<String>();
        game.addListener(messages::add);
    }


    // ----------------------------------------------------------
    /**
     * Puts exactly the customer's order on the tray.
     *
     * @param customer whose order to prepare
     */
    private void fillTrayFor(Customer customer)
    {
        for (Map.Entry<MenuItem, Integer> entry : customer.getOrder()
            .getItems().entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                game.addFood(entry.getKey());
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Puts the customer's order on the tray minus one item.
     *
     * @param customer whose order to prepare
     */
    private void fillTrayMissingOne(Customer customer)
    {
        boolean skipped = false;
        for (Map.Entry<MenuItem, Integer> entry : customer.getOrder()
            .getItems().entrySet())
        {
            int count = entry.getValue();
            if (!skipped)
            {
                count--;
                skipped = true;
            }
            for (int i = 0; i < count; i++)
            {
                game.addFood(entry.getKey());
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Spawns a customer and lets only them run out of patience. Anyone
     * spawned after this call still has patience left. Adds one strike.
     *
     * @return the impatient customer
     */
    private Customer spawnImpatientCustomer()
    {
        game.spawnCustomer();
        List<Customer> line = game.getCustomers();
        Customer customer = line.get(line.size() - 1);
        game.tick(customer.getPatienceMs());
        return customer;
    }


    /**
     * Tests a new game starts empty with no score or strikes.
     */
    public void testConstructor()
    {
        assertEquals(0, game.getScore());
        assertEquals(0, game.getStrikes());
        assertFalse(game.isGameOver());
        assertNull(game.getFrontCustomer());
        assertTrue(game.getCustomers().isEmpty());
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Tests the constructor rejects a null Random and a non-positive spawn
     * interval.
     */
    public void testConstructorBadInput()
    {
        try
        {
            new Game(null, 5000);
            fail("null rng should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Game(new Random(), 0);
            fail("zero spawn interval should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Game(new Random(), -5);
            fail("negative spawn interval should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }


    /**
     * Tests a listener hears about changes to the game.
     */
    public void testAddListener()
    {
        game.spawnCustomer();
        assertEquals(1, messages.size());
        assertEquals("Customer #1 arrived", messages.get(0));

        game.addFood(MenuItem.BURGER);
        assertEquals(2, messages.size());
    }


    /**
     * Tests addListener rejects null.
     */
    public void testAddListenerNull()
    {
        try
        {
            game.addListener(null);
            fail("null listener should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }


    /**
     * Tests tick lowers every customer's patience, not just the front one.
     */
    public void testTickDecreasesAllPatience()
    {
        game.spawnCustomer();
        game.spawnCustomer();
        Customer first = game.getCustomers().get(0);
        Customer second = game.getCustomers().get(1);
        int firstBefore = first.getPatienceMs();
        int secondBefore = second.getPatienceMs();

        game.tick(1000);
        assertEquals(firstBefore - 1000, first.getPatienceMs());
        assertEquals(secondBefore - 1000, second.getPatienceMs());
        assertEquals(0, game.getStrikes());
    }


    /**
     * Tests tick spawns a customer each time the interval passes.
     */
    public void testTickSpawnsCustomers()
    {
        Game timed = new Game(new Random(1), 5000);
        timed.tick(4999);
        assertEquals(0, timed.getCustomers().size());

        timed.tick(1);
        assertEquals(1, timed.getCustomers().size());

        // Two intervals pass in one tick
        timed.tick(10000);
        assertEquals(3, timed.getCustomers().size());
    }


    /**
     * Tests tick rejects negative time and leaves patience unchanged.
     */
    public void testTickNegative()
    {
        game.spawnCustomer();
        int before = game.getFrontCustomer().getPatienceMs();
        try
        {
            game.tick(-100);
            fail("negative tick should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(before, game.getFrontCustomer().getPatienceMs());
    }


    /**
     * Tests a customer who runs out of patience becomes impatient, stays in
     * line, and adds exactly one strike.
     */
    public void testTickRunsOutOfPatience()
    {
        Customer customer = spawnImpatientCustomer();
        assertEquals(CustomerState.IMPATIENT, customer.getState());
        assertEquals(1, game.getStrikes());
        assertEquals(customer, game.getFrontCustomer());
        assertEquals("Customer #1 ran out of patience, strike added",
            messages.get(messages.size() - 1));

        // Ticking again does not add another strike for the same customer
        game.tick(5000);
        assertEquals(1, game.getStrikes());
    }


    /**
     * Tests only the customers who ran out become impatient.
     */
    public void testTickOnlyExpiredCustomersImpatient()
    {
        game.spawnCustomer();
        game.tick(Game.MAX_PATIENCE_MS - Game.MIN_PATIENCE_MS + 1);
        game.spawnCustomer();
        Customer older = game.getCustomers().get(0);
        Customer newer = game.getCustomers().get(1);

        game.tick(older.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, older.getState());
        assertEquals(CustomerState.WAITING, newer.getState());
        assertEquals(1, game.getStrikes());
    }


    /**
     * Tests tick does nothing after the game is over.
     */
    public void testTickAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        assertTrue(game.isGameOver());
        int heard = messages.size();

        game.tick(NO_SPAWN);
        assertTrue(game.getCustomers().isEmpty());
        assertEquals(heard, messages.size());
    }


    /**
     * Tests spawnCustomer adds customers to the back with new ids, an
     * order, and patience in range.
     */
    public void testSpawnCustomer()
    {
        game.spawnCustomer();
        game.spawnCustomer();
        List<Customer> line = game.getCustomers();
        assertEquals(2, line.size());
        assertEquals(1, line.get(0).getId());
        assertEquals(2, line.get(1).getId());

        for (Customer customer : line)
        {
            assertEquals(CustomerState.WAITING, customer.getState());
            assertNotNull(customer.getOrder());
            int itemCount = 0;
            for (int count : customer.getOrder().getItems().values())
            {
                itemCount += count;
            }
            assertTrue(itemCount >= 1 && itemCount <= Game.MAX_ORDER_ITEMS);
            assertTrue(customer.getPatienceMs() >= Game.MIN_PATIENCE_MS);
            assertTrue(customer.getPatienceMs() <= Game.MAX_PATIENCE_MS);
        }
    }


    /**
     * Tests ids keep going up after customers leave, so none are reused.
     */
    public void testSpawnCustomerIdsNotReused()
    {
        game.spawnCustomer();
        Customer first = game.getFrontCustomer();
        fillTrayFor(first);
        game.serveOrder(first.getId());

        game.spawnCustomer();
        assertEquals(2, game.getFrontCustomer().getId());
    }


    /**
     * Tests spawnCustomer does nothing after the game is over.
     */
    public void testSpawnCustomerAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        game.spawnCustomer();
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Tests addFood puts items on the tray and counts repeats.
     */
    public void testAddFood()
    {
        assertTrue(game.addFood(MenuItem.BURGER));
        assertEquals(1, (int) game.getTray().get(MenuItem.BURGER));

        assertTrue(game.addFood(MenuItem.BURGER));
        assertTrue(game.addFood(MenuItem.FRIES));
        assertEquals(2, (int) game.getTray().get(MenuItem.BURGER));
        assertEquals(1, (int) game.getTray().get(MenuItem.FRIES));
    }


    /**
     * Tests addFood rejects null and leaves the tray unchanged.
     */
    public void testAddFoodNull()
    {
        game.addFood(MenuItem.DRINK);
        try
        {
            game.addFood(null);
            fail("addFood(null) should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(1, game.getTray().size());
    }


    /**
     * Tests addFood returns false after the game is over.
     */
    public void testAddFoodAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        assertFalse(game.addFood(MenuItem.BURGER));
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Tests clearTray empties the tray.
     */
    public void testClearTray()
    {
        game.addFood(MenuItem.BURGER);
        game.addFood(MenuItem.FRIES);
        game.clearTray();
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Tests clearTray on an empty tray does nothing and sends no message.
     */
    public void testClearTrayAlreadyEmpty()
    {
        game.clearTray();
        assertTrue(game.getTray().isEmpty());
        assertTrue(messages.isEmpty());
    }


    /**
     * Tests serving the exact order earns points and the customer leaves.
     */
    public void testServeOrderCorrect()
    {
        game.spawnCustomer();
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        fillTrayFor(front);

        assertEquals(ServeResult.SERVED_OK, game.serveOrder(front.getId()));
        assertEquals(Game.POINTS_CORRECT, game.getScore());
        assertEquals(0, game.getStrikes());
        assertEquals(CustomerState.SERVED, front.getState());
        assertEquals(1, game.getCustomers().size());
        assertEquals(2, game.getFrontCustomer().getId());
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Tests serving with nobody in line is rejected and resets the tray.
     */
    public void testServeOrderEmptyQueue()
    {
        game.addFood(MenuItem.BURGER);
        assertEquals(ServeResult.REJECTED_NO_CUSTOMER, game.serveOrder(1));
        assertEquals(0, game.getScore());
        assertEquals(0, game.getStrikes());
        assertTrue(game.getTray().isEmpty());
        assertEquals("No customers to serve",
            messages.get(messages.size() - 1));
    }


    /**
     * Tests serving the right food plus extras applies the waste penalty
     * but no strike.
     */
    public void testServeOrderWithExtras()
    {
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        fillTrayFor(front);
        game.addFood(MenuItem.DRINK);

        assertEquals(ServeResult.SERVED_WITH_EXTRAS,
            game.serveOrder(front.getId()));
        assertEquals(Game.POINTS_CORRECT - Game.EXTRAS_PENALTY,
            game.getScore());
        assertEquals(0, game.getStrikes());
        assertEquals(CustomerState.SERVED, front.getState());
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Tests an incomplete order adds a strike and the customer leaves.
     */
    public void testServeOrderIncomplete()
    {
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        fillTrayMissingOne(front);

        assertEquals(ServeResult.FAILED_ORDER, game.serveOrder(front.getId()));
        assertEquals(0, game.getScore());
        assertEquals(1, game.getStrikes());
        assertEquals(CustomerState.FAILED, front.getState());
        assertTrue(game.getCustomers().isEmpty());
        assertTrue(game.getTray().isEmpty());
    }


    /**
     * Tests serving an empty tray counts as a failed order.
     */
    public void testServeOrderEmptyTray()
    {
        game.spawnCustomer();
        assertEquals(ServeResult.FAILED_ORDER,
            game.serveOrder(game.getFrontCustomer().getId()));
        assertEquals(1, game.getStrikes());
    }


    /**
     * Tests serving someone who is not at the window is rejected without
     * changing anything.
     */
    public void testServeOrderNotFront()
    {
        game.spawnCustomer();
        game.spawnCustomer();
        Customer second = game.getCustomers().get(1);
        fillTrayFor(second);

        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            game.serveOrder(second.getId()));
        assertEquals(0, game.getScore());
        assertEquals(0, game.getStrikes());
        assertEquals(2, game.getCustomers().size());
        assertEquals(1, game.getFrontCustomer().getId());
        assertEquals(CustomerState.WAITING, second.getState());
        assertFalse(game.getTray().isEmpty());
    }


    /**
     * Tests serving the same customer twice only scores once.
     */
    public void testServeOrderTwice()
    {
        game.spawnCustomer();
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        fillTrayFor(front);
        game.serveOrder(front.getId());

        fillTrayFor(front);
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveOrder(front.getId()));
        assertEquals(Game.POINTS_CORRECT, game.getScore());
        assertEquals(0, game.getStrikes());
        assertEquals(1, game.getCustomers().size());
    }


    /**
     * Tests serving an id that was never in line is rejected.
     */
    public void testServeOrderUnknownId()
    {
        game.spawnCustomer();
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveOrder(99));
        assertEquals(1, game.getCustomers().size());
        assertEquals(0, game.getStrikes());
    }


    /**
     * Tests an impatient customer at the window cannot be served normally,
     * even with the right food.
     */
    public void testServeOrderImpatient()
    {
        Customer customer = spawnImpatientCustomer();
        fillTrayFor(customer);

        assertEquals(ServeResult.REJECTED_IMPATIENT,
            game.serveOrder(customer.getId()));
        assertEquals(0, game.getScore());
        assertEquals(1, game.getStrikes());
        assertEquals(CustomerState.IMPATIENT, customer.getState());
        assertEquals(customer, game.getFrontCustomer());
        assertFalse(game.getTray().isEmpty());
    }


    /**
     * Tests serving after the game is over is rejected.
     */
    public void testServeOrderAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        assertEquals(ServeResult.REJECTED_GAME_OVER, game.serveOrder(4));
        assertEquals(Game.MAX_STRIKES, game.getStrikes());
        assertEquals(0, game.getScore());
    }


    /**
     * Tests serving an impatient customer for free removes them with no
     * points.
     */
    public void testServeFree()
    {
        Customer customer = spawnImpatientCustomer();
        assertEquals(ServeResult.SERVED_OK, game.serveFree(customer.getId()));
        assertEquals(CustomerState.SERVED, customer.getState());
        assertEquals(0, game.getScore());
        assertEquals(1, game.getStrikes());
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Tests serving free works for an impatient customer who is not at the
     * front.
     */
    public void testServeFreeNotFront()
    {
        game.spawnCustomer();
        game.tick(Game.MAX_PATIENCE_MS - Game.MIN_PATIENCE_MS + 1);
        game.spawnCustomer();
        Customer older = game.getCustomers().get(0);
        Customer newer = game.getCustomers().get(1);
        game.tick(newer.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, newer.getState());

        assertEquals(ServeResult.SERVED_OK, game.serveFree(newer.getId()));
        assertEquals(1, game.getCustomers().size());
        assertEquals(older, game.getFrontCustomer());
    }


    /**
     * Tests a customer who still has patience cannot be served free.
     */
    public void testServeFreeNotImpatient()
    {
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT,
            game.serveFree(front.getId()));
        assertEquals(CustomerState.WAITING, front.getState());
        assertEquals(1, game.getCustomers().size());
        assertEquals(0, game.getScore());
    }


    /**
     * Tests serving free twice, or an unknown id, is rejected.
     */
    public void testServeFreeAlreadyHandled()
    {
        Customer customer = spawnImpatientCustomer();
        game.serveFree(customer.getId());
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveFree(customer.getId()));
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveFree(99));
    }


    /**
     * Tests serving free after the game is over is rejected.
     */
    public void testServeFreeAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        assertEquals(ServeResult.REJECTED_GAME_OVER, game.serveFree(1));
    }


    /**
     * Tests refunding an impatient customer from the middle of the line
     * applies the penalty and keeps everyone else in order.
     */
    public void testRemoveImpatientCustomerMiddle()
    {
        // Earn some points first so the penalty is visible
        game.spawnCustomer();
        fillTrayFor(game.getFrontCustomer());
        game.serveOrder(game.getFrontCustomer().getId());

        game.spawnCustomer();
        game.spawnCustomer();
        game.tick(Game.MAX_PATIENCE_MS - Game.MIN_PATIENCE_MS + 1);
        game.spawnCustomer();
        Customer first = game.getCustomers().get(0);
        Customer middle = game.getCustomers().get(1);
        Customer last = game.getCustomers().get(2);
        game.tick(Math.max(first.getPatienceMs(), middle.getPatienceMs()));
        assertEquals(CustomerState.IMPATIENT, middle.getState());
        assertEquals(CustomerState.WAITING, last.getState());

        assertTrue(game.removeImpatientCustomer(middle.getId()));
        assertEquals(CustomerState.REMOVED, middle.getState());
        assertEquals(Game.POINTS_CORRECT - Game.REFUND_PENALTY,
            game.getScore());
        List<Customer> line = game.getCustomers();
        assertEquals(2, line.size());
        assertEquals(first, line.get(0));
        assertEquals(last, line.get(1));
    }


    /**
     * Tests the refund penalty cannot push the score below zero.
     */
    public void testRemoveImpatientCustomerScoreFloor()
    {
        Customer customer = spawnImpatientCustomer();
        assertTrue(game.removeImpatientCustomer(customer.getId()));
        assertEquals(0, game.getScore());
    }


    /**
     * Tests a customer with patience left cannot be refunded.
     */
    public void testRemoveImpatientCustomerNotImpatient()
    {
        game.spawnCustomer();
        Customer front = game.getFrontCustomer();
        assertFalse(game.removeImpatientCustomer(front.getId()));
        assertEquals(CustomerState.WAITING, front.getState());
        assertEquals(1, game.getCustomers().size());
    }


    /**
     * Tests the refund only happens once, and unknown ids are rejected.
     */
    public void testRemoveImpatientCustomerTwice()
    {
        game.spawnCustomer();
        fillTrayFor(game.getFrontCustomer());
        game.serveOrder(game.getFrontCustomer().getId());
        Customer customer = spawnImpatientCustomer();

        assertTrue(game.removeImpatientCustomer(customer.getId()));
        assertFalse(game.removeImpatientCustomer(customer.getId()));
        assertFalse(game.removeImpatientCustomer(99));
        assertEquals(Game.POINTS_CORRECT - Game.REFUND_PENALTY,
            game.getScore());
    }


    /**
     * Tests refunding after the game is over is rejected.
     */
    public void testRemoveImpatientCustomerAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            game.serveOrder(game.getFrontCustomer().getId());
        }
        assertFalse(game.removeImpatientCustomer(1));
    }


    /**
     * Tests three failed orders end the game and everyone left in line is
     * marked FAILED.
     */
    public void testGameOverAfterThreeFailures()
    {
        for (int i = 0; i < 5; i++)
        {
            game.spawnCustomer();
        }
        Customer waiting = game.getCustomers().get(4);

        game.serveOrder(1);
        game.serveOrder(2);
        assertFalse(game.isGameOver());
        assertEquals(2, game.getStrikes());

        game.serveOrder(3);
        assertTrue(game.isGameOver());
        assertEquals(Game.MAX_STRIKES, game.getStrikes());
        assertEquals(CustomerState.FAILED, waiting.getState());
        assertTrue(game.getCustomers().isEmpty());
        assertNull(game.getFrontCustomer());
        assertEquals("Game over! Final score: 0",
            messages.get(messages.size() - 1));
    }


    /**
     * Tests strikes never go past the maximum when many customers run out
     * of patience in the same tick.
     */
    public void testStrikesCappedWhenManyRunOut()
    {
        for (int i = 0; i < 5; i++)
        {
            game.spawnCustomer();
        }
        game.tick(Game.MAX_PATIENCE_MS);
        assertTrue(game.isGameOver());
        assertEquals(Game.MAX_STRIKES, game.getStrikes());
        assertTrue(game.getCustomers().isEmpty());
    }


    /**
     * Tests the score and strikes start at zero and the game is not over.
     */
    public void testGettersBeforeAnything()
    {
        assertEquals(0, game.getScore());
        assertEquals(0, game.getStrikes());
        assertFalse(game.isGameOver());
    }


    /**
     * Tests getFrontCustomer returns the first customer added.
     */
    public void testGetFrontCustomer()
    {
        assertNull(game.getFrontCustomer());
        game.spawnCustomer();
        game.spawnCustomer();
        assertEquals(1, game.getFrontCustomer().getId());
    }


    /**
     * Tests getCustomers returns the line in order and cannot be used to
     * change it.
     */
    public void testGetCustomers()
    {
        assertTrue(game.getCustomers().isEmpty());
        game.spawnCustomer();
        game.spawnCustomer();
        game.spawnCustomer();
        List<Customer> line = game.getCustomers();
        assertEquals(3, line.size());
        for (int i = 0; i < 3; i++)
        {
            assertEquals(i + 1, line.get(i).getId());
        }

        try
        {
            line.clear();
            fail("returned list should be read-only");
        }
        catch (UnsupportedOperationException e)
        {
            assertNotNull(e);
        }
        assertEquals(3, game.getCustomers().size());
    }


    /**
     * Tests getTray returns a read-only copy.
     */
    public void testGetTray()
    {
        assertTrue(game.getTray().isEmpty());
        game.addFood(MenuItem.FRIES);
        Map<MenuItem, Integer> tray = game.getTray();
        assertEquals(1, (int) tray.get(MenuItem.FRIES));

        try
        {
            tray.put(MenuItem.BURGER, 5);
            fail("returned tray should be read-only");
        }
        catch (UnsupportedOperationException e)
        {
            assertNotNull(e);
        }

        // An old copy does not change when the game's tray does
        game.addFood(MenuItem.FRIES);
        assertEquals(1, (int) tray.get(MenuItem.FRIES));
        assertNull(game.getTray().get(MenuItem.BURGER));
    }
}
