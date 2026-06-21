package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;

/**
 * 监听响应拖拽动作的能力。
 * onChange 收到 DragCapabilityEvent 时确认可拖拽。
 */
public class DragCapability extends IslandCapability{

    @Override
    public boolean onChange(SceneEvent event){
        if(event instanceof DragCapabilityEvent drag){
            applyDelta(drag.dx, drag.dy);
            return true;
        }
        return false;
    }

    public void applyDelta(float dx, float dy){
        getOwner().layout.x += dx;
        getOwner().layout.y += dy;
    }
}
