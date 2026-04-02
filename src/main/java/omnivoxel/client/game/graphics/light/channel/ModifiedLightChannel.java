package omnivoxel.client.game.graphics.light.channel;

import omnivoxel.server.ConstantServerSettings;

public class ModifiedLightChannel implements LightChannel {
    private final int index;
    private final LightChannel parent;
    private final int modificationCount;
    private byte light;

    private ModifiedLightChannel(int index, byte light, LightChannel parent, int modificationCount) {
        this.index = index;
        this.light = light;
        this.parent = parent;
        this.modificationCount = modificationCount;
    }

    public ModifiedLightChannel(int index, byte light, LightChannel parent) {
        this(index, light, parent, 1);
    }

    @Override
    public byte getLighting(int index) {
        return index == this.index ? light : parent.getLighting(index);
    }

    @Override
    public LightChannel setLighting(int idx, byte newLight) {
        if (idx == index) {
            light = newLight;
            return this;
        } else {
            if (modificationCount < ConstantServerSettings.MODIFICATION_GENERALIZATION_LIMIT) {
                return new ModifiedLightChannel(idx, newLight, this, modificationCount + 1);
            }
            return new GeneralLightChannel(this).setLighting(idx, newLight);
        }
    }
}
