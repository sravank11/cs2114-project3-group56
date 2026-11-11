package model;

/**
 * Tests the ServeResult enum.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public class ServeResultTest extends student.TestCase
{

    /**
     * Tests that all serve results exist.
     */
    public void testValues()
    {
        ServeResult[] results = ServeResult.values();

        assertEquals(9, results.length);
        assertEquals(ServeResult.SERVED_OK, results[0]);
        assertEquals(ServeResult.SERVED_WITH_EXTRAS, results[1]);
        assertEquals(ServeResult.FAILED_ORDER, results[2]);
        assertEquals(ServeResult.REJECTED_NO_CUSTOMER, results[3]);
        assertEquals(ServeResult.REJECTED_NOT_FRONT, results[4]);
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED, results[5]);
        assertEquals(ServeResult.REJECTED_GAME_OVER, results[6]);
        assertEquals(ServeResult.REJECTED_IMPATIENT, results[7]);
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT, results[8]);
    }


    /**
     * Tests retrieving each serve result by its name.
     */
    public void testValueOf()
    {
        assertEquals(ServeResult.SERVED_OK,
            ServeResult.valueOf("SERVED_OK"));
        assertEquals(ServeResult.SERVED_WITH_EXTRAS,
            ServeResult.valueOf("SERVED_WITH_EXTRAS"));
        assertEquals(ServeResult.FAILED_ORDER,
            ServeResult.valueOf("FAILED_ORDER"));
        assertEquals(ServeResult.REJECTED_NO_CUSTOMER,
            ServeResult.valueOf("REJECTED_NO_CUSTOMER"));
        assertEquals(ServeResult.REJECTED_NOT_FRONT,
            ServeResult.valueOf("REJECTED_NOT_FRONT"));
        assertEquals(ServeResult.REJECTED_ALREADY_HANDLED,
            ServeResult.valueOf("REJECTED_ALREADY_HANDLED"));
        assertEquals(ServeResult.REJECTED_GAME_OVER,
            ServeResult.valueOf("REJECTED_GAME_OVER"));
        assertEquals(ServeResult.REJECTED_IMPATIENT,
            ServeResult.valueOf("REJECTED_IMPATIENT"));
        assertEquals(ServeResult.REJECTED_NOT_IMPATIENT,
            ServeResult.valueOf("REJECTED_NOT_IMPATIENT"));
    }


    /**
     * Tests that an invalid serve result name throws an exception.
     */
    public void testInvalidValueOf()
    {
        try
        {
            ServeResult.valueOf("INVALID_RESULT");
            fail("invalid serve result should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
    }
}