package omnivoxel.client.network.request;

public record PlayerUpdateRequest(double x, double y, double z, double pitch, double yaw) implements Request {
    @Override
    public RequestType getType() {
        return RequestType.PLAYER_UPDATE;
    }
}