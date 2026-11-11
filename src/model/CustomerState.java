package model;

/**
 * Represents the possible lifecycle states of a customer.
 *
 * WAITING: Customer is waiting in line with patience remaining.
 * IMPATIENT: Customer has run out of patience.
 * SERVED: Customer has been served and has left the line.
 * REMOVED: Customer has been refunded and removed.
 * FAILED: Customer has left because the game ended or their order was incorrect.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public enum CustomerState
{
    WAITING,
    IMPATIENT,
    SERVED,
    REMOVED,
    FAILED
}