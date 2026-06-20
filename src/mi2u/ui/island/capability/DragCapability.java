package mi2u.ui.island.capability;

import arc.scene.event.*;
import mi2u.ui.capability.*;

/**
 * 监听响应拖拽动作的能力。
 * onChange 收到 DragCapabilityEvent 时确认可拖拽，并累计 delta。
 * <p>
 * x / y 是运行时瞬态累加器，不参与序列化。
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
}
