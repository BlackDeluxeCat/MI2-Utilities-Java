package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.io.*;
import mi2u.ui.island.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.*;
import mindustry.game.*;

public class IslandOverlayManager {
    public boolean editMode = true;

    /** 根节点，不被序列化 */
    public WidgetGroup backendGroup = new WidgetGroup();
    public Island root;
    public String rootJson;
    protected MI2Utils.IntervalMillis saveTimer = new MI2Utils.IntervalMillis();

    /** island设置面板 */
    public IslandConfigurer configurer;
    public IslandOverlayAccess access = new IslandOverlayAccess() {
        @Override
        public Island getRoot() {
            return root;
        }

        @Override
        public void loadFromJson(String json) {
            Island jsonRoot = null;
            try{
                jsonRoot = JsonUtils.json.fromJson(Island.class, json);
            }catch(Exception e){
                Log.err("Island overlay JSON parse error.", e);
            }
            if(jsonRoot != null){
                loadOverlay(jsonRoot);
            }
        }

        @Override
        public float getAutoSaveCooldown() {
            return saveTimer.getTime(0) / 10000f;
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

        configurer = new IslandConfigurer();
        configurer.setAccess(access);

        Island savedRoot = null;
        try{
            savedRoot = JsonUtils.json.fromJson(Island.class, SettingHandler.global.getStr("islandOverlay"));
        }catch(Exception e){
            Log.err("Island overlay JSON parse error.", e);
        }
        if(savedRoot != null){
            loadOverlay(savedRoot);  // 注意：如果load失败，root回滚为null
        }
        if(root == null){
            // 重装为初始root
            Log.infoTag("MI2U", "Create default overlay.");
            root = new Island("root", new ChildrenContent(new RootStackLayout()));
            RootStackLayout rootLayout = (RootStackLayout)((ChildrenContent)root.content).childrenLayout;
            rootLayout.setConfigurer(configurer);
            configurer.setPosition(Core.graphics.getWidth() / 2f - configurer.getPrefWidth() / 2f, Core.graphics.getHeight() / 2f - configurer.getPrefHeight() / 2f);
            rebuild();
        }

        // auto save
        Events.run(EventType.Trigger.update, () -> {
            if(saveTimer.get(10000)){
                saveOverlay();
            }
        });
    }

    public void rebuild(){
        backendGroup.clear();
        backendGroup.touchable = Touchable.childrenOnly;
        backendGroup.setFillParent(true);
        Core.scene.add(backendGroup);

        configurer.selectRoot();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        root.rebuild();
        backendGroup.addChild(root);
    }

    public void saveOverlay(){
        String current = JsonUtils.json.toJson(root);
        if(!current.equals(rootJson)){
            SettingHandler.global.putString("islandOverlay", current);
            rootJson = current;
            Log.infoTag("MI2U", "Island overlay saved.");
        }
    }

    public void loadOverlay(Island savedRoot){
        Island oldRoot = this.root; // 暂存旧根
        try {
            IslandOverlayManager.this.root = savedRoot;
            RootStackLayout rootLayout = (RootStackLayout)((ChildrenContent)root.content).childrenLayout;
            rootLayout.setConfigurer(configurer);
            rebuildLinks(root);
            IslandUtils.runRecursive(root, island -> {
                if(island.content instanceof WidgetContent wc){
                    wc.onRebindReference(root);
                }
            });
            rebuild();
        } catch (Exception e) {
            IslandOverlayManager.this.root = oldRoot;   // 回滚
            Log.err("Load overlay failed, rollback.", e);
        }
    }

    public void rebuildLinks(Island island){
        rebuildLinksRecursive(island, null);
    }

    private void rebuildLinksRecursive(Island island, Island parent){
        island.setParentIsland(parent);

        if(island.content instanceof ChildrenContent cc){
            for(Island child : cc.getChildren()){
                rebuildLinksRecursive(child, island);
            }
        }
    }
}
