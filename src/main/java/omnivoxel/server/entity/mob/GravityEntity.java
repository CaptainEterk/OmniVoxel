package omnivoxel.server.entity.mob;

import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.server.entity.Entity;

public abstract class GravityEntity extends Entity {
    protected GravityEntity(Hitbox hitbox) {
        super(hitbox);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
    }
}