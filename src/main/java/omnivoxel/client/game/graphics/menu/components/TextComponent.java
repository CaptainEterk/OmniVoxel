package omnivoxel.client.game.graphics.menu.components;

import omnivoxel.client.game.graphics.api.opengl.text.Alignment;
import omnivoxel.client.game.graphics.menu.position.ComponentPosition;

public final class TextComponent implements Component {
    private final String text;
    private final ComponentPosition componentPosition;
    private final float scale;
    private final Alignment alignment;
    private boolean hidden = false;

    public TextComponent(String text, ComponentPosition componentPosition, float scale, Alignment alignment) {
        this.text = text;
        this.componentPosition = componentPosition;
        this.scale = scale;
        this.alignment = alignment;
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public String text() {
        return text;
    }

    public float scale() {
        return scale;
    }

    public Alignment alignment() {
        return alignment;
    }

    @Override
    public ComponentPosition position() {
        return componentPosition;
    }
}