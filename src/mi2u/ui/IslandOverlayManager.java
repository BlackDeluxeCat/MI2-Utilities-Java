package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mi2u.io.*;
import mi2u.ui.island.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;

//TODO将islandConfigureContainer抽离为独立widgetisland
public class IslandOverlayManager {
    public boolean editMode = true;
    /** 根节点，不被序列化 */
    public WidgetGroup backendGroup = new WidgetGroup();
    public Island root;
    /** island设置面板 */
    public Island islandConfigureIsland;
    public IslandConfigureWidget.IslandOverlayAccess access = new IslandConfigureWidget.IslandOverlayAccess() {
        @Override
        public Island getRoot() {
            return root;
        }

        @Override
        public void setRoot(Island root) {
            IslandOverlayManager.this.root = root;
            rebuildLinks(root);
            replaceConfigureWidget(root);
            rebuild();
        }
    };

    public IslandOverlayManager(){}

    /** 必须在 onClientLoad 之前（或开头）调用。 */
    public static void registerJsonClasses(){
        // IslandContent
        JsonUtils.registerClass("children", ChildrenContent.class);
        JsonUtils.registerClass("text", TextWidget.class);
        JsonUtils.registerClass("dragHandle", DragHandle.class);
        JsonUtils.registerClass("tabHandle", TabHandle.class);
        JsonUtils.registerClass("configure", IslandConfigureWidget.class);

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

    public void onClientLoad(){
        registerJsonClasses();

        root = new Island("root", new ChildrenContent(new RootStackLayout()));

        islandConfigureIsland = new Island("ConfigurePanel", new IslandConfigureWidget(access));
        islandConfigureIsland.addCapability(new DragCapability());
        islandConfigureIsland.layout.x = Core.graphics.getWidth() / 2f - islandConfigureIsland.getWidth() / 2f;
        islandConfigureIsland.layout.y = Core.graphics.getHeight() / 2f - islandConfigureIsland.getHeight() / 2f;
        IslandUtils.addChild(root, islandConfigureIsland);
        rebuild();
    }

    public void rebuild(){
        backendGroup.clear();
        backendGroup.touchable = Touchable.childrenOnly;
        backendGroup.setFillParent(true);
        Core.scene.add(backendGroup);

        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        root.rebuild();
        backendGroup.addChild(root);
    }

    public void rebuildLinks(Island island){
        rebuildLinksRecursive(island, null);
    }

    private void rebuildLinksRecursive(Island island, Island parent){
        island.setParentIsland(parent);
        island.content.attach(island);

        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                rebuildLinksRecursive(child, island);
            }
        }
    }

    /**
     * 将 savedRoot 树中的临时设置岛替换为硬编码的设置岛
     * @param savedRoot 刚从 JSON 反序列化的树（全是临时实例）
     */
    public void replaceConfigureWidget(Island savedRoot){
        // ── 递归遍历替换 ──
        replaceConfigureWidgetRecursive(savedRoot);

        // ── 未替换，强制挂到 root ──
        if(!IslandUtils.isAscendantOf(root, islandConfigureIsland)){
            IslandUtils.addChild(root, islandConfigureIsland);
        }

        ((IslandConfigureWidget)islandConfigureIsland.content).setAccess(access);
    }

    private void replaceConfigureWidgetRecursive(Island island){
        if(island.content instanceof IslandConfigureWidget){
            var parent = island.getParentIsland();

            // 用硬编码实例替换保存树中的引用
            if(parent != null && parent.content instanceof ChildrenContent cc){
                islandConfigureIsland.layout = island.layout;   // 迁移 layout （直接替换引用）
                cc.replaceChild(island, islandConfigureIsland);
            }
        }

        // 递归子岛
        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                replaceConfigureWidgetRecursive(child);
            }
        }
    }
}
