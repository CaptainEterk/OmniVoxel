package omnivoxel.client.game.graphics.menu.components;

import omnivoxel.client.game.graphics.menu.position.ComponentPosition;
import omnivoxel.client.game.graphics.menu.position.ComponentPositionOrigin;

import java.util.HashMap;
import java.util.Map;

public class LayoutComponent implements Component {
    private final Map<String, ComponentValue> components;
    private final ComponentPosition componentPosition;
    private boolean hidden;

    public LayoutComponent(ComponentPosition componentPosition, boolean hidden) {
        this.componentPosition = componentPosition;
        this.hidden = hidden;
        this.components = new HashMap<>();
    }

    public LayoutComponent(ComponentPosition componentPosition) {
        this(componentPosition, false);
    }

    public LayoutComponent(ComponentPositionOrigin componentPositionOrigin) {
        this(new ComponentPosition(0, 0, componentPositionOrigin));
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

    public void addComponent(String id, int x, int y, Component component) {
        this.components.put(id, new ComponentValue(id, x, y, component));
    }

    public void addComponent(String id, Component component) {
        this.components.put(id, new ComponentValue(id, 0, 0, component));
    }

    public Component getComponent(String id) {
        return components.get(id).getComponent();
    }

    public Map<String, ComponentValue> getComponents() {
        return components;
    }

    @Override
    public ComponentPosition position() {
        return componentPosition;
    }

    public static final class ComponentValue {
        private final String id;
        private final Component component;
        private int x;
        private int y;

        public ComponentValue(String id, int x, int y, Component component) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.component = component;
        }

        public String getID() {
            return id;
        }

        public Component getComponent() {
            return component;
        }

        public int x() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int y() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}