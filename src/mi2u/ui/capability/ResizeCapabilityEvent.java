package mi2u.ui.capability;

/**
 * 调整尺寸动作的能力事件。
 */
public class ResizeCapabilityEvent extends CapabilityEvent{
    public float deltaX, deltaY;// 目前暂时认为这样和drag一致，比较合理

    public ResizeCapabilityEvent(){
    }

    public ResizeCapabilityEvent(float width, float height){
        this.deltaX = width;
        this.deltaY = height;
    }

    public ResizeCapabilityEvent(boolean isQuery, float width, float height){
        super(isQuery);
        this.deltaX = width;
        this.deltaY = height;
    }
}
