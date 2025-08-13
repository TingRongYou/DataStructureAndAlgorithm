package adt;

public class MyClinicADT<T> implements ClinicADT<T> {
    private Object[] data;
    private int size;
    private static final int INITIAL_CAPACITY = 10;

    public MyClinicADT() {
        data = new Object[INITIAL_CAPACITY];
        size = 0;
    }

    private void ensureCapacity() {
        if (size >= data.length) {
            Object[] newData = new Object[data.length * 2];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
    }

    // ---------------- List-like operations ----------------
    @Override
    public void add(T item) {
        ensureCapacity();
        data[size++] = item;
    }

    @Override
    public void add(int index, T item) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
        ensureCapacity();
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = item;
        size++;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        return (T) data[index];
    }

    @Override
    public T set(int index, T item) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        T old = (T) data[index];
        data[index] = item;
        return old;
    }

    @Override
    public T remove(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        T removed = (T) data[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) System.arraycopy(data, index + 1, data, index, numMoved);
        data[--size] = null;
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
        if (item == null) {
            for (int i = 0; i < size; i++) if (data[i] == null) return i;
        } else {
            for (int i = 0; i < size; i++) if (item.equals(data[i])) return i;
        }
        return -1;
    }

    @Override
    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    // ---------------- Queue-like operations ----------------
    @Override
    public void enqueue(T item) { add(item); }

    @Override
    public T dequeue() {
        if (isEmpty()) throw new RuntimeException("Queue is empty");
        return remove(0);
    }

    @Override
    public T peek() {
        if (isEmpty()) throw new RuntimeException("Queue is empty");
        return get(0);
    }

    // ---------------- Utility operations ----------------
    @Override
    public int size() { return size; }

    @Override
    public boolean isEmpty() { return size == 0; }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    // ---------------- Sorting ----------------
    @Override
    public void sort(MyComparator<T> comparator) {
        if (comparator == null) throw new IllegalArgumentException("Comparator cannot be null");
        for (int i = 0; i < size - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < size - i - 1; j++) {
                T a = (T) data[j];
                T b = (T) data[j + 1];
                if (comparator.compare(a, b) > 0) {
                    data[j] = b;
                    data[j + 1] = a;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    // ---------------- Iterator ----------------
    @Override
    public MyIterator<T> iterator() {
        return new MyIterator<T>() {
            private int currentIndex = 0;
            @Override
            public boolean hasNext() { return currentIndex < size; }
            @Override
            public T next() {
                if (!hasNext()) throw new RuntimeException("No more elements");
                return (T) data[currentIndex++];
            }
        };
    }

    // ---------------- toString ----------------
    @Override
    public String toString() {
        if (size == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(data[i]);
            if (i < size - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
