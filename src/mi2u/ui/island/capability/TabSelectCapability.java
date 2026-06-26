package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;

/**
 * 监听响应"选中某个 child"动作的能力。
 * <p>
 * 不持有持久化索引状态——当前选中索引归 TabbedLayout 所有。
 */
public class TabSelectCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            var island = getOwner();
            if(island != null && island.content instanceof ChildrenContent cc && cc.childrenLayout instanceof TabbedLayout tl){
                tl.index = sel.index;
                island.rebuild();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof ChildrenSelectCapabilityEvent sel){
            var island = getOwner();
            if(island != null && island.content instanceof ChildrenContent cc && cc.childrenLayout instanceof TabbedLayout tl){
                sel.setIndex(tl.index);
                return true;
            }
        }
        return false;
    }
}
