package omnivoxel.server;

public enum PackageID {
    // TCP

    // CLIENT -> SERVER

    REGISTER_CLIENT,
    CHUNK_REQUEST,
    CLOSE,

    // SERVER -> CLIENT

    CHUNK,
    NEW_ENTITY,
    SERVER_INFO,
    REGISTER_BLOCK,
    REGISTER_BLOCK_SHAPE,
    REMOVE_BLOCK_SUCCESS,
    REMOVE_BLOCK_FAILURE,

    // TODO: Implement UDP Client/Server (using TCP for now)
    // UDP

    // Client -> Server
    PLAYER_UPDATE,

    // Server -> Client
    ENTITY_UPDATE,
}