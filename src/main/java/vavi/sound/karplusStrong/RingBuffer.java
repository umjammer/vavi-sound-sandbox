/*
 * https://github.com/dspence84/GuitarHero
 */

package vavi.sound.karplusStrong;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;

import static java.lang.System.getLogger;


/**
 * Creates a ring buffer of a set size.  Constructor inputs the capacity
 * of the ring buffer.  Enqueue will increase the size and queue up the item
 * until the buffer is full, in which case it will overflow subsequent items
 * onto the end of the queue.
 */
public class RingBuffer {

    private static final Logger logger = getLogger(RingBuffer.class.getName());

    /** a double array to hold the contents of the buffer */
    private final double[] buffer;
    /** the index of the first item in the 'first' position */
    private int first;
    /** the index of the item in the last position */
    private int last;
    /** the current size of the buffer */
    private int size;
    /** the capacity of the buffer.  Read only */
    private final int capacity;

    /** create an empty ring buffer, with given max capacity */
    public RingBuffer(int capacity) {
        size = 0;
        this.capacity = capacity;
        buffer = new double[this.capacity];
        first = 0;
        last = first;
    }

    /** return capacity */
    public int capacity() {
        return capacity;
    }

    /** return number of items currently in the buffer */
    public int size() {
        return size;
    }

    /** is the buffer empty (size equals zero)? */
    public boolean isEmpty() {
        if (size == 0)
            return true;

        return false;
    }

    /** is the buffer full? */
    public boolean isFull() {
        if (size == capacity)
            return true;

        return false;
    }

    /** add an item to the buffer */
    public void enqueue(double x) {
        // wrap the last item around to the 0 position and queue up
        if (last == capacity) {
            last = 0;
            enqueue(x);
            // if the buffer is full, special care needs to be taken to increment first and last
        } else if (isFull()) {
            // place the item where first is (last should be the same as first)
            if (last != first)
                throw new IllegalStateException("last does not equal first, should not have got here!");
            // set the buffer item in last
            buffer[last] = x;
            // never point first at an invalid position in the array.
            if (first + 1 == capacity)
                first = 0;
            else
                first++;
            last++;
            // simply add an item to the buffer
        } else {
            // enqueue the item!
            buffer[last] = x;
            last++;
            size++;
        }
    }

    /**
     * remove an item from the buffer and update its pointer positions
     * @throws NoSuchElementException buffer is empty
     */
    public double dequeue() {
        // don't remove from an empty buffer.. bad things happen.
        if (isEmpty()) {
            throw new NoSuchElementException("The ring buffer is empty!");
        }

        // store the value in the buffer at current position
        double item = buffer[first];
        // don't point first where there is no item
        if (first == capacity - 1)
            first = 0;
        else
            // update our position in the buffer
            first++;
        // decrease the size of the buffer and return the value
        size--;
        return item;
    }

    /**
     * peek the first position in the buffer without dequeueing
     * @throws NoSuchElementException buffer is empty
     */
    public double peek() {
        // can't peek an empty buffer, right
        if (isEmpty())
            throw new NoSuchElementException("The ring buffer is empty!");

        // simply return the first position of the buffer
        return buffer[first];
    }

    /** convert the buffer to a string for debugging */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();

        // simply loop through the buffer
        for (int i = 0; i < capacity; i++) {
            sb.append(String.format("%.2f  ", buffer[i]));
        }

        // return the string representation
        return sb.toString();
    }

    /** contains unit tests for the ring buffer */
    public static void main(String[] args) {
        RingBuffer rb = new RingBuffer(10);

        // test isFull()
        for (int i = 0; i < 30; i++) {
            double rnd = Math.random() * 10;
            rb.enqueue(rnd);

            logger.log(Level.DEBUG, rb.toString() + rb.size());
        }

        // test isEmpty
        while (!rb.isEmpty()) {
            double num = rb.dequeue();
            logger.log(Level.DEBUG, rb + "  " + num);
        }

        // dequeue an empty buffer
        // should throw an exception
//        double num = rb.dequeue();
//        logger.log(Level.DEBUG, rb + "  " + num);

        // peak an empty buffer should throw and exception
//        double num = rb.peek();
//        logger.log(Level.DEBUG, num);

        // test ring functionality

        for (int i = 0; i < 20; i++) {
            double rnd = Math.random() * 10;
            rb.enqueue(rnd);
            logger.log(Level.DEBUG, rb);
        }

        // test peek and size
        logger.log(Level.DEBUG, "ring buffer first item is: " + rb.peek());
        logger.log(Level.DEBUG, "ring buffer size is: " + rb.size());

        // unit test provided by assignment page
        int N = Integer.parseInt(args[0]);
        RingBuffer buffer = new RingBuffer(N);
        for (int i = 1; i <= N; i++) {
            buffer.enqueue(i);
        }
        double t = buffer.dequeue();
        buffer.enqueue(t);
        logger.log(Level.DEBUG, "Size after wrap-around is " + buffer.size());
        while (buffer.size() >= 2) {
            double x = buffer.dequeue();
            double y = buffer.dequeue();
            buffer.enqueue(x + y);
        }
        logger.log(Level.DEBUG, buffer.peek());
    }
}
