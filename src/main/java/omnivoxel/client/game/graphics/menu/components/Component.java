package omnivoxel.client.game.graphics.menu.components;

import omnivoxel.client.game.graphics.menu.position.ComponentPosition;

public interface Component {
    boolean isHidden();

    ComponentPosition position();
}