/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */

public class MyClinicADT<T> implements ClinicADT<T> {

    private class Node {
        T data;
        Node next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node head;
    private int size;

    public MyClinicADT() {
        head = null;
        size = 0;
    }

    // ---------------- List-like operations ----------------

    @Override
    public void add(T item) {
        add(size, item);
    }

    @Override
    public void add(int index, T item) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Invalid index");

        Node newNode = new Node(item);

        if (index == 0) {
            newNode.next = head;
            head = newNode;
        } else {
            Node prev = head;
            for (int i = 0; i < index - 1; i++)
                prev = prev.next;

            newNode.next = prev.next;
            prev.next = newNode;
        }
        size++;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Invalid index");

        Node current = head;
        for (int i = 0; i < index; i++)
            current = current.next;

        return current.data;
    }

    @Override
    public T set(int index, T item) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Invalid index");

        Node current = head;
        for (int i = 0; i < index; i++)
            current = current.next;

        T oldData = current.data;
        current.data = item;
        return oldData;
    }

    @Override
    public T remove(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Invalid index");

        Node removed;

        if (index == 0) {
            removed = head;
            head = head.next;
        } else {
            Node prev = head;
            for (int i = 0; i < index - 1; i++)
                prev = prev.next;

            removed = prev.next;
            prev.next = removed.next;
        }

        size--;
        return removed.data;
    }

    @Override
    public boolean remove(T item) {
        if (head == null) return false;

        if (head.data.equals(item)) {
            head = head.next;
            size--;
            return true;
        }

        Node current = head;
        while (current.next != null && !current.next.data.equals(item)) {
            current = current.next;
        }

        if (current.next != null) {
            current.next = current.next.next;
            size--;
            return true;
        }

        return false;
    }

    @Override
    public int indexOf(T item) {
        Node current = head;
        int index = 0;

        while (current != null) {
            if (current.data.equals(item))
                return index;
            current = current.next;
            index++;
        }

        return -1;
    }

    @Override
    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    // ---------------- Queue-like operations ----------------

    @Override
    public void enqueue(T item) {
        add(item);
    }

    @Override
    public T dequeue() {
        return remove(0);
    }

    @Override
    public T peek() {
        return get(0);
    }

    // ---------------- Utility operations ----------------

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        head = null;
        size = 0;
    }
}

