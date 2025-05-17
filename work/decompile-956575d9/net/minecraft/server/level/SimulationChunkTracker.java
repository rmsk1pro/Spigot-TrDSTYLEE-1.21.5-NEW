package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.TicketStorage;

public class SimulationChunkTracker extends ChunkMap {

    public static final int MAX_LEVEL = 33;
    protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
    private final TicketStorage ticketStorage;

    public SimulationChunkTracker(TicketStorage ticketstorage) {
        super(34, 16, 256);
        this.ticketStorage = ticketstorage;
        ticketstorage.setSimulationChunkUpdatedListener(this::update);
        this.chunks.defaultReturnValue((byte) 33);
    }

    @Override
    protected int getLevelFromSource(long i) {
        return this.ticketStorage.getTicketLevelAt(i, true);
    }

    public int getLevel(ChunkCoordIntPair chunkcoordintpair) {
        return this.getLevel(chunkcoordintpair.toLong());
    }

    @Override
    protected int getLevel(long i) {
        return this.chunks.get(i);
    }

    @Override
    protected void setLevel(long i, int j) {
        if (j >= 33) {
            this.chunks.remove(i);
        } else {
            this.chunks.put(i, (byte) j);
        }

    }

    public void runAllUpdates() {
        this.runUpdates(Integer.MAX_VALUE);
    }
}
