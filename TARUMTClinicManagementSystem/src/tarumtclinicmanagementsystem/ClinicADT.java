/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */
public interface ClinicADT<T> {

    // --- List-like operations ---
    void add(T item);                  // Add to end
    void add(int index, T item);       // Add at position
    T get(int index);                  // Get item at position
    T set(int index, T item);          // Replace item at position
    T remove(int index);              // Remove item by index
    boolean remove(T item);            // Remove item by value
    int indexOf(T item);               // Find index of item
    boolean contains(T item);          // Check if item exists

    // --- Queue-like operations ---
    void enqueue(T item);              // Add to rear (alias of add())
    T dequeue();                       // Remove and return front item (alias of remove(0))
    T peek();                          // Return front item without removing (alias of get(0))

    // --- Utility operations ---
    int size();                        // Number of elements
    boolean isEmpty();                 // Is collection empty
    void clear();                      // Clear all items
}
