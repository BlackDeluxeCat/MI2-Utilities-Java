package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.DragCapabilityEvent;

/**
 * 监听响应拖拽动作的能力。
 * onChange 收到 DragCapabilityEvent 时确认可拖拽，并记录当前 delta。
 */
public class DragCapability extends IslandCapability{
    public transient float x, y;

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof DragCapabilityEvent drag){
            x += drag.deltaX;
            y += drag.deltaY;
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json){
        json.writeValue("x", x);
        json.writeValue("y", y);
        // drag delta 是运行时状态，暂不持久化
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        x = json.readValue("x", Float.class, jsonData);
        y = json.readValue("y", Float.class, jsonData);
    }
}
