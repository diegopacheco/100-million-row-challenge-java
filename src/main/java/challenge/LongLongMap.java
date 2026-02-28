package challenge;

import java.util.Arrays;

public class LongLongMap {
    private static final long EMPTY = Long.MIN_VALUE;
    private long[] keys;
    private long[] values;
    private int size;
    private int mask;

    public LongLongMap() {
        int cap = 2048;
        keys = new long[cap];
        values = new long[cap];
        mask = cap - 1;
        Arrays.fill(keys, EMPTY);
    }

    public void addTo(long key, long delta) {
        int idx = ((int)(key ^ (key >>> 32))) & mask;
        while (true) {
            if (keys[idx] == EMPTY) {
                keys[idx] = key;
                values[idx] = delta;
                size++;
                if (size > (keys.length * 3) / 4) resize();
                return;
            }
            if (keys[idx] == key) {
                values[idx] += delta;
                return;
            }
            idx = (idx + 1) & mask;
        }
    }

    public void mergeFrom(LongLongMap other) {
        for (int i = 0; i < other.keys.length; i++) {
            if (other.keys[i] != EMPTY) {
                addTo(other.keys[i], other.values[i]);
            }
        }
    }

    public void forEach(LongLongConsumer consumer) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != EMPTY) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    private void resize() {
        long[] oldKeys = keys;
        long[] oldValues = values;
        int newCap = keys.length << 1;
        keys = new long[newCap];
        values = new long[newCap];
        mask = newCap - 1;
        Arrays.fill(keys, EMPTY);
        size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != EMPTY) {
                addTo(oldKeys[i], oldValues[i]);
            }
        }
    }

    @FunctionalInterface
    public interface LongLongConsumer {
        void accept(long key, long value);
    }
}
