package omnivoxel.server.client.block;

public record PriorityServerBlock(ServerBlock serverBlock, Priority priority) {
    public enum Priority {
        DECORATION(0),
        WORLD_TERRAIN(1),
        DECORATION_CRITICAL(2),
        STRUCTURE_PRIMARY(3),
        STRUCTURE_CRITICAL(4),
        MANUAL_OVERRIDE(5),
        NO_STRUCTURE(6);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean canOverwrite(Priority other) {
            return this.value >= other.value;
        }
    }
}
