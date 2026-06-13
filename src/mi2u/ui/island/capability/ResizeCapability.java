package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.ResizeCapabilityEvent;

/**
 * 监听响应调整尺寸动作的能力。
 */
public class ResizeCapability extends IslandCapability{
    public float width, height;

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof ResizeCapabilityEvent resize){
            width += resize.deltaX;
            height += resize.deltaY;
            if(owner != null){
                owner.setSize(Math.max(width, 0), Math.max(height, 0));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof ResizeCapabilityEvent resize){
            resize.deltaX = this.width;
            resize.deltaY = this.height;
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json){
        json.writeValue("width", width);
        json.writeValue("height", height);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        width = json.readValue("width", float.class, 0f, jsonData);
        height = json.readValue("height", float.class, 0f, jsonData);
    }
}
