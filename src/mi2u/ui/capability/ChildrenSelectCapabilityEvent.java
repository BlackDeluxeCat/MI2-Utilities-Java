package mi2u.ui.capability;

import arc.util.pooling.*;

/**
 * 选中某个 children 动作的能力事件。
 */
public class ChildrenSelectCapabilityEvent extends CapabilityEvent{
    public int index;

    public static ChildrenSelectCapabilityEvent obtain(){
        var value = Pools.get(ChildrenSelectCapabilityEvent.class, ChildrenSelectCapabilityEvent::new).obtain();
        value.isQuery = true;
        return value;
    }

    public ChildrenSelectCapabilityEvent setIndex(int index){
        this.index = index;
        return this;
    }
}
