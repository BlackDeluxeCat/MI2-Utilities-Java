package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;

/**
 * 监听响应吸附动作的能力。
 * 吸附：屏边吸附和元素吸附。
 * <p>
 * 不持有持久化吸附状态——吸附信息归 IslandLayout 所有。
 */
public class SnapCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof SnapCapabilityEvent snap){
            // TODO: 吸附逻辑（寻找周围吸附目标、更新 owner.layout 的 snap 字段）
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof SnapCapabilityEvent snap){
            // TODO: 从 owner.layout 回传当前吸附状态
            return true;
        }
        return false;
    }
}
