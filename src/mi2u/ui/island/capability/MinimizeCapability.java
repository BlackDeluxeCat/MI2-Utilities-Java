package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.MinimizeCapabilityEvent;
import mi2u.ui.island.*;

import static mi2u.MI2UVars.textb;

/**
 * 监听响应最小化动作的能力。
 */
public class MinimizeCapability extends IslandCapability{
    public boolean minimized;

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof MinimizeCapabilityEvent min){
            minimized = min.minimized;
            if(owner != null){
                buildMinimized(owner, minimized);
            }
            return true;
        }
        return false;
    }

    public void buildMinimized(Island island, boolean minimized){
        if(minimized){
            island.clearChildren();
            island.button(island.name, textb, () -> {
                island.fire(MinimizeCapabilityEvent.obtain().setMinimized(false));
            });
        }else {
            island.rebuild();
        }
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof MinimizeCapabilityEvent min){
            min.minimized = this.minimized;
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json){
        json.writeValue("minimized", minimized);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        minimized = json.readValue("minimized", boolean.class, false, jsonData);
    }
}
