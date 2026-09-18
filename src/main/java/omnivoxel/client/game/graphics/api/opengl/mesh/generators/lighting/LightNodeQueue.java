package omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;

import java.util.Arrays;

public final class LightNodeQueue {
    private static final int DEFAULT_CAPACITY = ConstantCommonSettings.BLOCKS_IN_CHUNK;

    private static final int COORD_MASK = 0x1F;

    private static final int Y_SHIFT = 5;
    private static final int Z_SHIFT = 10;
    private static final int LIGHT_SHIFT = 15;

    private int[] queue = new int[DEFAULT_CAPACITY];

    private int head;
    private int tail;

    private int x;
    private int y;
    private int z;
    private byte lightLevel;

    private int allSameLightCount;

    public void add(int x, int y, int z, byte lightLevel) {
        if (allSameLightCount > 0) {
            throw new IllegalStateException(
                    "Cannot add to queue when all the light levels are the same"
            );
        }

        ensureCapacity();

        queue[tail++] =
                (x & COORD_MASK)
                        | ((y & COORD_MASK) << Y_SHIFT)
                        | ((z & COORD_MASK) << Z_SHIFT)
                        | ((lightLevel & 0xFF) << LIGHT_SHIFT);
    }

    public void poll() {
        if (allSameLightCount > 0) {
            x = IndexCalculator.x(allSameLightCount);
            y = IndexCalculator.y(allSameLightCount);
            z = IndexCalculator.z(allSameLightCount);
            allSameLightCount--;
        } else {
            int node = queue[head++];

            x = node & COORD_MASK;
            y = (node >>> Y_SHIFT) & COORD_MASK;
            z = (node >>> Z_SHIFT) & COORD_MASK;
            lightLevel = (byte) (node >>> LIGHT_SHIFT);
        }
    }

    public boolean isEmpty() {
        return head == tail && allSameLightCount == 0;
    }

    public void clear() {
        head = 0;
        tail = 0;
        allSameLightCount = 0;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public byte lightLevel() {
        return lightLevel;
    }

    private void ensureCapacity() {
        if (tail < queue.length) {
            return;
        }

        if (head > 0) {
            int size = tail - head;

            System.arraycopy(queue, head, queue, 0, size);

            head = 0;
            tail = size;
            return;
        }

        int newCapacity = queue.length << 1;
        queue = Arrays.copyOf(queue, newCapacity);
    }
}