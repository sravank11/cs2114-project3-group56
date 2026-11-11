package model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a food order for a customer.
 * Stores menu items and their requested quantities.
 * Compares the order against the player's tray.
 *
 * @author Matthew Lee
 * @version November 2026
 */
public class Order
{
    private final Map<MenuItem, Integer> items;

    /**
     * Creates an order containing the requested food items.
     *
     * @param items the requested food items and quantities
     * @throws IllegalArgumentException if the map is null or empty, an item is null, or a quantity is less than one.
     */
    public Order(Map<MenuItem, Integer> items)
    {
        if (items == null || items.isEmpty())
        {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Map<MenuItem, Integer> copy = new EnumMap<MenuItem, Integer>(MenuItem.class);

        for (Map.Entry<MenuItem, Integer> entry : items.entrySet())
        {
            MenuItem item = entry.getKey();
            Integer quantity = entry.getValue();

            if (item == null)
            {
                throw new IllegalArgumentException("Menu item must not be null");
            }

            if (quantity == null || quantity < 1)
            {
                throw new IllegalArgumentException("Quantity must be at least one");
            }

            copy.put(item, quantity);
        }

        this.items = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the requested food items and quantities.
     *
     * @return an unmodifiable map of the order
     */
    public Map<MenuItem, Integer> getItems()
    {
        return items;
    }

    /**
     * Compares the customer's order with the player's tray.
     *
     * An exact match returns SERVED_OK.
     * All required food with additional food returns
     * SERVED_WITH_EXTRAS.
     * Missing or incorrect food returns FAILED_ORDER.
     *
     * @param tray the player's prepared food
     * @return the result of comparing the order and tray
     * @throws IllegalArgumentException if tray is null
     */
    public ServeResult compareToTray(Map<MenuItem, Integer> tray)
    {
        if (tray == null)
        {
            throw new IllegalArgumentException("Tray must not be null");
        }

        if (tray.isEmpty())
        {
            return ServeResult.FAILED_ORDER;
        }

        boolean hasExtras = false;

        for (Map.Entry<MenuItem, Integer> entry : items.entrySet())
        {
            MenuItem item = entry.getKey();
            int required = entry.getValue();
            Integer available = tray.get(item);

            if (available == null || available < required)
            {
                return ServeResult.FAILED_ORDER;
            }

            if (available > required)
            {
                hasExtras = true;
            }
        }

        for (Map.Entry<MenuItem, Integer> entry : tray.entrySet())
        {
            MenuItem item = entry.getKey();
            Integer quantity = entry.getValue();

            if (item == null || quantity == null || quantity < 1)
            {
                return ServeResult.FAILED_ORDER;
            }

            if (!items.containsKey(item))
            {
                hasExtras = true;
            }
        }

        if (hasExtras)
        {
            return ServeResult.SERVED_WITH_EXTRAS;
        }

        return ServeResult.SERVED_OK;
    }
}