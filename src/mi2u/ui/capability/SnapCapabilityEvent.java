package mi2u.ui.capability;

import arc.util.pooling.*;

/**
 * 吸附动作的能力事件。
 */
public class SnapCapabilityEvent extends CapabilityEvent{

    public static SnapCapabilityEvent obtain(){
        return Pools.get(SnapCapabilityEvent.class, SnapCapabilityEvent::new).obtain();
    }
}
