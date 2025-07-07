/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */
public interface ListInterface<T> {
    void add(T item);
    T get(int index);
    boolean remove(int index);
    boolean contains(T item);
    int size();
    boolean isEmpty();
    void clear();
}
