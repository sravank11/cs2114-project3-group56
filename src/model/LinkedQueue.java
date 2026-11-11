package model;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A first-in, first-out queue built on a singly linked chain of nodes.
 * New items join at the tail and leave from the head, which is how the
 * drive-thru line works.
 *
 * Beyond the usual queue operations, remove(T) can take an item out of
 * the middle of the line (for impatient customers), and the queue is
 * Iterable so every customer's timer can be ticked in arrival order.
 *
 * Time complexity, where n is the number of items:
 *   enqueue   O(1)
 *   dequeue   O(1)
 *   peek      O(1)
 *   isEmpty   O(1)
 *   size      O(1)
 *   remove(T) O(n)
 *
 * @author Matthew Li
 * @version November 2026
 *
 * @param <T> the type of items held in the queue
 */
public class LinkedQueue<T> implements Iterable<T>
{
    private Node<T> head; // front of the line, or null when empty
    private Node<T> tail; // back of the line, or null when empty
    private int size;

    /**
     * Creates an empty queue.
     */
    public LinkedQueue()
    {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Adds an item to the back of the queue.
     *
     * @param item the item to add; must not be null
     * @throws IllegalArgumentException if item is null
     */
    public void enqueue(T item)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Cannot enqueue null");
        }
        Node<T> node = new Node<T>(item);
        if (isEmpty())
        {
            head = node;
        }
        else
        {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    /**
     * Removes and returns the item at the front of the queue.
     *
     * @return the front item, or null if the queue is empty
     */
    public T dequeue()
    {
        if (isEmpty())
        {
            return null;
        }
        T item = head.data;
        head = head.next;
        if (head == null)
        {
            tail = null;
        }
        size--;
        return item;
    }

    /**
     * Returns the item at the front of the queue without removing it.
     *
     * @return the front item, or null if the queue is empty
     */
    public T peek()
    {
        if (isEmpty())
        {
            return null;
        }
        return head.data;
    }

    /**
     * Checks whether the queue has no items.
     *
     * @return true if the queue is empty
     */
    public boolean isEmpty()
    {
        return size == 0;
    }

    /**
     * Removes the first item equal to the given one, wherever it is in the
     * queue. The remaining items keep their order.
     *
     * @param item the item to remove
     * @return true if an item was removed, false if item is null or not
     *         found
     */
    public boolean remove(T item)
    {
        if (item == null)
        {
            return false;
        }
        Node<T> previous = null;
        Node<T> current = head;
        while (current != null)
        {
            if (current.data.equals(item))
            {
                if (previous == null)
                {
                    head = current.next;
                }
                else
                {
                    previous.next = current.next;
                }
                if (current == tail)
                {
                    tail = previous;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /**
     * Returns the number of items in the queue.
     *
     * @return the size of the queue
     */
    public int size()
    {
        return size;
    }

    /**
     * Returns an iterator that visits items from front to back. The
     * iterator does not support remove(); use remove(T) on the queue.
     *
     * @return an iterator over the queue in arrival order
     */
    @Override
    public Iterator<T> iterator()
    {
        return new QueueIterator();
    }

    /**
     * Walks the chain from head to tail.
     */
    private class QueueIterator implements Iterator<T>
    {
        private Node<T> current = head;

        @Override
        public boolean hasNext()
        {
            return current != null;
        }

        @Override
        public T next()
        {
            if (current == null)
            {
                throw new NoSuchElementException();
            }
            T item = current.data;
            current = current.next;
            return item;
        }
    }

    /**
     * One link in the chain.
     *
     * @param <T> the type of item stored
     */
    private static class Node<T>
    {
        private final T data;
        private Node<T> next;

        /**
         * Creates a node with no successor.
         *
         * @param data the item stored in this node
         */
        Node(T data)
        {
            this.data = data;
            this.next = null;
        }
    }
}
