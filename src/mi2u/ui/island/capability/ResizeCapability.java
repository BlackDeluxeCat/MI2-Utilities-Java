package mi2u.ui.island.capability;

import arc.scene.event.SceneEvent;
import mi2u.ui.capability.ResizeCapabilityEvent;

/**
 * 监听响应调整尺寸动作的能力。
 * <p>
 * 不持有持久化尺寸状态——尺寸归 IslandLayout 所有。
 */
public class ResizeCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof ResizeCapabilityEvent resize){
            if(owner != null && owner.layout != null){
                owner.layout.width += resize.deltaX;
                owner.layout.height += resize.deltaY;
                owner.setSize(Math.max(owner.layout.width, 0), Math.max(owner.layout.height, 0));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onQuery(SceneEvent event){
        if(event instanceof ResizeCapabilityEvent resize){
            if(owner != null && owner.layout != null){
                resize.deltaX = owner.layout.width;
                resize.deltaY = owner.layout.height;
            }
            return true;
        }
        return false;
    }
}
