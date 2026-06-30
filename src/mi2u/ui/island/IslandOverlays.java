package mi2u.ui.island;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.ui.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.game.*;


/**
 * Overlay Layer 的静态管理器。
 * 负责全局一次性注册、layer 注册/查询、批量持久化。
 * <p>
 * 每个 {@link IslandOverlay} 实例对应一块独立的叠加层（如 scene、logic），
 * 拥有自己的 Island 树、Configurer、存盘状态和挂载目标。
 */
public class IslandOverlays{
    private static final Seq<IslandOverlay> layers = new Seq<>();

    /** 编辑模式开关，影响所有 layer。 */
    public static boolean editMode = true;

    public static void addLayer(IslandOverlay layer){
        layers.add(layer);
    }

    public static Seq<IslandOverlay> getLayers(){
        return layers;
    }

    @Nullable
    public static IslandOverlay getLayer(String name){
        return layers.find(l -> l.name.equals(name));
    }

    public static void init(){
        registerJsonClasses();

        var hudLayer = new IslandOverlay("hud");
        addLayer(hudLayer);
        Core.scene.add(hudLayer.backendGroup);  // 手动将外壳挂载到指定位置

        loadAll();

        // auto save
        Events.run(EventType.Trigger.update, () -> {
            saveAll();
        });
    }

    /** 自动保存所有 layer 的状态。频率受layer内部计时器限制。 */
    public static void saveAll(){
        for(var layer : layers){
            layer.save();
        }
    }

    /** 从存档加载所有 layer。 */
    public static void loadAll(){
        for(var layer : layers){
            layer.load();
        }
    }

    public static void registerJsonClasses(){
        // IslandContent
        JsonUtils.registerClass("children", ChildrenContent.class);
        JsonUtils.registerClass("text", TextWidget.class);
        JsonUtils.registerClass("dragHandle", DragHandle.class);
        JsonUtils.registerClass("tabHandle", TabHandle.class);

        // ChildrenLayout
        JsonUtils.registerClass("rootStack", RootStackLayout.class);
        JsonUtils.registerClass("column", ColumnLayout.class);
        JsonUtils.registerClass("row", RowLayout.class);
        JsonUtils.registerClass("tabbed", TabbedLayout.class);

        // IslandCapability
        JsonUtils.registerClass("dragCap", DragCapability.class);
        JsonUtils.registerClass("snapCap", SnapCapability.class);
        JsonUtils.registerClass("resizeCap", ResizeCapability.class);
        JsonUtils.registerClass("minimizeCap", MinimizeCapability.class);
        JsonUtils.registerClass("tabSelectCap", TabSelectCapability.class);

        // 容器元素类型
        JsonUtils.json.setElementType(ChildrenContent.class, "children", Island.class);
    }
}
