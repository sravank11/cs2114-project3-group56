package model;

import java.util.Random;
import java.util.Map;

/** Tests VIP priority behavior independently of the GUI. */
public class VipQueueTest extends student.TestCase
{
    private Game game;

    public void setUp()
    {
        game = new Game(new Random(7), 100000);
    }

    public void testVipGoesBeforeRegularCustomer()
    {
        game.spawnCustomer();
        int regular = game.getFrontCustomer().getId();
        Customer vip = game.spawnVipCustomer();
        assertTrue(game.isVip(vip.getId()));
        assertEquals(vip, game.getFrontCustomer());
        assertEquals(2, game.getCustomers().size());
        assertEquals(regular, game.getCustomers().get(1).getId());
        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            game.serveOrder(regular));
    }

    public void testVipRejectedAfterGameOver()
    {
        game.spawnCustomer();
        game.tick(30001);
        game.spawnCustomer();
        game.tick(30001);
        game.spawnCustomer();
        game.tick(30001);
        assertTrue(game.isGameOver());
        assertNull(game.spawnVipCustomer());
        assertTrue(game.getCustomers().isEmpty());
    }

    public void testMultipleVipCustomersKeepTheirOrder()
    {
        Customer first = game.spawnVipCustomer();
        Customer second = game.spawnVipCustomer();
        assertEquals(first, game.getCustomers().get(0));
        assertEquals(second, game.getCustomers().get(1));
        assertFalse(game.isVip(-1));
    }

    /** Prepares the exact food that a customer ordered. */
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

    /** A correct VIP order succeeds and the regular line remains intact. */
    public void testServeVipCorrectOrder()
    {
        game.spawnCustomer();
        Customer regular = game.getFrontCustomer();
        Customer vip = game.spawnVipCustomer();
        fillTrayFor(vip);

        assertEquals(ServeResult.SERVED_OK, game.serveOrder(vip.getId()));
        assertEquals(CustomerState.SERVED, vip.getState());
        assertEquals(Game.POINTS_CORRECT, game.getScore());
        assertEquals(0, game.getStrikes());
        assertTrue(game.getTray().isEmpty());
        assertEquals(regular, game.getFrontCustomer());
        assertEquals(1, game.getCustomers().size());
    }

    /** A second attempt to serve a VIP that has left is safely rejected. */
    public void testCannotServeVipTwice()
    {
        Customer vip = game.spawnVipCustomer();
        fillTrayFor(vip);
        assertEquals(ServeResult.SERVED_OK, game.serveOrder(vip.getId()));
        int score = game.getScore();

        assertEquals(ServeResult.REJECTED_NO_CUSTOMER,
            game.serveOrder(vip.getId()));
        assertEquals(score, game.getScore());
        assertEquals(0, game.getStrikes());
    }

    /** An impatient VIP can be refunded once, even from the middle. */
    public void testRefundImpatientVipFromMiddle()
    {
        Customer first = game.spawnVipCustomer();
        Customer middle = game.spawnVipCustomer();
        Customer last = game.spawnVipCustomer();
        // First earn points so the refund penalty is visible in the score.
        fillTrayFor(first);
        assertEquals(ServeResult.SERVED_OK, game.serveOrder(first.getId()));
        int scoreBefore = game.getScore();
        // A newer VIP is still behind middle; expiry never reorders the line.
        game.tick(middle.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, middle.getState());
        assertTrue(game.removeImpatientCustomer(middle.getId()));
        assertEquals(CustomerState.REMOVED, middle.getState());
        assertEquals(scoreBefore - Game.REFUND_PENALTY, game.getScore());
        assertFalse(game.removeImpatientCustomer(middle.getId()));
        assertEquals(scoreBefore - Game.REFUND_PENALTY, game.getScore());
        assertEquals(last, game.getFrontCustomer());
    }

    /** An impatient VIP can leave through free service with no added points. */
    public void testServeImpatientVipForFree()
    {
        Customer vip = game.spawnVipCustomer();
        game.tick(vip.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, vip.getState());
        int score = game.getScore();

        assertEquals(ServeResult.REJECTED_IMPATIENT,
            game.serveOrder(vip.getId()));
        assertEquals(ServeResult.SERVED_OK, game.serveFree(vip.getId()));
        assertEquals(CustomerState.SERVED, vip.getState());
        assertTrue(game.getCustomers().isEmpty());
        assertEquals(score, game.getScore());
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            game.serveFree(vip.getId()));
    }

    /** A VIP who is still waiting cannot be refunded or served free. */
    public void testPatientVipRejectsRefundAndFreeService()
    {
        Customer vip = game.spawnVipCustomer();
        assertFalse(game.removeImpatientCustomer(vip.getId()));
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT,
            game.serveFree(vip.getId()));
        assertEquals(vip, game.getFrontCustomer());
        assertEquals(CustomerState.WAITING, vip.getState());
        assertEquals(0, game.getStrikes());
    }

    /** Serving the first VIP preserves priority for the next VIP. */
    public void testVipPriorityAfterServingFirstVip()
    {
        game.spawnCustomer();
        Customer regular = game.getFrontCustomer();
        Customer vipOne = game.spawnVipCustomer();
        Customer vipTwo = game.spawnVipCustomer();
        fillTrayFor(vipOne);
        assertEquals(ServeResult.SERVED_OK, game.serveOrder(vipOne.getId()));
        assertEquals(vipTwo, game.getFrontCustomer());
        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            game.serveOrder(regular.getId()));
        fillTrayFor(vipTwo);
        assertEquals(ServeResult.SERVED_OK, game.serveOrder(vipTwo.getId()));
        assertEquals(regular, game.getFrontCustomer());
    }

}
