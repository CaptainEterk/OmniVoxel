package omnivoxel.client.game.graphics.menu;

import omnivoxel.client.game.graphics.api.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.api.opengl.text.font.Font;
import omnivoxel.client.game.graphics.menu.components.Component;
import omnivoxel.client.game.graphics.menu.components.LayoutComponent;
import omnivoxel.client.game.graphics.menu.components.TextComponent;
import omnivoxel.client.game.graphics.menu.position.ComponentPosition;

import java.io.IOException;

public class MenuRenderer {
    private final LayoutComponent mainComponent;
    private Font font;

    public MenuRenderer(LayoutComponent mainComponent) {
        this.mainComponent = mainComponent;
    }

    public void init() throws IOException {
        // TODO: This should be able to use other fonts
        this.font = Font.create("Minecraft.ttf");
    }

    public void renderComponents(TextRenderer textRenderer) {
        renderComponent(mainComponent, textRenderer, 0, 0);
    }

    private void renderComponent(Component component, TextRenderer textRenderer, int offsetX, int offsetY) {
        if (component == null || component.isHidden()) {
            return;
        }
        ComponentPosition componentPosition = component.position();
        if (component instanceof TextComponent textComponent) {
            textRenderer.queueText(font, textComponent.text(), componentPosition.x() + offsetX, componentPosition.y() + offsetY, textComponent.scale(), textComponent.alignment());
        } else if (component instanceof LayoutComponent layoutComponent) {
            layoutComponent.getComponents().forEach((id, componentValue) -> renderComponent(componentValue.getComponent(), textRenderer, offsetX + layoutComponent.position().x(), offsetY + layoutComponent.position().y()));
        }
    }

    public void addComponent(String id, Component component) {
        mainComponent.addComponent(id, component);
    }

    public void cleanup() {
        font.cleanup();
    }

    public Font getFont() {
        return font;
    }
}