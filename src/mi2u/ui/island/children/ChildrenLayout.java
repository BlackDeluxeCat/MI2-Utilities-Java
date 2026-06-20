package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.Json.*;
import mi2u.ui.island.*;

/**
 * Children 布局策略接口。
 * 负责排列 ChildrenContent 内部的 children Island。
 * <p>
 * 实现可以持有可序列化状态（如 TabbedLayout 的 currentIndex）。
 */
public interface ChildrenLayout extends JsonSerializable{

    /**
     * rebuild所有children，将 children 按布局策略填充到 table 中。
     */
    void applyRebuild(Table table, Seq<Island> children);

    /**
     * 将布局的设置 UI 构建到传入的 table 中。
     * 在编辑模式的配置面板中展示。
     */
    default void buildSettingsTable(Table table){
    }
}
