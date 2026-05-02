package omnivoxel.client.game.graphics.menu;

import omnivoxel.client.game.graphics.api.opengl.text.Alignment;
import omnivoxel.client.game.graphics.api.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.api.opengl.text.font.Font;
import omnivoxel.client.game.graphics.menu.components.LayoutComponent;
import omnivoxel.client.game.graphics.menu.components.TextComponent;
import omnivoxel.client.game.graphics.menu.position.ComponentPosition;
import omnivoxel.client.game.graphics.menu.position.ComponentPositionOrigin;

import java.io.IOException;

/**
 * This class is in charge of <br> 1. Rendering the menus<br>2. Updating the menus (showing/hiding, position, etc...)
 */
public class MenuSystem {
    private final MenuRenderer menuRenderer;
    private final TextRenderer textRenderer;

    public MenuSystem(MenuRenderer menuRenderer, TextRenderer textRenderer) {
        this.menuRenderer = menuRenderer;
        this.textRenderer = textRenderer;
        LayoutComponent debugScreen = new LayoutComponent(ComponentPositionOrigin.TOP_LEFT);
        debugScreen.addComponent("left", new TextComponent("Hello World!", new ComponentPosition(0, 0, ComponentPositionOrigin.TOP_LEFT), 0.6f, Alignment.LEFT));
        debugScreen.addComponent("right", new TextComponent("Hello World!", new ComponentPosition(0, 0, ComponentPositionOrigin.TOP_RIGHT), 0.6f, Alignment.RIGHT));
        menuRenderer.addComponent("omnivoxel:debug_screen", debugScreen);
    }

    public void tick() {
        menuRenderer.renderComponents(textRenderer);
    }

    public void cleanup() {
        menuRenderer.cleanup();
    }

    public void init() throws IOException {
        menuRenderer.init();
    }

    public Font getFont() {
        return menuRenderer.getFont();
    }
}