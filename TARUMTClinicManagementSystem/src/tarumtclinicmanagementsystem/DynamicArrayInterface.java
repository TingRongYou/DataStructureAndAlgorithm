/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

/**
 *
 * @author User
 */
public interface DynamicArrayInterface<T> {
    void add(T item);
    void add(int index, T item);
    T get(int index);
    T set(int index, T item);
    T remove(int index);
    boolean remove(T item);
    int indexOf(T item);
    boolean contains(T item);
    int size();
    boolean isEmpty();
    void clear();
}
