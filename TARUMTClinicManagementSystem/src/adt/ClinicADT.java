package adt;

public interface ClinicADT<T> {

    // --- Custom Comparator ---
    public static interface MyIterator<T> {
       boolean hasNext();
       T next();
    }

    public static interface MyComparator<T> {
        int compare(T o1, T o2);
    }


    // --- List-like operations ---
    void add(T item);
    void add(int index, T item);
    T get(int index);
    T set(int index, T item);
    T remove(int index);
    boolean remove(T item);
    int indexOf(T item);
    boolean contains(T item);

    // --- Queue-like operations ---
    void enqueue(T item);
    T dequeue();
    T peek();

    // --- Utility operations ---
    int size();
    boolean isEmpty();
    void clear();

    // --- Sorting ---
    void sort(MyComparator<T> comparator);

    // --- Iterator ---
    MyIterator<T> iterator();
}
