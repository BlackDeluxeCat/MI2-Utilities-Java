package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.ChildrenSelectCapabilityEvent;

/**
 * 监听响应"选中某个 child"动作的能力。
 */
public class ChildrenSelectCapability extends IslandCapability{
    public int index;

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            index = sel.index;
            // TODO: 切换到对应 child 的显示
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            sel.index = index;
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json){
        json.writeValue("index", index);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        index = json.readValue("index", int.class, 0, jsonData);
    }
}
