package model;

/**
 * One customer in the drive-thru line: an id, the order they want, how
 * much patience they have left, and where they are in their lifecycle.
 *
 * The id, order and starting patience never change. Only patience and
 * state move, and state only moves forward:
 *   WAITING   -> IMPATIENT, SERVED, FAILED
 *   IMPATIENT -> SERVED, REMOVED, FAILED
 * Once a customer is SERVED, REMOVED or FAILED nothing can change them,
 * which is what stops repeat serves and actions on customers who left.
 *
 * The mutators are package-private so only Game can change a customer;
 * the view can read a customer but not modify one.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class Customer
{
    private final int id;
    private final Order order;
    private final int maxPatienceMs;
    private int patienceMs;
    private CustomerState state;

    /**
     * Creates a WAITING customer with full patience.
     *
     * @param id         unique customer id; must be positive
     * @param order      the meal this customer wants; must not be null
     * @param patienceMs starting patience in milliseconds; must be positive
     * @throws IllegalArgumentException if id or patienceMs is not positive
     *                                  or order is null
     */
    public Customer(int id, Order order, int patienceMs)
    {
        if (id <= 0)
        {
            throw new IllegalArgumentException("id must be positive: " + id);
        }
        if (order == null)
        {
            throw new IllegalArgumentException("order must not be null");
        }
        if (patienceMs <= 0)
        {
            throw new IllegalArgumentException(
                "patienceMs must be positive: " + patienceMs);
        }
        this.id = id;
        this.order = order;
        this.maxPatienceMs = patienceMs;
        this.patienceMs = patienceMs;
        this.state = CustomerState.WAITING;
    }

    /**
     * Returns this customer's id.
     *
     * @return the id
     */
    public int getId()
    {
        return id;
    }

    /**
     * Returns the order this customer wants.
     *
     * @return the order
     */
    public Order getOrder()
    {
        return order;
    }

    /**
     * Returns the patience this customer has left.
     *
     * @return remaining patience in milliseconds, never negative
     */
    public int getPatienceMs()
    {
        return patienceMs;
    }

    /**
     * Returns the patience this customer started with, for drawing the
     * timer bar.
     *
     * @return starting patience in milliseconds
     */
    public int getMaxPatienceMs()
    {
        return maxPatienceMs;
    }

    /**
     * Returns this customer's current state.
     *
     * @return the state
     */
    public CustomerState getState()
    {
        return state;
    }

    /**
     * Checks whether this customer is still in the line, waiting or
     * impatient.
     *
     * @return true if the customer is WAITING or IMPATIENT
     */
    public boolean isActive()
    {
        return state == CustomerState.WAITING
            || state == CustomerState.IMPATIENT;
    }

    /**
     * Counts down this customer's patience. Patience stops at zero, and a
     * WAITING customer who reaches zero becomes IMPATIENT. Does nothing for
     * a customer who is no longer WAITING.
     *
     * @param elapsedMs milliseconds that have passed; must not be negative
     * @throws IllegalArgumentException if elapsedMs is negative
     */
    void decreasePatience(int elapsedMs)
    {
        if (elapsedMs < 0)
        {
            throw new IllegalArgumentException(
                "elapsedMs must not be negative: " + elapsedMs);
        }
        if (state != CustomerState.WAITING)
        {
            return;
        }
        patienceMs = Math.max(0, patienceMs - elapsedMs);
        if (patienceMs == 0)
        {
            state = CustomerState.IMPATIENT;
        }
    }

    /**
     * Moves this customer to a new state.
     *
     * @param newState the next state; must be a legal forward move
     * @throws IllegalArgumentException if newState is null or the move is
     *                                  not allowed from the current state
     */
    void setState(CustomerState newState)
    {
        if (newState == null)
        {
            throw new IllegalArgumentException("newState must not be null");
        }
        if (!canMoveTo(newState))
        {
            throw new IllegalArgumentException(
                "Customer " + id + " cannot go from " + state + " to "
                    + newState);
        }
        state = newState;
    }

    /**
     * Checks whether the lifecycle allows moving from the current state to
     * the given one.
     *
     * @param next the proposed state
     * @return true if the move is allowed
     */
    private boolean canMoveTo(CustomerState next)
    {
        switch (state)
        {
            case WAITING:
                return next == CustomerState.IMPATIENT
                    || next == CustomerState.SERVED
                    || next == CustomerState.FAILED;
            case IMPATIENT:
                return next == CustomerState.SERVED
                    || next == CustomerState.REMOVED
                    || next == CustomerState.FAILED;
            default:
                return false;
        }
    }

    /**
     * Returns a short description such as "Customer #3 (WAITING, 12400 ms)".
     *
     * @return this customer as a string
     */
    @Override
    public String toString()
    {
        return "Customer #" + id + " (" + state + ", " + patienceMs + " ms)";
    }
}
