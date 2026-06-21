package mi2u.ui.capability;

import arc.util.pooling.*;

/**
 * 拖拽动作的能力事件。
 */
public class DragCapabilityEvent extends CapabilityEvent{
    /** 拖拽起始点 */
    public float originX, originY;
    /** 自拖拽起始点originXY偏移的累积量 */
    public float dx, dy;

    public static DragCapabilityEvent obtain(){
        return Pools.get(DragCapabilityEvent.class, DragCapabilityEvent::new).obtain();
    }

    public void setOrigin(float originX, float originY){
        this.originX = originX;
        this.originY = originY;
    }

    public void setDelta(float dx, float dy){
        this.dx = dx;
        this.dy = dy;
    }
}
