package mi2u.ui.island;

import arc.scene.ui.layout.*;
import arc.util.serialization.Json.*;

/**
 * 内容多态接口。负责把内容构建进 Island、暴露自己的设置 UI，并序列化自己的状态。
 * build(Island) 可以拿到 Island 引用，但内容应把它当作只读上下文。
 */
public interface IslandContent extends JsonSerializable{
    void attach(Island owner);
    Island getOwner();

    /**
     * 将内容 UI 构建到 island 中。
     * 只放轻操作
     * @param island 只读上下文，不应修改树结构。
     */
    void rebuild(Island island);

    /** 是否有设置项。 */
    default boolean hasSetting(){
        return false;
    }

    /** 将设置 UI 构建到传入的 table 中。 */
    default void buildSettingsTable(Table table, Island island){
        table.clear();
        table.add("未实现设置");
    }
}
