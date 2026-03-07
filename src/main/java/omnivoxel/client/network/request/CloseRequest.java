package omnivoxel.client.network.request;

public record CloseRequest() implements Request {
    @Override
    public RequestType getType() {
        return RequestType.CLOSE;
    }
}