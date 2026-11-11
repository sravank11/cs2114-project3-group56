package model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tests the Customer class.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class CustomerTest
    extends student.TestCase
{
    private Order order;
    private Customer customer;

    /**
     * Creates a customer with ID 5, one burger, and 5000 ms of patience
     * before each test.
     */
    public void setUp()
    {
        Map<MenuItem, Integer> items =
            new EnumMap<MenuItem, Integer>(MenuItem.class);
        items.put(MenuItem.BURGER, 1);
        order = new Order(items);
        customer = new Customer(5, order, 5000);
    }


    /**
     * Tests the constructor sets every field and starts WAITING.
     */
    public void testConstructor()
    {
        assertEquals(5, customer.getId());
        assertEquals(order, customer.getOrder());
        assertEquals(5000, customer.getPatienceMs());
        assertEquals(5000, customer.getMaxPatienceMs());
        assertEquals(CustomerState.WAITING, customer.getState());
        assertTrue(customer.isActive());
    }


    /**
     * Tests the constructor rejects a non-positive ID, a null order, and
     * non-positive patience.
     */
    public void testConstructorBadInput()
    {
        try
        {
            new Customer(-1, order, 5000);
            fail("negative id should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Customer(0, order, 5000);
            fail("zero id should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Customer(1, null, 5000);
            fail("null order should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Customer(1, order, 0);
            fail("zero patience should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            new Customer(1, order, -100);
            fail("negative patience should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }


    /**
     * Tests the id and order stay the same after the state changes.
     */
    public void testIdAndOrderNeverChange()
    {
        customer.decreasePatience(5000);
        assertEquals(CustomerState.IMPATIENT, customer.getState());
        assertEquals(5, customer.getId());
        assertEquals(order, customer.getOrder());

        customer.setState(CustomerState.REMOVED);
        assertEquals(5, customer.getId());
        assertEquals(order, customer.getOrder());
    }


    /**
     * Tests decreasePatience counts down while keeping max patience.
     */
    public void testDecreasePatience()
    {
        customer.decreasePatience(1000);
        assertEquals(4000, customer.getPatienceMs());
        assertEquals(5000, customer.getMaxPatienceMs());
        assertEquals(CustomerState.WAITING, customer.getState());

        customer.decreasePatience(0);
        assertEquals(4000, customer.getPatienceMs());
    }


    /**
     * Tests patience reaching exactly zero makes the customer impatient.
     */
    public void testDecreasePatienceToExactlyZero()
    {
        customer.decreasePatience(5000);
        assertEquals(0, customer.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, customer.getState());
        assertTrue(customer.isActive());
    }


    /**
     * Tests patience stops at zero instead of going negative.
     */
    public void testDecreasePatiencePastZero()
    {
        customer.decreasePatience(9000);
        assertEquals(0, customer.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, customer.getState());

        customer.decreasePatience(1000);
        assertEquals(0, customer.getPatienceMs());
        assertEquals(CustomerState.IMPATIENT, customer.getState());
    }


    /**
     * Tests decreasePatience rejects negative time and leaves patience
     * unchanged.
     */
    public void testDecreasePatienceNegative()
    {
        try
        {
            customer.decreasePatience(-100);
            fail("negative elapsed time should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(5000, customer.getPatienceMs());
    }


    /**
     * Tests decreasePatience does nothing once the customer has left.
     */
    public void testDecreasePatienceAfterLeaving()
    {
        customer.decreasePatience(1000);
        customer.setState(CustomerState.SERVED);
        customer.decreasePatience(5000);
        assertEquals(4000, customer.getPatienceMs());
        assertEquals(CustomerState.SERVED, customer.getState());
    }


    /**
     * Tests every legal move out of WAITING.
     */
    public void testSetStateFromWaiting()
    {
        customer.setState(CustomerState.IMPATIENT);
        assertEquals(CustomerState.IMPATIENT, customer.getState());

        Customer served = new Customer(1, order, 5000);
        served.setState(CustomerState.SERVED);
        assertEquals(CustomerState.SERVED, served.getState());
        assertFalse(served.isActive());

        Customer failed = new Customer(2, order, 5000);
        failed.setState(CustomerState.FAILED);
        assertEquals(CustomerState.FAILED, failed.getState());
        assertFalse(failed.isActive());
    }


    /**
     * Tests every legal move out of IMPATIENT.
     */
    public void testSetStateFromImpatient()
    {
        customer.setState(CustomerState.IMPATIENT);
        customer.setState(CustomerState.SERVED);
        assertEquals(CustomerState.SERVED, customer.getState());

        Customer removed = new Customer(1, order, 5000);
        removed.setState(CustomerState.IMPATIENT);
        removed.setState(CustomerState.REMOVED);
        assertEquals(CustomerState.REMOVED, removed.getState());
        assertFalse(removed.isActive());

        Customer failed = new Customer(2, order, 5000);
        failed.setState(CustomerState.IMPATIENT);
        failed.setState(CustomerState.FAILED);
        assertEquals(CustomerState.FAILED, failed.getState());
    }


    /**
     * Tests a WAITING customer cannot be refunded or set to WAITING again.
     */
    public void testSetStateIllegalFromWaiting()
    {
        try
        {
            customer.setState(CustomerState.REMOVED);
            fail("WAITING -> REMOVED should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }

        try
        {
            customer.setState(CustomerState.WAITING);
            fail("WAITING -> WAITING should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(CustomerState.WAITING, customer.getState());
    }


    /**
     * Tests an IMPATIENT customer cannot go back to WAITING.
     */
    public void testSetStateIllegalFromImpatient()
    {
        customer.setState(CustomerState.IMPATIENT);
        try
        {
            customer.setState(CustomerState.WAITING);
            fail("IMPATIENT -> WAITING should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(CustomerState.IMPATIENT, customer.getState());
    }


    /**
     * Tests a SERVED customer can never change state again.
     */
    public void testSetStateAfterServed()
    {
        customer.setState(CustomerState.SERVED);
        for (CustomerState next : CustomerState.values())
        {
            try
            {
                customer.setState(next);
                fail("SERVED -> " + next + " should throw");
            }
            catch (IllegalArgumentException e)
            {
                assertNotNull(e.getMessage());
            }
        }
        assertEquals(CustomerState.SERVED, customer.getState());
    }


    /**
     * Tests setState rejects null.
     */
    public void testSetStateNull()
    {
        try
        {
            customer.setState(null);
            fail("setState(null) should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(CustomerState.WAITING, customer.getState());
    }


    /**
     * Tests toString shows the id, state and patience.
     */
    public void testToString()
    {
        assertEquals("Customer #5 (WAITING, 5000 ms)", customer.toString());
        customer.decreasePatience(5000);
        assertEquals("Customer #5 (IMPATIENT, 0 ms)", customer.toString());
    }
}
