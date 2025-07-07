/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */
public interface QueueInterface<T> {
    void enqueue(T item);     // Add item to the rear
    T dequeue();              // Remove and return front item
    T peek();                 // Return front item without removing
    boolean isEmpty();        // Check if the queue is empty
    int size();               // Get the number of items
    void clear();             // Remove all items from the queue
}
