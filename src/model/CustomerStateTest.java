package model;

/**
 * Tests the CustomerState enum.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public class CustomerStateTest extends student.TestCase
{

    /**
     * Tests that all customer states exist.
     */
    public void testValues()
    {
        CustomerState[] states = CustomerState.values();

        assertEquals(5, states.length);
        assertEquals(CustomerState.WAITING, states[0]);
        assertEquals(CustomerState.IMPATIENT, states[1]);
        assertEquals(CustomerState.SERVED, states[2]);
        assertEquals(CustomerState.REMOVED, states[3]);
        assertEquals(CustomerState.FAILED, states[4]);
    }


    /**
     * Tests retrieving each customer state by its name.
     */
    public void testValueOf()
    {
        assertEquals(CustomerState.WAITING,
            CustomerState.valueOf("WAITING"));
        assertEquals(CustomerState.IMPATIENT,
            CustomerState.valueOf("IMPATIENT"));
        assertEquals(CustomerState.SERVED,
            CustomerState.valueOf("SERVED"));
        assertEquals(CustomerState.REMOVED,
            CustomerState.valueOf("REMOVED"));
        assertEquals(CustomerState.FAILED,
            CustomerState.valueOf("FAILED"));
    }


    /**
     * Tests that an invalid customer state throws an exception.
     */
    public void testInvalidValueOf()
    {
        try
        {
            CustomerState.valueOf("COOKING");
            fail("invalid customer state should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }
}