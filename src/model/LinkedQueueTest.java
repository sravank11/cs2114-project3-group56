package model;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tests the LinkedQueue class.
 *
 * @author Matthew Li
 * @version November 2026
 */
public class LinkedQueueTest
    extends student.TestCase
{
    private LinkedQueue<String> queue;

    /**
     * Creates an empty queue before each test.
     */
    public void setUp()
    {
        queue = new LinkedQueue<String>();
    }


    /**
     * Tests that a new queue is empty.
     */
    public void testNewQueueIsEmpty()
    {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
        assertNull(queue.dequeue());
    }


    /**
     * Tests enqueue keeps items in arrival order.
     */
    public void testEnqueue()
    {
        queue.enqueue("a");
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertEquals("a", queue.peek());

        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.peek());
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());
        assertEquals("c", queue.dequeue());
    }


    /**
     * Tests enqueue rejects null and leaves the queue unchanged.
     */
    public void testEnqueueNull()
    {
        queue.enqueue("a");
        try
        {
            queue.enqueue(null);
            fail("enqueue(null) should throw");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull(e.getMessage());
        }
        assertEquals(1, queue.size());
        assertEquals("a", queue.peek());
    }


    /**
     * Tests dequeue removes from the front and the second item becomes the
     * new front.
     */
    public void testDequeue()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        assertEquals("a", queue.dequeue());
        assertEquals(2, queue.size());
        assertEquals("b", queue.peek());
    }


    /**
     * Tests the queue works again after being emptied, so the tail was
     * reset.
     */
    public void testDequeueToEmptyThenReuse()
    {
        queue.enqueue("a");
        assertEquals("a", queue.dequeue());
        assertTrue(queue.isEmpty());
        assertNull(queue.peek());
        assertNull(queue.dequeue());

        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals("b", queue.peek());
        assertEquals(2, queue.size());
        assertEquals("b", queue.dequeue());
        assertEquals("c", queue.dequeue());
    }


    /**
     * Tests peek does not remove the front item.
     */
    public void testPeek()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        assertEquals("a", queue.peek());
        assertEquals("a", queue.peek());
        assertEquals(2, queue.size());
    }


    /**
     * Tests removing from the middle keeps the others in order.
     */
    public void testRemoveMiddle()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        assertTrue(queue.remove("b"));
        assertEquals(2, queue.size());
        assertEquals("a", queue.dequeue());
        assertEquals("c", queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests removing the front item.
     */
    public void testRemoveFront()
    {
        queue.enqueue("a");
        queue.enqueue("b");

        assertTrue(queue.remove("a"));
        assertEquals(1, queue.size());
        assertEquals("b", queue.peek());
    }


    /**
     * Tests removing the back item, then enqueueing so the tail is right.
     */
    public void testRemoveBack()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        assertTrue(queue.remove("c"));
        queue.enqueue("d");
        assertEquals(3, queue.size());
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());
        assertEquals("d", queue.dequeue());
    }


    /**
     * Tests removing the only item leaves a usable empty queue.
     */
    public void testRemoveOnlyItem()
    {
        queue.enqueue("a");
        assertTrue(queue.remove("a"));
        assertTrue(queue.isEmpty());
        assertNull(queue.peek());

        queue.enqueue("b");
        assertEquals("b", queue.peek());
        assertEquals(1, queue.size());
    }


    /**
     * Tests remove only takes out the first matching item.
     */
    public void testRemoveFirstMatchOnly()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("a");

        assertTrue(queue.remove("a"));
        assertEquals(2, queue.size());
        assertEquals("b", queue.dequeue());
        assertEquals("a", queue.dequeue());
    }


    /**
     * Tests remove returns false for null or a missing item and leaves the
     * queue unchanged.
     */
    public void testRemoveMissingOrNull()
    {
        assertFalse(queue.remove("a"));

        queue.enqueue("a");
        queue.enqueue("b");
        assertFalse(queue.remove("z"));
        assertFalse(queue.remove(null));
        assertEquals(2, queue.size());
        assertEquals("a", queue.dequeue());
        assertEquals("b", queue.dequeue());
    }


    /**
     * Tests size tracks enqueues, dequeues and removes.
     */
    public void testSize()
    {
        assertEquals(0, queue.size());
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");
        assertEquals(3, queue.size());
        queue.dequeue();
        assertEquals(2, queue.size());
        queue.remove("c");
        assertEquals(1, queue.size());
    }


    /**
     * Tests the iterator visits items in arrival order without removing
     * them.
     */
    public void testIterator()
    {
        queue.enqueue("a");
        queue.enqueue("b");
        queue.enqueue("c");

        StringBuilder visited = new StringBuilder();
        for (String item : queue)
        {
            visited.append(item);
        }
        assertEquals("abc", visited.toString());
        assertEquals(3, queue.size());
    }


    /**
     * Tests the iterator on an empty queue has no elements and next()
     * throws.
     */
    public void testIteratorEmpty()
    {
        Iterator<String> iter = queue.iterator();
        assertFalse(iter.hasNext());
        try
        {
            iter.next();
            fail("next() past the end should throw");
        }
        catch (NoSuchElementException e)
        {
            assertNotNull(e);
        }
    }
}
