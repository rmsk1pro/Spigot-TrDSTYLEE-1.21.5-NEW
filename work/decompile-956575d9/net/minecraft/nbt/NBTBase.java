package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public sealed interface NBTBase permits NBTTagCompound, NBTList, PrimitiveTag, NBTTagEnd {

    int OBJECT_HEADER = 8;
    int ARRAY_HEADER = 12;
    int OBJECT_REFERENCE = 4;
    int STRING_SIZE = 28;
    byte TAG_END = 0;
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;
    int MAX_DEPTH = 512;

    void write(DataOutput dataoutput) throws IOException;

    String toString();

    byte getId();

    NBTTagType<?> getType();

    NBTBase copy();

    int sizeInBytes();

    void accept(TagVisitor tagvisitor);

    StreamTagVisitor.b accept(StreamTagVisitor streamtagvisitor);

    default void acceptAsRoot(StreamTagVisitor streamtagvisitor) {
        StreamTagVisitor.b streamtagvisitor_b = streamtagvisitor.visitRootEntry(this.getType());

        if (streamtagvisitor_b == StreamTagVisitor.b.CONTINUE) {
            this.accept(streamtagvisitor);
        }

    }

    default Optional<String> asString() {
        return Optional.empty();
    }

    default Optional<Number> asNumber() {
        return Optional.empty();
    }

    default Optional<Byte> asByte() {
        return this.asNumber().map(Number::byteValue);
    }

    default Optional<Short> asShort() {
        return this.asNumber().map(Number::shortValue);
    }

    default Optional<Integer> asInt() {
        return this.asNumber().map(Number::intValue);
    }

    default Optional<Long> asLong() {
        return this.asNumber().map(Number::longValue);
    }

    default Optional<Float> asFloat() {
        return this.asNumber().map(Number::floatValue);
    }

    default Optional<Double> asDouble() {
        return this.asNumber().map(Number::doubleValue);
    }

    default Optional<Boolean> asBoolean() {
        return this.asByte().map((obyte) -> {
            return obyte != 0;
        });
    }

    default Optional<byte[]> asByteArray() {
        return Optional.empty();
    }

    default Optional<int[]> asIntArray() {
        return Optional.empty();
    }

    default Optional<long[]> asLongArray() {
        return Optional.empty();
    }

    default Optional<NBTTagCompound> asCompound() {
        return Optional.empty();
    }

    default Optional<NBTTagList> asList() {
        return Optional.empty();
    }
}
