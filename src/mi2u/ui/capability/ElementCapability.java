package mi2u.ui.capability;

import arc.scene.event.*;

/**
 * 能力响应接口，可响应后代 Element 发出的能力动作。
 * <p>
 * 监听子 Element 的 SceneEvent 冒泡，路由到 onQuery / onChange 处理。
 */
public interface ElementCapability extends EventListener{

    /**
     * 事件基础状态管理，路由查询事件和命令事件。
     * 若事件是 CapabilityEvent 的实例，委托给 onQuery / onChange。
     */
    @Override
    default boolean handle(SceneEvent event){
        if(event instanceof CapabilityEvent capEvent){
            boolean handled;
            if(capEvent.isQuery){
                handled = onQuery(event);
            }else{
                handled = onChange(event);
            }
            if(handled){
                event.stop();
            }
            return handled;
        }
        return false;
    }

    /**
     * 以 SceneEvent 为载体回传状态。
     * @return true 表示此能力处理了该查询事件
     */
    default boolean onQuery(SceneEvent event){
        return false;
    }

    /**
     * 接收修改命令。
     * @return true 表示此能力处理了该命令事件（事件冒泡终止）
     */
    default boolean onChange(SceneEvent event){
        return false;
    }
}
