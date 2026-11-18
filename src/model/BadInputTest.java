package model;

import java.util.Map;
import java.util.Random;

/**
 * Regression tests for the bad-input cases in the Order Rush scope.
 * These tests exercise the model without opening the GUI.
 *
 * @author Sravan
 * @version November 2026
 */
public class BadInputTest extends student.TestCase
{
    private Game game;

    /** Starts each test with a predictable, empty game. */
    public void setUp()
    {
        game = new Game(new Random(42), 1000000);
    }

    /** Adds exactly the food in the customer's order. */
    private void fillTray(Customer customer)
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

    /** 1: An empty/incomplete order counts as one failure, not a crash. */
    public void testIncorrectOrIncompleteOrder()
    {
        game.spawnCustomer();
        Customer customer = game.getFrontCustomer();
        assertEquals(ServeResult.FAILED_ORDER,
            game.serveOrder(customer.getId()));
        assertEquals(CustomerState.FAILED, customer.getState());
        assertEquals(1, game.getStrikes());
        assertNull(game.getFrontCustomer());
        assertTrue(game.getTray().isEmpty());
    }

    /** 2: Extra food reduces points without adding a strike. */
    public void testExtraFoodIsWastePenaltyNotFailure()
    {
        game.spawnCustomer();
        Customer customer = game.getFrontCustomer();
        fillTray(customer);
        game.addFood(MenuItem.FRIES);
        assertEquals(ServeResult.SERVED_WITH_EXTRAS,
            game.serveOrder(customer.getId()));
        assertEquals(Game.POINTS_CORRECT - Game.EXTRAS_PENALTY,
            game.getScore());
        assertEquals(0, game.getStrikes());
        assertEquals(CustomerState.SERVED, customer.getState());
    }

    /** 3: Clicking serve with an empty queue is safely rejected. */
    public void testServeWithEmptyQueue()
    {
        game.addFood(MenuItem.DRINK);
        assertEquals(ServeResult.REJECTED_NO_CUSTOMER,
            game.serveOrder(1));
        assertTrue(game.getTray().isEmpty());
        assertEquals(0, game.getStrikes());
        assertEquals(0, game.getScore());
    }

    /** 4: Only the front customer, including VIP priority, can be served. */
    public void testCannotServeNonFrontCustomer()
    {
        game.spawnCustomer();
        Customer first = game.getFrontCustomer();
        game.spawnCustomer();
        Customer second = game.getCustomers().get(1);
        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            game.serveOrder(second.getId()));
        assertEquals(first, game.getFrontCustomer());
        assertEquals(2, game.getCustomers().size());
        assertEquals(0, game.getStrikes());

        Customer vip = game.spawnVipCustomer();
        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            game.serveOrder(first.getId()));
        assertEquals(vip, game.getFrontCustomer());
    }

    /** 5: Expiring exactly at click time forbids normal service. */
    public void testExpirationAtExactClickTime()
    {
        game.spawnCustomer();
        Customer customer = game.getFrontCustomer();
        game.tick(customer.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, customer.getState());
        assertEquals(1, game.getStrikes());
        fillTray(customer);
        assertEquals(ServeResult.REJECTED_IMPATIENT,
            game.serveOrder(customer.getId()));
        assertEquals(1, game.getStrikes());
        assertEquals(0, game.getScore());
        assertEquals(customer, game.getFrontCustomer());
        assertEquals(ServeResult.SERVED_OK,
            game.serveFree(customer.getId()));
    }

    /** 6: Repeated serve/free/refund clicks do not handle a customer twice. */
    public void testRepeatedClicksAreSafe()
    {
        game.spawnCustomer();
        Customer served = game.getFrontCustomer();
        fillTray(served);
        assertEquals(ServeResult.SERVED_OK,
            game.serveOrder(served.getId()));
        int score = game.getScore();
        assertEquals(ServeResult.REJECTED_NO_CUSTOMER,
            game.serveOrder(served.getId()));
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveFree(served.getId()));
        assertFalse(game.removeImpatientCustomer(served.getId()));
        assertEquals(score, game.getScore());
        assertEquals(0, game.getStrikes());
    }

    /** 7: All gameplay actions are ignored or rejected after game over. */
    public void testActionsAfterGameOver()
    {
        for (int i = 0; i < Game.MAX_STRIKES; i++)
        {
            game.spawnCustomer();
            Customer customer = game.getFrontCustomer();
            assertEquals(ServeResult.FAILED_ORDER,
                game.serveOrder(customer.getId()));
        }
        assertTrue(game.isGameOver());
        assertEquals(Game.MAX_STRIKES, game.getStrikes());
        int score = game.getScore();
        assertFalse(game.addFood(MenuItem.BURGER));
        game.clearTray();
        game.spawnCustomer();
        assertNull(game.spawnVipCustomer());
        game.tick(1000000);
        assertEquals(ServeResult.REJECTED_GAME_OVER,
            game.serveOrder(123));
        assertEquals(ServeResult.REJECTED_GAME_OVER,
            game.serveFree(123));
        assertFalse(game.removeImpatientCustomer(123));
        assertTrue(game.getCustomers().isEmpty());
        assertTrue(game.getTray().isEmpty());
        assertEquals(score, game.getScore());
        assertEquals(Game.MAX_STRIKES, game.getStrikes());
    }

    /** 8: An ID for an already-departed customer is never served again. */
    public void testAlreadyDepartedCustomerIsRejected()
    {
        game.spawnCustomer();
        Customer departed = game.getFrontCustomer();
        fillTray(departed);
        assertEquals(ServeResult.SERVED_OK,
            game.serveOrder(departed.getId()));
        game.spawnCustomer();
        Customer next = game.getFrontCustomer();
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveOrder(departed.getId()));
        assertEquals(next, game.getFrontCustomer());
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveFree(departed.getId()));
        assertFalse(game.removeImpatientCustomer(departed.getId()));
        assertEquals(0, game.getStrikes());
    }

    /** Expiring a customer in the middle does not reorder the queue. */
    public void testImpatientMiddleCustomerCanBeRemoved()
    {
        game.spawnCustomer();
        Customer first = game.getFrontCustomer();
        game.spawnCustomer();
        Customer middle = game.getCustomers().get(1);
        game.tick(middle.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, middle.getState());
        assertTrue(game.removeImpatientCustomer(middle.getId()));
        assertEquals(CustomerState.REMOVED, middle.getState());
        assertEquals(first, game.getFrontCustomer());
        assertEquals(1, game.getCustomers().size());
        assertFalse(game.removeImpatientCustomer(middle.getId()));
    }

    /** A patient customer cannot receive free service or a refund. */
    public void testCannotRefundOrServeFreeBeforeExpiration()
    {
        Customer vip = game.spawnVipCustomer();
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT,
            game.serveFree(vip.getId()));
        assertFalse(game.removeImpatientCustomer(vip.getId()));
        assertEquals(CustomerState.WAITING, vip.getState());
        assertEquals(vip, game.getFrontCustomer());
        assertEquals(0, game.getStrikes());
    }

    /** A refund penalty never makes the score negative. */
    public void testRefundAtZeroScoreDoesNotGoNegative()
    {
        Customer vip = game.spawnVipCustomer();
        game.tick(vip.getPatienceMs());
        assertTrue(game.removeImpatientCustomer(vip.getId()));
        assertEquals(0, game.getScore());
        assertFalse(game.removeImpatientCustomer(vip.getId()));
    }
}
