package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.io.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.game.*;

public class IslandOverlay {
    public final String name;

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

    public IslandOverlay(String name){
        this.name = name;
    }

    public void load(){
        configurer = new IslandConfigurer();
        configurer.setAccess(access);

        Island savedRoot = null;
        try{
            savedRoot = JsonUtils.json.fromJson(Island.class, SettingHandler.global.getStr("islandOverlay." + name));
        }catch(Exception e){
            Log.err("Island overlay JSON parse error: " + name + ".", e);
        }
        if(savedRoot != null){
            loadOverlay(savedRoot);  // 注意：如果load失败，root回滚为null
        }
        if(root == null){
            // 重装为初始root
            Log.infoTag("MI2U", "Create default overlay for " + name);
            root = new Island("root", new ChildrenContent(new RootStackLayout()));
            RootStackLayout rootLayout = (RootStackLayout)((ChildrenContent)root.content).childrenLayout;
            rootLayout.setConfigurer(configurer);
            configurer.setPosition(Core.graphics.getWidth() / 2f - configurer.getPrefWidth() / 2f, Core.graphics.getHeight() / 2f - configurer.getPrefHeight() / 2f);
            rebuild();
        }
    }

    public void rebuild(){
        backendGroup.clear();
        backendGroup.touchable = Touchable.childrenOnly;
        backendGroup.setFillParent(true);
        configurer.selectRoot();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        root.rebuild();
        backendGroup.addChild(root);
    }

    public void save(){
        if(saveTimer.get(10000)){
            forceSave();
        }
    }

    public void forceSave(){
        String current = JsonUtils.json.toJson(root);
        if(!current.equals(rootJson)){
            SettingHandler.global.putString("islandOverlay." + name, current);
            rootJson = current;
            Log.infoTag("MI2U", "Island overlay saved: " + name);
        }
    }

    public void loadOverlay(Island savedRoot){
        Island oldRoot = this.root; // 暂存旧根
        try {
            IslandOverlay.this.root = savedRoot;
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
            IslandOverlay.this.root = oldRoot;   // 回滚
            Log.err("Load overlay " + name + " failed, rollback.", e);
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
