package mi2u.ui.island.widget;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.ui.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandConfigureWidget implements WidgetContent{
    protected Island owner;
    public IslandOverlayAccess access;

    public Island selectedIsland;

    /** 左侧功能按钮 */
    protected Table buttonsTable;
    /** 中部主设置面板 */
    protected IslandConfigurePanel islandConfigurePanel;
    /** 右侧分支导航面板 */
    protected IslandBranchTable branchTable;

    public IslandConfigureWidget(){}

    public IslandConfigureWidget(IslandOverlayAccess access){
        buttonsTable = new Table();
        buttonsTable.background(Styles.black3);
        buttonsTable.button("从剪切板导入", textb, () -> {
            try{
                var root = JsonUtils.json.fromJson(Island.class, Core.app.getClipboardText());
                if(root != null){
                    getAccess().setRoot(root);
                }
            }catch (Exception e){
                Log.err("导入失败", e);
                Vars.ui.showErrorMessage("导入失败: " + e.getMessage());
            }
        }).with(funcSetTextb);
        buttonsTable.row();
        buttonsTable.button("导出到剪切板", textb, () -> {
            Core.app.setClipboardText(JsonUtils.json.prettyPrint(getAccess().getRoot()));
        }).with(funcSetTextb);

        branchTable = new IslandBranchTable();
        branchTable.background(Styles.black3);
        branchTable.build();
        branchTable.canConfirmValidation = isle -> selectedIsland != isle;
        branchTable.onConfirm = isle -> setSelectedIsland(isle);

        islandConfigurePanel = new IslandConfigurePanel(
            (child, parent) -> {
                addChild(child, parent);
            },
            removed -> {
                removeChild(removed);
            }
        );

        setAccess(access);
    }

    public void setAccess(IslandOverlayAccess access){
        this.access = access;

        setSelectedIsland(getAccess().getRoot());
    }

    // 以下是自构建相关方法
    @Override
    public void rebuild(Island island){
        island.add(buttonsTable).padRight(10f).bottom();
        island.add(islandConfigurePanel).bottom();
        island.add(branchTable).bottom().padLeft(10f).minWidth(100f);
    }

    public IslandOverlayAccess getAccess(){
        return access;
    }

    public void setSelectedIsland(@Nullable Island island){
        selectedIsland = island;
        islandConfigurePanel.setTarget(selectedIsland);
        branchTable.setTarget(selectedIsland);
    }

    public void addChild(Island island, Island parent){
        IslandUtils.addChild(parent, island);
        branchTable.setTarget(branchTable.getTarget()); // 只刷新左侧分支导航器
    }

    public void removeChild(Island removed){
        if(removed.getParentIsland() != null && removed.getParentIsland().content instanceof ChildrenContent cc){
            cc.removeChild(removed);
        }
        if(selectedIsland == removed) setSelectedIsland(getAccess().getRoot());
    }

    @Override
    public void attach(Island owner){
        this.owner = owner;
    }

    @Override
    public Island getOwner() {
        return owner;
    }

    public interface IslandOverlayAccess{
        Island getRoot();

        void setRoot(Island root);
    }
}
