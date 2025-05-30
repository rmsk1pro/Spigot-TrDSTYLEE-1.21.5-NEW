package net.minecraft.core;

import javax.annotation.Nullable;

public interface Registry<T> extends Iterable<T> {

    int DEFAULT = -1;

    int getId(T t0);

    @Nullable
    T byId(int i);

    default T byIdOrThrow(int i) {
        T t0 = (T) this.byId(i);

        if (t0 == null) {
            throw new IllegalArgumentException("No value with id " + i);
        } else {
            return t0;
        }
    }

    default int getIdOrThrow(T t0) {
        int i = this.getId(t0);

        if (i == -1) {
            String s = String.valueOf(t0);

            throw new IllegalArgumentException("Can't find id for '" + s + "' in map " + String.valueOf(this));
        } else {
            return i;
        }
    }

    int size();
}
