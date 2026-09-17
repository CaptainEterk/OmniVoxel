package omnivoxel.util;

import java.util.Arrays;

public class IntegerDynamicList {
    private int size = 0;
    private int[] values = new int[size];

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public void checkSize() {
        if (size + 1 > values.length) {
            values = Arrays.copyOf(values, values.length == 0 ? 1 : values.length << 1);
        }
    }

    public void add(int i) {
        checkSize();
        values[size++] = i;
    }

    public int get(int i) {
        return values[i];
    }
}