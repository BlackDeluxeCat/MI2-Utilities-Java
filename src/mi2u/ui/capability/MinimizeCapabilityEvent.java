package mi2u.ui.capability;

import arc.util.pooling.*;

/**
 * 最小化动作的能力事件。
 */
public class MinimizeCapabilityEvent extends CapabilityEvent{
    public boolean minimized;

    public static MinimizeCapabilityEvent obtain(){
        return Pools.get(MinimizeCapabilityEvent.class, MinimizeCapabilityEvent::new).obtain();
    }

    public MinimizeCapabilityEvent setMinimized(boolean minimized){
        this.minimized = minimized;
        return this;
    }
}
