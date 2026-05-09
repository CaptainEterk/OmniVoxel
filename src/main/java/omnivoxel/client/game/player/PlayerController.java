package omnivoxel.client.game.player;

import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.client.game.input.KeyInput;
import omnivoxel.client.game.input.MouseButtonInput;
import omnivoxel.client.game.input.MouseInput;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.Client;
import omnivoxel.client.network.request.BlockReplaceRequest;
import omnivoxel.client.network.request.PlayerUpdateRequest;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.block.hitbox.BlockHitbox;
import omnivoxel.common.settings.ConstantClientSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class PlayerController {
    private static final double GRAVITY = 0.8f;
    private static final double JUMP_VELOCITY = 12f * ConstantClientSettings.TARGET_TPS;
    private static final double AIR_RESISTANCE = 0.91f * ConstantClientSettings.TARGET_TPS;
    private static final double GROUND_FRICTION = 0.546f * ConstantClientSettings.TARGET_TPS;
    private static final byte COLLISION_X = 0b001;
    private static final byte COLLISION_Y = 2;
    private static final byte COLLISION_Z = 4;
    private static final byte COLLISION_DONE = 0b111;
    private static final String[] blocks = new String[]{
            "core:red_light_block/default",
            "core:green_light_block/default",
            "core:blue_light_block/default"
    };
    private final Client client;
    private final Camera camera;
    private final Settings settings;
    private final BlockingQueue<Consumer<Window>> contextTasks;
    private final State state;
    private final ClientWorld world;
    private final BlockService<BlockWithMesh> blockService;
    private final Hitbox hitbox;
    private final Window window;
    private double speed;
    @NotNull
    private MovementMode movementMode = MovementMode.FALL_COLLIDE;

    private double x;
    private double y = 128;
    private double z;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double yaw;
    private double pitch;
    private KeyInput keyInput;
    private MouseButtonInput mouseButtonInput;
    private MouseInput mouseInput;
    private boolean togglingWireframe;
    private boolean togglingFullscreen;
    private boolean togglingDebug;
    private boolean togglingMovementMode;
    private boolean leftMouseDown;
    private boolean rightMouseDown;
    private Position3D cachedChunkPos = new Position3D(0, 0, 0);
    private Chunk<BlockWithMesh> cachedChunk;
    private boolean onGround = false;
    private int selectedBlock = 0;
    private boolean selectingBlockDown;

    public PlayerController(Client client, Camera camera, Settings settings, BlockingQueue<Consumer<Window>> contextTasks, State state, ClientWorld world, BlockService<BlockWithMesh> blockService, Window window) {
        this.client = client;
        this.camera = camera;
        this.blockService = blockService;
        camera.setPosition(x, y, z);
        this.settings = settings;
        this.contextTasks = contextTasks;
        this.state = state;
        this.world = world;
        this.window = window;
        hitbox = new Hitbox(-0.4f, -1.5f, -0.4f, 0.4f, 0.3f, 0.4f);
    }

    private static double intBound(double s, double ds) {
        if (ds > 0) {
            return (Math.floor(s + 1) - s) / ds;
        } else {
            return (s - Math.floor(s)) / -ds;
        }
    }

    private void updateBlockMovementModifiers(double wx, double wy, double wz) {
        speed = 1;

        double minX = wx + hitbox.minX();
        double maxX = wx + hitbox.maxX();
        double minY = wy + hitbox.minY();
        double maxY = wy + hitbox.maxY();
        double minZ = wz + hitbox.minZ();
        double maxZ = wz + hitbox.maxZ();

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.ceil(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.ceil(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.ceil(maxZ);

        for (int bx = blockMinX; bx <= blockMaxX; bx++) {
            for (int by = blockMinY; by <= blockMaxY; by++) {
                for (int bz = blockMinZ; bz <= blockMaxZ; bz++) {
                    int chunkX = IndexCalculator.chunkX(bx);
                    int chunkY = IndexCalculator.chunkY(by);
                    int chunkZ = IndexCalculator.chunkZ(bz);

                    int localX = IndexCalculator.localX(bx);
                    int localY = IndexCalculator.localY(by);
                    int localZ = IndexCalculator.localZ(bz);

                    if (cachedChunkPos == null || cachedChunk == null ||
                            cachedChunkPos.x() != chunkX ||
                            cachedChunkPos.y() != chunkY ||
                            cachedChunkPos.z() != chunkZ) {
                        cachedChunkPos = new Position3D(chunkX, chunkY, chunkZ);
                        ClientWorldChunk clientWorldChunk = world.get(cachedChunkPos, false, false);
                        if (clientWorldChunk == null) return;

                        cachedChunk = clientWorldChunk.getChunkData();
                        if (cachedChunk == null) return;
                    }

                    Block block = cachedChunk.getBlock(localX, localY, localZ);
                    if (block != null) {
                        BlockMesh blockMesh = blockService.getBlock(block.id()).blockMesh();
                        BlockHitbox[] blockHitbox = blockMesh.getHitbox();
                        for (BlockHitbox bh : blockHitbox) {
                            if (bh.isColliding(hitbox, (float) wx - bx, (float) wy - by, (float) wz - bz)) {
                                if (bh.volumeProperties().isVolume()) {
                                    if (speed > bh.volumeProperties().speed()) {
                                        speed = bh.volumeProperties().speed();
                                    }
                                    if (!onGround && bh.volumeProperties().isGround()) {
                                        onGround = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSolidAt(double wx, double wy, double wz) {
        double minX = wx + hitbox.minX();
        double maxX = wx + hitbox.maxX();
        double minY = wy + hitbox.minY();
        double maxY = wy + hitbox.maxY();
        double minZ = wz + hitbox.minZ();
        double maxZ = wz + hitbox.maxZ();

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.ceil(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.ceil(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.ceil(maxZ);

        for (int bx = blockMinX; bx <= blockMaxX; bx++) {
            for (int by = blockMinY; by <= blockMaxY; by++) {
                for (int bz = blockMinZ; bz <= blockMaxZ; bz++) {
                    int chunkX = IndexCalculator.chunkX(bx);
                    int chunkY = IndexCalculator.chunkY(by);
                    int chunkZ = IndexCalculator.chunkZ(bz);

                    int localX = IndexCalculator.localX(bx);
                    int localY = IndexCalculator.localY(by);
                    int localZ = IndexCalculator.localZ(bz);

                    if (cachedChunkPos == null || cachedChunk == null ||
                            cachedChunkPos.x() != chunkX ||
                            cachedChunkPos.y() != chunkY ||
                            cachedChunkPos.z() != chunkZ) {
                        cachedChunkPos = new Position3D(chunkX, chunkY, chunkZ);
                        ClientWorldChunk clientWorldChunk = world.get(cachedChunkPos, false, false);
                        if (clientWorldChunk == null) return true;

                        cachedChunk = clientWorldChunk.getChunkData();
                        if (cachedChunk == null) return true;
                    }

                    Block block = cachedChunk.getBlock(localX, localY, localZ);
                    if (block != null) {
                        BlockMesh blockMesh = blockService.getBlock(block.id()).blockMesh();
                        BlockHitbox[] blockHitbox = blockMesh.getHitbox();
                        for (BlockHitbox bh : blockHitbox) {
                            if (bh.isColliding(hitbox, (float) wx - bx, (float) wy - by, (float) wz - bz)) {
                                if (!bh.volumeProperties().isVolume()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public void tick(double deltaTime) {
        double tickDelta = deltaTime * ConstantClientSettings.TARGET_TPS;

        if (movementMode == MovementMode.FALL_COLLIDE) {
            updateBlockMovementModifiers(x, y, z);
        }

        state.setItem("deltaTime", deltaTime);

        BooleanRef changeRot = new BooleanRef(false);
        if (mouseButtonInput.isMouseLocked()) {
            handleInput(deltaTime, changeRot, movementMode != MovementMode.FALL_COLLIDE);

            if (mouseButtonInput.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                if (!leftMouseDown) {
                    leftMouseDown = true;
                    Position3D observedBlock = findObservedBlock(false);
                    if (observedBlock != null) {
                        client.sendRequest(new BlockReplaceRequest(observedBlock, blockService.getBlock("omnivoxel:air/default")));
                    }
                }
            } else {
                leftMouseDown = false;
            }

            if (mouseButtonInput.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_MIDDLE)) {
                if (!selectingBlockDown) {
                    selectedBlock++;
                    selectedBlock %= blocks.length;
                    state.setItem("selected_block", blocks[selectedBlock]);
                }
                selectingBlockDown = true;
            } else {
                selectingBlockDown = false;
            }

            if (mouseButtonInput.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                if (!rightMouseDown) {
                    rightMouseDown = true;
                    Position3D observedBlock = findObservedBlock(true);
                    if (observedBlock != null) {
                        int chunkX = IndexCalculator.chunkX(observedBlock.x());
                        int chunkY = IndexCalculator.chunkY(observedBlock.y());
                        int chunkZ = IndexCalculator.chunkZ(observedBlock.z());

                        int localX = IndexCalculator.localX(observedBlock.x());
                        int localY = IndexCalculator.localY(observedBlock.y());
                        int localZ = IndexCalculator.localZ(observedBlock.z());

                        Block block = world.get(new Position3D(chunkX, chunkY, chunkZ), false, false).getChunkData().getBlock(localX, localY, localZ);
                        BlockMesh blockMesh = blockService.getBlock(block.id()).blockMesh();
                        BlockHitbox[] blockHitbox = blockMesh.getHitbox();

                        float lx = (float) (x - Math.floor(x));
                        float ly = (float) (y - Math.floor(y));
                        float lz = (float) (z - Math.floor(z));

                        boolean isColliding = false;
                        for (BlockHitbox bh : blockHitbox) {

                            if (bh.isColliding(hitbox, lx, ly, lz)) {
                                isColliding = true;
                                break;
                            }
                        }

                        if (isColliding) {
                            Logger.warn(Logger.Priority.NORMAL, "Cannot place block inside player!");
                        } else {
                            client.sendRequest(new BlockReplaceRequest(observedBlock, blockService.getBlock(blocks[selectedBlock])));
                        }
                    }
                }
            } else {
                rightMouseDown = false;
            }
            if (keyInput.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                contextTasks.add(mouseButtonInput::unlockMouse);
            }
        } else if (mouseButtonInput.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
            contextTasks.add(mouseButtonInput::lockMouse);
        }
        mouseInput.clearDelta();

        if (movementMode == MovementMode.FALL_COLLIDE) {
            velocityY -= (float) (GRAVITY * tickDelta);
        }

        double frictionFactor;
        if (movementMode == MovementMode.FALL_COLLIDE) {
            frictionFactor = (float) (((onGround ? GROUND_FRICTION : AIR_RESISTANCE) * speed) * deltaTime);
            velocityX *= frictionFactor;
            velocityZ *= frictionFactor;
            velocityY *= 0.98f*speed;
        } else {
            frictionFactor = (float) (AIR_RESISTANCE * deltaTime);
            velocityX *= frictionFactor;
            velocityY *= frictionFactor;
            velocityZ *= frictionFactor;
        }
        state.setItem("friction_factor", frictionFactor);

        boolean shouldUpdate = velocityX != 0 || velocityY != 0 || velocityZ != 0 || changeRot.get();

        handleMovement(deltaTime, movementMode != MovementMode.FLY);

        state.setItem("velocity_x", velocityX);
        state.setItem("velocity_y", velocityY);
        state.setItem("velocity_z", velocityZ);
        state.setItem("on_ground", onGround);
        state.setItem("movement_mode", movementMode.toString());

        if (shouldUpdate) {
            state.setItem("shouldUpdateView", true);
            state.setItem("shouldUpdateVisibleMeshes", true);

            client.sendRequest(new PlayerUpdateRequest(x, y, z, pitch, yaw));

            camera.setPosition(x, y, z);
        }
    }

    private Position3D findObservedBlock(boolean getBlockOn) {
        int maxDistance = 6;
        double originX = this.x;
        double originY = this.y;
        double originZ = this.z;

        double cosPitch = Math.cos(pitch);
        double dirX = Math.sin(yaw) * cosPitch;
        double dirY = -Math.sin(pitch);
        double dirZ = -Math.cos(yaw) * cosPitch;

        int x = (int) Math.floor(originX);
        int y = (int) Math.floor(originY);
        int z = (int) Math.floor(originZ);

        int stepX = dirX > 0 ? 1 : -1;
        int stepY = dirY > 0 ? 1 : -1;
        int stepZ = dirZ > 0 ? 1 : -1;

        double tMaxX = intBound(originX, dirX);
        double tMaxY = intBound(originY, dirY);
        double tMaxZ = intBound(originZ, dirZ);

        double tDeltaX = stepX / dirX;
        double tDeltaY = stepY / dirY;
        double tDeltaZ = stepZ / dirZ;

        double dist = 0;

        Position3D lastAir = null;

        while (dist <= maxDistance) {
            int chunkX = IndexCalculator.chunkX(x);
            int chunkY = IndexCalculator.chunkY(y);
            int chunkZ = IndexCalculator.chunkZ(z);

            int localX = IndexCalculator.localX(x);
            int localY = IndexCalculator.localY(y);
            int localZ = IndexCalculator.localZ(z);

            ClientWorldChunk clientWorldChunk = world.get(new Position3D(chunkX, chunkY, chunkZ), false, false);
            if (clientWorldChunk == null) return null;

            Block block = clientWorldChunk.getChunkData().getBlock(localX, localY, localZ);

            if (block == null || "omnivoxel:air/default".equals(block.id())) {
                lastAir = new Position3D(x, y, z);
            } else {
                BlockMesh blockMesh = blockService.getBlock(block.id()).blockMesh();
                BlockHitbox[] blockHitbox = blockMesh.getHitbox();
                boolean intersects = false;
                for (BlockHitbox bh : blockHitbox) {
                    if (bh.intersectsRay(originX, originY, originZ, dirX, dirY, dirZ, x, y, z)) {
                        intersects = true;
                        break;
                    }
                }

                if (intersects) {
                    if (getBlockOn) {
                        return lastAir;
                    }
                    return new Position3D(x, y, z);
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    private void handleMovement(double deltaTime, boolean collide) {
        if (!collide) {
            x += (float) (velocityX * deltaTime);
            y += (float) (velocityY * deltaTime);
            z += (float) (velocityZ * deltaTime);
            return;
        }

        int collisionSteps = settings.getIntSetting("collision_steps", 5);
        int collisionCount = settings.getIntSetting("collision_refining_count", 2);

        double stepDeltaTime = deltaTime / collisionSteps;
        byte collisionDone = 0;

        for (int i = 0; i < collisionSteps; i++) {
            if ((collisionDone & COLLISION_Y) == 0) {
                double targetY = y + velocityY * stepDeltaTime;
                if (isSolidAt(x, targetY, z)) {
                    double low = y;
                    double high = targetY;
                    for (int iter = 0; iter < collisionCount; iter++) {
                        double mid = (low + high) * 0.5;
                        if (isSolidAt(x, mid, z)) {
                            high = mid;
                        } else {
                            low = mid;
                        }
                    }
                    y = low;
                    if (velocityY < 0) {
                        onGround = true;
                    }
                    velocityY = 0;
                    collisionDone |= COLLISION_Y;
                } else {
                    y = targetY;
                    onGround = false;
                    collisionDone &= ~COLLISION_Y;
                }
            }

            if ((collisionDone & COLLISION_X) == 0) {
                double targetX = x + velocityX * stepDeltaTime;
                if (isSolidAt(targetX, y, z)) {
                    double low = x;
                    double high = targetX;
                    for (int iter = 0; iter < collisionCount; iter++) {
                        double mid = (low + high) * 0.5;
                        if (isSolidAt(mid, y, z)) {
                            high = mid;
                        } else {
                            low = mid;
                        }
                    }
                    x = low;
                    velocityX = 0;
                    collisionDone |= COLLISION_X;
                } else {
                    x = targetX;
                    collisionDone &= ~COLLISION_X;
                }
            }

            if ((collisionDone & COLLISION_Z) == 0) {
                double targetZ = z + velocityZ * stepDeltaTime;
                if (isSolidAt(x, y, targetZ)) {
                    double low = z;
                    double high = targetZ;
                    for (int iter = 0; iter < collisionCount; iter++) {
                        double mid = (low + high) * 0.5;
                        if (isSolidAt(x, y, mid)) {
                            high = mid;
                        } else {
                            low = mid;
                        }
                    }
                    z = low;
                    velocityZ = 0;
                    collisionDone |= COLLISION_Z;
                } else {
                    z = targetZ;
                    collisionDone &= ~COLLISION_Z;
                }
            }

            if (collisionDone == COLLISION_DONE) {
                break;
            }
        }
    }

    private void handleInput(double deltaTime, BooleanRef changeRot, boolean fly) {
        double deltaX = mouseInput.getDeltaX();
        double deltaY = mouseInput.getDeltaY();

        float pitchChange = (float) (deltaY / settings.getFloatSetting("sensitivity", 2f) * deltaTime);
        float yawChange = (float) (deltaX / settings.getFloatSetting("sensitivity", 2f) * deltaTime);

        if (pitchChange != 0 || yawChange != 0) {
            changeRot.set(true);
            camera.rotateX(pitchChange);
            camera.rotateY(yawChange);
            this.pitch = camera.getPitch();
            this.yaw = camera.getYaw();
        }

        double speed = 4.317f * ConstantClientSettings.TARGET_TPS;
        if (!fly) {
            if (onGround && keyInput.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
                velocityY = (float) (JUMP_VELOCITY * deltaTime);
                onGround = false;
            }
        } else {
            velocityY += (float) (((keyInput.isKeyPressed(GLFW.GLFW_KEY_SPACE) ? 1 : 0) - (keyInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) ? 1 : 0)) * (speed * deltaTime));
        }
        double moveRelativeX = (keyInput.isKeyPressed(GLFW.GLFW_KEY_D) ? 1 : 0) - (keyInput.isKeyPressed(GLFW.GLFW_KEY_A) ? 1 : 0);
        double moveRelativeZ = (keyInput.isKeyPressed(GLFW.GLFW_KEY_W) ? 1 : 0) - (keyInput.isKeyPressed(GLFW.GLFW_KEY_S) ? 1 : 0);
        if (moveRelativeX != 0 || moveRelativeZ != 0) {
            double length = Math.sqrt(moveRelativeX * moveRelativeX + moveRelativeZ * moveRelativeZ);
            if (length != 0) {
                moveRelativeX /= length;
                moveRelativeZ /= length;
            }

            double moveSpeed = speed * deltaTime * (onGround ? 1 : 0.3);
            moveRelativeX *= moveSpeed;
            moveRelativeZ *= moveSpeed;

            double sinYaw = org.joml.Math.sin(-yaw);
            double cosYaw = org.joml.Math.cos(-yaw);

            double moveX = -moveRelativeZ * sinYaw + moveRelativeX * cosYaw;
            double moveZ = -moveRelativeZ * cosYaw - moveRelativeX * sinYaw;

            velocityX += moveX;
            velocityZ += moveZ;
        }

        // Handle actions
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_F1)) {
            if (!togglingWireframe) {
                state.setItem("shouldRenderWireframe", !state.getItem("shouldRenderWireframe", Boolean.class));
            }
            togglingWireframe = true;
        } else {
            togglingWireframe = false;
        }
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_KP_ADD)) {
            camera.setFOV(camera.getFOV() - 1);
            state.setItem("shouldUpdateView", true);
        }
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_KP_SUBTRACT)) {
            camera.setFOV(camera.getFOV() + 1);
            state.setItem("shouldUpdateView", true);
        }
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_F3)) {
            if (!togglingDebug) {
                state.setItem("seeDebug", !state.getItem("seeDebug", Boolean.class));
            }
            togglingDebug = true;
        } else {
            togglingDebug = false;
        }
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_F4)) {
            if (!togglingMovementMode) {
                movementMode = MovementMode.values()[(movementMode.ordinal() + 1) % MovementMode.values().length];
            }
            togglingMovementMode = true;
        } else {
            togglingMovementMode = false;
        }
        if (keyInput.isKeyPressed(GLFW.GLFW_KEY_F11)) {
            if (!togglingFullscreen) {
                window.toggleFullscreen();

                mouseInput.clearDelta();
            }
            togglingFullscreen = true;
        } else {
            togglingFullscreen = false;
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public void setKeyInput(KeyInput keyInput) {
        this.keyInput = keyInput;
    }

    public void setMouseButtonInput(MouseButtonInput mouseButtonInput) {
        this.mouseButtonInput = mouseButtonInput;
    }

    public void setMouseInput(MouseInput mouseInput) {
        this.mouseInput = mouseInput;
    }

    private enum MovementMode {
        FALL_COLLIDE,
        FLY_COLLIDE,
        FLY
    }

    private static class BooleanRef {
        private boolean b;

        public BooleanRef(boolean b) {
            this.b = b;
        }

        public boolean get() {
            return b;
        }

        public void set(boolean b) {
            this.b = b;
        }
    }
}
