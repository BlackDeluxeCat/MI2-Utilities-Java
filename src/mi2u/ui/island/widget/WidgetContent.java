package mi2u.ui.island.widget;

import arc.util.serialization.*;
import mi2u.ui.island.IslandContent;

/**
 * 用户小组件接口。具体 widget 直接实现此接口。
 * <p>
 * Widget 默认应支持多实例，除非功能明确需要共享状态。
 */
public interface WidgetContent extends IslandContent{
    /** 小组件标识名称。 */
    String name();

    @Override
    default void read(Json json, JsonValue jsonData){
    }

    @Override
    default void write(Json json){
    }
}
