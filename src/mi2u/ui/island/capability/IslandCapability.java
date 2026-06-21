package mi2u.ui.island.capability;

import mi2u.ui.capability.*;
import mi2u.ui.island.*;

/**
 * Island 能力基类。
 * 持有 owner 引用，子类通过 onChange/onQuery 响应特定 CapabilityEvent。
 */
public class IslandCapability implements ElementCapability{
    /** 运行时由 attach 注入.*/
    public Island owner;

    public IslandCapability(){
    }

    /** 将能力绑定到指定 Island。 */
    public void attach(Island island){
        this.owner = island;
    }

    public Island getOwner(){
        return owner;
    }
}
