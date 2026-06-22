package mi2u.ui.capability;

/**
 * 拖拽动作的能力事件。
 * <p>
 * 各拖拽把手自行持有单例实例，不复用池。
 */
public class DragCapabilityEvent extends CapabilityEvent{
    /** 自拖拽起始点originXY偏移的累积量 */
    public float dx, dy;

    public void setDelta(float dx, float dy){
        this.dx = dx;
        this.dy = dy;
    }
}
