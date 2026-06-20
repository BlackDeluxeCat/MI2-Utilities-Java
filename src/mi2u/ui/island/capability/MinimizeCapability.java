package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.*;

import static mi2u.MI2UVars.*;

/**
 * 监听响应最小化动作的能力。
 * <p>
 * 不持有持久化最小化状态——最小化状态由 TabbedLayout.currentIndex 推导。
 */
public class MinimizeCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof MinimizeCapabilityEvent min){
            if(owner != null){
                //TODO 这里改成控制TabbedLayout
            }
            return true;
        }
        return false;
    }

    @Deprecated
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
            // TODO: 从 TabbedLayout.currentIndex 推导最小化状态
            // min.minimized = (tabbedLayout.currentIndex == lastPage);
            return true;
        }
        return false;
    }
}
