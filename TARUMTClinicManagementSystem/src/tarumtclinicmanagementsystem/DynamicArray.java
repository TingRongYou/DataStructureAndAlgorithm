package tarumtclinicmanagementsystem;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */

public class DynamicArray<T> implements DynamicArrayInterface<T> {
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] array;
    private int size;

    public DynamicArray() {
        this.array = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    private void grow() {
        int newCapacity = array.length * 2;
        Object[] newArray = new Object[newCapacity];
        for (int i = 0; i < size; i++) newArray[i] = array[i];
        array = newArray;
    }

    @Override
    public void add(T item) {
        if (size == array.length) grow();
        array[size++] = item;
    }

    @Override
    public void add(int index, T item) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
        if (size == array.length) grow();
        for (int i = size; i > index; i--) array[i] = array[i - 1];
        array[index] = item;
        size++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        return (T) array[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public T set(int index, T item) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        T old = (T) array[index];
        array[index] = item;
        return old;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T remove(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        T removed = (T) array[index];
        for (int i = index; i < size - 1; i++) array[i] = array[i + 1];
        array[--size] = null;
        return removed;
    }

    @Override
    public boolean remove(T item) {
        int index = indexOf(item);
        if (index != -1) {
            remove(index);
            return true;
        }
        return false;
    }

    @Override
    public int indexOf(T item) {
        for (int i = 0; i < size; i++) {
            if (array[i].equals(item)) return i;
        }
        return -1;
    }

    @Override
    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    @Override
    public int size() { return size; }

    @Override
    public boolean isEmpty() { return size == 0; }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) array[i] = null;
        size = 0;
    }
}

