package model;

/**
 * Represents the possible results of a player's attempt to serve a customer.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public enum ServeResult
{
    /** The customer received the exact correct order. */
    SERVED_OK,

    /** The customer received the correct food with extras. */
    SERVED_WITH_EXTRAS,

    /** The customer received an incorrect or incomplete order. */
    FAILED_ORDER,

    /** There are no customers in the queue. */
    REJECTED_NO_CUSTOMER,

    /** The selected customer is not at the front of the line. */
    REJECTED_NOT_FRONT,

    /** The customer has already been handled or has left. */
    REJECTED_ALREADY_HANDLED,

    /** The game has already ended. */
    REJECTED_GAME_OVER,

    /** The customer has run out of patience. */
    REJECTED_IMPATIENT,

    /** The customer has not run out of patience yet. */
    REJECTED_NOT_IMPATIENT
}