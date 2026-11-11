package model;

/**
 * Tests the MenuItem enum.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public class MenuItemTest extends student.TestCase
{

    /**
     * Tests that all menu items exist.
     */
    public void testValues()
    {
        MenuItem[] items = MenuItem.values();

        assertEquals(3, items.length);
        assertEquals(MenuItem.BURGER, items[0]);
        assertEquals(MenuItem.FRIES, items[1]);
        assertEquals(MenuItem.DRINK, items[2]);
    }


    /**
     * Tests retrieving each menu item by its name.
     */
    public void testValueOf()
    {
        assertEquals(MenuItem.BURGER, MenuItem.valueOf("BURGER"));
        assertEquals(MenuItem.FRIES, MenuItem.valueOf("FRIES"));
        assertEquals(MenuItem.DRINK, MenuItem.valueOf("DRINK"));
    }


    /**
     * Tests that an invalid menu item name throws an exception.
     */
    public void testInvalidValueOf()
    {
        try
        {
            MenuItem.valueOf("PIZZA");
            fail("invalid menu item should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }
}