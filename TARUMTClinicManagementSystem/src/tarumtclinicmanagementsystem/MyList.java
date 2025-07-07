/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */

public class MyList<T> implements ListInterface<T> {

    private class Node {
        T data;
        Node next;
        public Node(T data) {
            this.data = data;
        }
    }

    private Node head;
    private int size;

    public MyList() {
        head = null;
        size = 0;
    }

    @Override
    public void add(T item) {
        Node newNode = new Node(item);
        if (head == null) {
            head = newNode;
        } else {
            Node current = head;
            while (current.next != null)
                current = current.next;
            current.next = newNode;
        }
        size++;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        Node current = head;
        for (int i = 0; i < index; i++)
            current = current.next;
        return current.data;
    }

    @Override
    public boolean remove(int index) {
        if (index < 0 || index >= size) return false;
        if (index == 0) {
            head = head.next;
        } else {
            Node current = head;
            for (int i = 0; i < index - 1; i++)
                current = current.next;
            current.next = current.next.next;
        }
        size--;
        return true;
    }

    @Override
    public boolean contains(T item) {
        Node current = head;
        while (current != null) {
            if (current.data.equals(item)) return true;
            current = current.next;
        }
        return false;
    }

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

