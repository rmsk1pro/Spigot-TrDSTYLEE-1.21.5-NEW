package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

class LoadingChunkTracker extends ChunkMap {

    private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final ChunkMapDistance distanceManager;
    private final TicketStorage ticketStorage;

    public LoadingChunkTracker(ChunkMapDistance chunkmapdistance, TicketStorage ticketstorage) {
        super(LoadingChunkTracker.MAX_LEVEL + 1, 16, 256);
        this.distanceManager = chunkmapdistance;
        this.ticketStorage = ticketstorage;
        ticketstorage.setLoadingChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(long i) {
        return this.ticketStorage.getTicketLevelAt(i, false);
    }

    @Override
    protected int getLevel(long i) {
        if (!this.distanceManager.isChunkToRemove(i)) {
            PlayerChunk playerchunk = this.distanceManager.getChunk(i);

            if (playerchunk != null) {
                return playerchunk.getTicketLevel();
            }
        }

        return LoadingChunkTracker.MAX_LEVEL;
    }

    @Override
    protected void setLevel(long i, int j) {
        PlayerChunk playerchunk = this.distanceManager.getChunk(i);
        int k = playerchunk == null ? LoadingChunkTracker.MAX_LEVEL : playerchunk.getTicketLevel();

        if (k != j) {
            playerchunk = this.distanceManager.updateChunkScheduling(i, j, playerchunk, k);
            if (playerchunk != null) {
                this.distanceManager.chunksToUpdateFutures.add(playerchunk);
            }

        }
    }

    public int runDistanceUpdates(int i) {
        return this.runUpdates(i);
    }
}
