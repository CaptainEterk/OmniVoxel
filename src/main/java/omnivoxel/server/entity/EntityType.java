package omnivoxel.server.entity;

public record EntityType(Type type, String uuid) {
    public EntityType(Type type) {
        this(type, "");
    }

    public enum Type {
        PIG, PLAYER
    }
}