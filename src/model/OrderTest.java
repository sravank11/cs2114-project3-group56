package model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tests the Order class.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public class OrderTest extends student.TestCase
{
    private Order order;

    /**
     * Creates an order with one burger and one fries before each test.
     */
    public void setUp()
    {
        Map<MenuItem, Integer> items = new EnumMap<MenuItem, Integer>(MenuItem.class);
        items.put(MenuItem.BURGER, 1);
        items.put(MenuItem.FRIES, 1);

        order = new Order(items);
    }


    /**
     * Tests the constructor stores all requested items.
     */
    public void testConstructor()
    {
        assertEquals(1,
            (int) order.getItems().get(MenuItem.BURGER));
        assertEquals(1,
            (int) order.getItems().get(MenuItem.FRIES));
        assertFalse(order.getItems().containsKey(MenuItem.DRINK));
    }


    /**
     * Tests the constructor rejects null and empty maps, null menu items,
     * and non-positive quantities.
     */
    public void testConstructorBadInput()
    {
        try
        {
            new Order(null);
            fail("null items should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Order(new EnumMap<MenuItem, Integer>(MenuItem.class));
            fail("empty items should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        Map<MenuItem, Integer> zero = new EnumMap<MenuItem, Integer>(MenuItem.class);
        zero.put(MenuItem.BURGER, 0);

        try
        {
            new Order(zero);
            fail("zero quantity should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        Map<MenuItem, Integer> negative =
            new EnumMap<MenuItem, Integer>(MenuItem.class);
        negative.put(MenuItem.BURGER, -1);

        try
        {
            new Order(negative);
            fail("negative quantity should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }


    /**
     * Tests changes to the original map do not change the order.
     */
    public void testDefensiveCopy()
    {
        Map<MenuItem, Integer> items = new EnumMap<MenuItem, Integer>(MenuItem.class);
        items.put(MenuItem.BURGER, 1);

        Order newOrder = new Order(items);

        items.put(MenuItem.BURGER, 5);

        assertEquals(1,
            (int) newOrder.getItems().get(MenuItem.BURGER));
    }


    /**
     * Tests getItems does not allow changes to the order.
     */
    public void testGetItemsUnmodifiable()
    {
        try
        {
            order.getItems().put(MenuItem.DRINK, 1);
            fail("getItems should return an unmodifiable map");
        }
        catch (UnsupportedOperationException e)
        {
            assertNotNull(e);
        }
    }


    /**
     * Tests a tray that exactly matches the order.
     */
    public void testCompareToTrayExactMatch()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        tray.put(MenuItem.BURGER, 1);
        tray.put(MenuItem.FRIES, 1);

        assertEquals(ServeResult.SERVED_OK,
            order.compareToTray(tray));
    }


    /**
     * Tests a tray with extra menu items.
     */
    public void testCompareToTrayExtraItem()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        tray.put(MenuItem.BURGER, 1);
        tray.put(MenuItem.FRIES, 1);
        tray.put(MenuItem.DRINK, 1);

        assertEquals(ServeResult.SERVED_WITH_EXTRAS, order.compareToTray(tray));
    }


    /**
     * Tests a tray with too many of an ordered item.
     */
    public void testCompareToTrayExtraQuantity()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        tray.put(MenuItem.BURGER, 2);
        tray.put(MenuItem.FRIES, 1);

        assertEquals(ServeResult.SERVED_WITH_EXTRAS, order.compareToTray(tray));
    }


    /**
     * Tests a tray missing a required menu item.
     */
    public void testCompareToTrayMissingItem()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        tray.put(MenuItem.BURGER, 1);

        assertEquals(ServeResult.FAILED_ORDER,
            order.compareToTray(tray));
    }


    /**
     * Tests a tray with an insufficient quantity.
     */
    public void testCompareToTrayInsufficientQuantity()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        tray.put(MenuItem.BURGER, 1);
        tray.put(MenuItem.FRIES, 0);

        assertEquals(ServeResult.FAILED_ORDER,
            order.compareToTray(tray));
    }


    /**
     * Tests an empty tray.
     */
    public void testCompareToTrayEmpty()
    {
        Map<MenuItem, Integer> tray = new EnumMap<MenuItem, Integer>(MenuItem.class);

        assertEquals(ServeResult.FAILED_ORDER,
            order.compareToTray(tray));
    }


    /**
     * Tests a null tray.
     */
    public void testCompareToTrayNull()
    {
        try
        {
            order.compareToTray(null);
            fail("compareToTray(null) should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }
}