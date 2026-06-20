package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;

/**
 * 监听响应"选中某个 child"动作的能力。
 * <p>
 * 不持有持久化索引状态——当前选中索引归 TabbedLayout 所有。
 */
public class ChildrenSelectCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            // TODO: 将 sel.index 写入 TabbedLayout.currentIndex 并切换显示
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            // TODO: 从 TabbedLayout.currentIndex 读取当前选中索引
            return true;
        }
        return false;
    }
}
