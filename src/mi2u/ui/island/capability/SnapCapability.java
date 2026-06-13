package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.SnapCapabilityEvent;
import mi2u.ui.island.Island;

/**
 * 监听响应吸附动作的能力。
 * 吸附：屏边吸附和元素吸附。
 */
public class SnapCapability extends IslandCapability{
    /**
     * 吸附方向标识。无目标的方向应空置（= center 表示无吸附）。
     * align | left = 1 表示向左吸附到水平 target 的右侧。
     */
    public int snapAlign;

    /** 水平吸附目标。null 指代屏边吸附。 */
    public Island snapHorizontalTarget;

    /** 垂直吸附目标。null 指代屏边吸附。 */
    public Island snapVerticalTarget;

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof SnapCapabilityEvent snap){
            // TODO: 吸附逻辑（寻找周围吸附目标、更新 IslandLayout）
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof SnapCapabilityEvent snap){
            // TODO: 回传当前吸附状态
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json){
        json.writeValue("snapAlign", snapAlign);
        // target 序列化为 Island 路径，null 指代屏边吸附
        json.writeValue("snapHorizontalTarget", snapHorizontalTarget, Island.class);
        json.writeValue("snapVerticalTarget", snapVerticalTarget, Island.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        snapAlign = json.readValue("snapAlign", int.class, 0, jsonData);
        // TODO: read 后将路径解析回 Island 引用
        snapHorizontalTarget = json.readValue("snapHorizontalTarget", Island.class, null, jsonData);
        snapVerticalTarget = json.readValue("snapVerticalTarget", Island.class, null, jsonData);
    }
}
