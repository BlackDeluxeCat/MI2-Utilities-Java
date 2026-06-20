package mi2u.ui.island.capability;

import mi2u.ui.capability.*;
import mi2u.ui.island.*;

/**
 * Island 能力基类。
 * 持有 owner 引用，子类通过 onChange/onQuery 响应特定 CapabilityEvent。
 */
public class IslandCapability implements ElementCapability{
    /** 运行时由 attach/onload 注入，不参与 JSON。 */
    public transient Island owner;

    public IslandCapability(){
    }

    /** 将能力绑定到指定 Island。 */
    public void attach(Island island){
        this.owner = island;
    }

    /**
     * 反序列化后的回调：将自身属性重新应用到 Island。
     * 在 read 完成后由加载方调用。
     */
    public void onload(Island island){
    }
}
