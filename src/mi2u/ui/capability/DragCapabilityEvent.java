package mi2u.ui.capability;

import arc.util.pooling.*;

/**
 * 拖拽动作的能力事件。
 */
public class DragCapabilityEvent extends CapabilityEvent{
    public float deltaX, deltaY;

    public static DragCapabilityEvent obtain(){
        return Pools.get(DragCapabilityEvent.class, DragCapabilityEvent::new).obtain();
    }

    public DragCapabilityEvent setDelta(float deltaX, float deltaY){
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        return this;
    }
}
