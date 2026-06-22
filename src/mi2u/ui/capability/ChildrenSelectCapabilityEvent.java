package mi2u.ui.capability;

/**
 * 选中某个 children 动作的能力事件。
 * <p>
 * 使用方自行持有单例实例，不复用池。
 */
public class ChildrenSelectCapabilityEvent extends CapabilityEvent{
    public int index;

    public ChildrenSelectCapabilityEvent setIndex(int index){
        this.index = index;
        return this;
    }

    public ChildrenSelectCapabilityEvent(){
        isQuery = true;
    }
}
