package mi2u.ui.capability;

import arc.scene.event.SceneEvent;

/**
 * 能力事件基类。
 * 子 Element（例如拖拽把手）fire 此事件，向上冒泡直到某祖先 Element 的 capability 接受。
 *
 */
public class CapabilityEvent extends SceneEvent{
    public boolean isQuery;

    public CapabilityEvent(){
    }

    public CapabilityEvent setQuery(boolean isQuery){
        this.isQuery = isQuery;
        return this;
    }

    public CapabilityEvent(boolean isQuery){
        this.isQuery = isQuery;
    }
}
