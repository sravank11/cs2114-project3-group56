package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Owns all game state for Order Rush and enforces the rules. It holds the
 * drive-thru line, the player's tray, the score and the strikes, spawns
 * customers with random orders, ticks every customer's patience, and
 * handles serving, free serves and refunds.
 *
 * Game never starts its own timer. The controller calls tick(int) on a
 * clock, so the model stays single-threaded and can be driven directly
 * from tests or a console driver. After every change Game notifies its
 * GameListeners; it does not know what the view looks like.
 *
 * Scoring:
 *   exact order               +POINTS_CORRECT
 *   order with extra food     +POINTS_CORRECT - EXTRAS_PENALTY
 *   wrong/incomplete order    strike
 *   patience runs out         strike, then the player serves the customer
 *                             free (no points) or refunds them
 *                             (-REFUND_PENALTY)
 * The score never goes below 0. MAX_STRIKES strikes ends the game.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class Game
{
    /** Strikes that end the game. */
    public static final int MAX_STRIKES = 3;
    /** Points for serving an exact order. */
    public static final int POINTS_CORRECT = 10;
    /** Deducted from the reward when the tray had extra food. */
    public static final int EXTRAS_PENALTY = 5;
    /** Deducted when an impatient customer is refunded. */
    public static final int REFUND_PENALTY = 5;
    /** Shortest starting patience a customer can have. */
    public static final int MIN_PATIENCE_MS = 20000;
    /** Longest starting patience a customer can have. */
    public static final int MAX_PATIENCE_MS = 30000;
    /** Most food items one order can contain. */
    public static final int MAX_ORDER_ITEMS = 3;

    private final LinkedQueue<Customer> line;
    private final Map<MenuItem, Integer> tray;
    private final Random rng;
    private final List<GameListener> listeners;
    private final int spawnIntervalMs;
    private int score;
    private int strikes;
    private boolean gameOver;
    private int nextCustomerId;
    private int spawnTimerMs;

    /**
     * Creates a game with an empty line and tray, a score of 0 and no
     * strikes. The first customer arrives after one spawn interval.
     *
     * @param rng             source of random orders and patience; pass a
     *                        seeded Random for repeatable tests
     * @param spawnIntervalMs milliseconds between customer arrivals
     * @throws IllegalArgumentException if rng is null or spawnIntervalMs
     *                                  is not positive
     */
    public Game(Random rng, int spawnIntervalMs)
    {
        if (rng == null)
        {
            throw new IllegalArgumentException("rng must not be null");
        }
        if (spawnIntervalMs <= 0)
        {
            throw new IllegalArgumentException(
                "spawnIntervalMs must be positive: " + spawnIntervalMs);
        }
        this.rng = rng;
        this.spawnIntervalMs = spawnIntervalMs;
        this.line = new LinkedQueue<Customer>();
        this.tray = new EnumMap<MenuItem, Integer>(MenuItem.class);
        this.listeners = new ArrayList<GameListener>();
        this.score = 0;
        this.strikes = 0;
        this.gameOver = false;
        this.nextCustomerId = 1;
        this.spawnTimerMs = spawnIntervalMs;
    }

    /**
     * Registers a listener to be told whenever the game changes.
     *
     * @param listener the listener to add; must not be null
     * @throws IllegalArgumentException if listener is null
     */
    public void addListener(GameListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Advances the game clock. Every customer in the line loses patience,
     * not just the front one. Each customer who runs out becomes IMPATIENT
     * and adds a strike. New customers arrive whenever the spawn interval
     * passes. Does nothing once the game is over.
     *
     * @param elapsedMs milliseconds since the last tick; must not be
     *                  negative
     * @throws IllegalArgumentException if elapsedMs is negative
     */
    public void tick(int elapsedMs)
    {
        if (elapsedMs < 0)
        {
            throw new IllegalArgumentException(
                "elapsedMs must not be negative: " + elapsedMs);
        }
        if (gameOver)
        {
            return;
        }

        // Collect first, then add strikes, because the third strike ends
        // the game and empties the line we would still be iterating.
        List<Customer> ranOut = new ArrayList<Customer>();
        for (Customer customer : line)
        {
            boolean wasWaiting = customer.getState() == CustomerState.WAITING;
            customer.decreasePatience(elapsedMs);
            if (wasWaiting
                && customer.getState() == CustomerState.IMPATIENT)
            {
                ranOut.add(customer);
            }
        }
        for (int i = 0; i < ranOut.size() && !gameOver; i++)
        {
            addStrike();
        }

        spawnTimerMs -= elapsedMs;
        while (spawnTimerMs <= 0 && !gameOver)
        {
            addCustomer();
            spawnTimerMs += spawnIntervalMs;
        }

        String message = "";
        if (gameOver)
        {
            message = gameOverMessage();
        }
        else if (!ranOut.isEmpty())
        {
            message = describeRanOut(ranOut);
        }
        notifyListeners(message);
    }

    /**
     * Adds a new customer with a random order and random patience to the
     * back of the line. Does nothing once the game is over.
     */
    public void spawnCustomer()
    {
        if (gameOver)
        {
            return;
        }
        Customer customer = addCustomer();
        notifyListeners("Customer #" + customer.getId() + " arrived");
    }

    /**
     * Puts one food item on the player's tray.
     *
     * @param item the food to add; must not be null
     * @return true if the item was added, false if the game is over
     * @throws IllegalArgumentException if item is null
     */
    public boolean addFood(MenuItem item)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("item must not be null");
        }
        if (gameOver)
        {
            return false;
        }
        tray.merge(item, 1, Integer::sum);
        notifyListeners("Added " + item);
        return true;
    }

    /**
     * Empties the player's tray. Does nothing if the tray is already empty
     * or the game is over.
     */
    public void clearTray()
    {
        if (gameOver || tray.isEmpty())
        {
            return;
        }
        tray.clear();
        notifyListeners("Tray cleared");
    }

    /**
     * Serves the tray to a customer. Only the front customer can be served
     * normally, and only while they are still WAITING. The tray is checked
     * against their order:
     *   exact match  - SERVED_OK, points awarded
     *   extra food   - SERVED_WITH_EXTRAS, points minus a food-waste
     *                  penalty, no strike
     *   wrong/short  - FAILED_ORDER, one strike
     * The customer then leaves the line and the tray is emptied.
     *
     * @param customerId id of the customer to serve
     * @return the outcome; a REJECTED_ value means nothing changed, apart
     *         from REJECTED_NO_CUSTOMER also emptying the tray
     */
    public ServeResult serveOrder(int customerId)
    {
        if (gameOver)
        {
            return reject(ServeResult.REJECTED_GAME_OVER,
                "The game is over");
        }
        if (line.isEmpty())
        {
            tray.clear();
            return reject(ServeResult.REJECTED_NO_CUSTOMER,
                "No customers to serve");
        }
        Customer customer = findCustomer(customerId);
        if (customer == null)
        {
            return reject(ServeResult.REJECTED_ALREADY_HANDLED,
                "That customer has already left");
        }
        if (customer != line.peek())
        {
            return reject(ServeResult.REJECTED_NOT_FRONT,
                "Only the customer at the window can be served");
        }
        if (customer.getState() == CustomerState.IMPATIENT)
        {
            return reject(ServeResult.REJECTED_IMPATIENT,
                "Customer #" + customerId
                    + " is impatient: serve free or refund");
        }

        ServeResult result = customer.getOrder().compareToTray(getTray());
        tray.clear();
        line.dequeue();
        String message;
        if (result == ServeResult.SERVED_OK)
        {
            customer.setState(CustomerState.SERVED);
            addPoints(POINTS_CORRECT);
            message = "Order correct! +" + POINTS_CORRECT;
        }
        else if (result == ServeResult.SERVED_WITH_EXTRAS)
        {
            customer.setState(CustomerState.SERVED);
            addPoints(POINTS_CORRECT - EXTRAS_PENALTY);
            message = "Order served with extra food, -" + EXTRAS_PENALTY
                + " for waste";
        }
        else
        {
            customer.setState(CustomerState.FAILED);
            addStrike();
            message = "Wrong order, strike added";
        }
        if (gameOver)
        {
            message = gameOverMessage();
        }
        notifyListeners(message);
        return result;
    }

    /**
     * Serves an impatient customer for free. They leave the line and no
     * points are awarded. Unlike serveOrder, the customer may be anywhere
     * in the line.
     *
     * @param customerId id of the impatient customer
     * @return SERVED_OK on success, otherwise REJECTED_GAME_OVER,
     *         REJECTED_ALREADY_HANDLED (not in the line) or
     *         REJECTED_NOT_IMPATIENT (still waiting)
     */
    public ServeResult serveFree(int customerId)
    {
        if (gameOver)
        {
            return reject(ServeResult.REJECTED_GAME_OVER,
                "The game is over");
        }
        Customer customer = findCustomer(customerId);
        if (customer == null)
        {
            return reject(ServeResult.REJECTED_ALREADY_HANDLED,
                "That customer has already left");
        }
        if (customer.getState() != CustomerState.IMPATIENT)
        {
            return reject(ServeResult.REJECTED_NOT_IMPATIENT,
                "Customer #" + customerId + " is still waiting patiently");
        }
        customer.setState(CustomerState.SERVED);
        line.remove(customer);
        notifyListeners("Customer #" + customerId + " served for free");
        return ServeResult.SERVED_OK;
    }

    /**
     * Refunds an impatient customer and removes them from anywhere in the
     * line. The refund penalty is applied once.
     *
     * @param customerId id of the impatient customer
     * @return true if removed, false if the game is over or the customer is
     *         not in the line or not impatient
     */
    public boolean removeImpatientCustomer(int customerId)
    {
        if (gameOver)
        {
            return false;
        }
        Customer customer = findCustomer(customerId);
        if (customer == null
            || customer.getState() != CustomerState.IMPATIENT)
        {
            return false;
        }
        customer.setState(CustomerState.REMOVED);
        line.remove(customer);
        addPoints(-REFUND_PENALTY);
        notifyListeners("Customer #" + customerId + " refunded, -"
            + REFUND_PENALTY);
        return true;
    }

    /**
     * Returns the current score.
     *
     * @return the score, never negative
     */
    public int getScore()
    {
        return score;
    }

    /**
     * Returns the number of failed customers.
     *
     * @return strikes, from 0 to MAX_STRIKES
     */
    public int getStrikes()
    {
        return strikes;
    }

    /**
     * Checks whether the game has ended.
     *
     * @return true once MAX_STRIKES strikes have been reached
     */
    public boolean isGameOver()
    {
        return gameOver;
    }

    /**
     * Returns the customer at the serving window.
     *
     * @return the front customer, or null if the line is empty
     */
    public Customer getFrontCustomer()
    {
        return line.peek();
    }

    /**
     * Returns a read-only snapshot of the line, front first.
     *
     * @return the customers in arrival order; empty if no one is waiting
     */
    public List<Customer> getCustomers()
    {
        List<Customer> snapshot = new ArrayList<Customer>(line.size());
        for (Customer customer : line)
        {
            snapshot.add(customer);
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Returns a read-only copy of the tray.
     *
     * @return each food item on the tray mapped to its count; empty if the
     *         tray is empty
     */
    public Map<MenuItem, Integer> getTray()
    {
        Map<MenuItem, Integer> copy =
            new EnumMap<MenuItem, Integer>(MenuItem.class);
        copy.putAll(tray);
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Creates a customer with a random order and patience and puts them at
     * the back of the line, without notifying listeners.
     *
     * @return the new customer
     */
    private Customer addCustomer()
    {
        int patience = MIN_PATIENCE_MS
            + rng.nextInt(MAX_PATIENCE_MS - MIN_PATIENCE_MS + 1);
        Customer customer =
            new Customer(nextCustomerId, randomOrder(), patience);
        nextCustomerId++;
        line.enqueue(customer);
        return customer;
    }

    /**
     * Builds an order of 1 to MAX_ORDER_ITEMS random menu items. The same
     * item can be picked more than once.
     *
     * @return a new random order
     */
    private Order randomOrder()
    {
        MenuItem[] menu = MenuItem.values();
        int count = 1 + rng.nextInt(MAX_ORDER_ITEMS);
        Map<MenuItem, Integer> items =
            new EnumMap<MenuItem, Integer>(MenuItem.class);
        for (int i = 0; i < count; i++)
        {
            items.merge(menu[rng.nextInt(menu.length)], 1, Integer::sum);
        }
        return new Order(items);
    }

    /**
     * Finds a customer who is still in the line.
     *
     * @param customerId the id to look for
     * @return the customer, or null if no one in the line has that id
     */
    private Customer findCustomer(int customerId)
    {
        for (Customer customer : line)
        {
            if (customer.getId() == customerId)
            {
                return customer;
            }
        }
        return null;
    }

    /**
     * Changes the score by the given amount, never letting it go below 0.
     *
     * @param amount points to add; negative for a penalty
     */
    private void addPoints(int amount)
    {
        score = Math.max(0, score + amount);
    }

    /**
     * Adds one strike and ends the game when MAX_STRIKES is reached.
     */
    private void addStrike()
    {
        strikes++;
        if (strikes >= MAX_STRIKES)
        {
            endGame();
        }
    }

    /**
     * Ends the game. Every customer still in the line is marked FAILED and
     * drives off. Strikes stay at MAX_STRIKES.
     */
    private void endGame()
    {
        gameOver = true;
        while (!line.isEmpty())
        {
            line.dequeue().setState(CustomerState.FAILED);
        }
        tray.clear();
    }

    /**
     * Notifies listeners of a rejected action and returns the result.
     *
     * @param result  the rejection to return
     * @param message what to tell the player
     * @return result
     */
    private ServeResult reject(ServeResult result, String message)
    {
        notifyListeners(message);
        return result;
    }

    /**
     * Builds the message for customers who just ran out of patience.
     *
     * @param ranOut the customers who became impatient this tick
     * @return a message for the player
     */
    private String describeRanOut(List<Customer> ranOut)
    {
        if (ranOut.size() == 1)
        {
            return "Customer #" + ranOut.get(0).getId()
                + " ran out of patience, strike added";
        }
        return ranOut.size() + " customers ran out of patience, strikes added";
    }

    /**
     * Builds the game-over message.
     *
     * @return a message with the final score
     */
    private String gameOverMessage()
    {
        return "Game over! Final score: " + score;
    }

    /**
     * Tells every listener the game changed.
     *
     * @param message a short line for the player; may be empty
     */
    private void notifyListeners(String message)
    {
        for (GameListener listener : listeners)
        {
            listener.gameChanged(message);
        }
    }
}
