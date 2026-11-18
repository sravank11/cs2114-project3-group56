package model;

/**
 * Something that wants to hear when the game changes. The model defines
 * this interface and the view implements it, so Game can notify the view
 * without knowing the view exists.
 *
 * @author Sravan Surapaneni
 * @version November 2026
 */
public interface GameListener
{
    /**
     * Called after any change to the game, such as a tick, a new customer,
     * food added, or a serve.
     *
     * @param message a short line for the player; may be empty
     */
    void gameChanged(String message);
}
