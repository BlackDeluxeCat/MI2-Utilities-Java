package mi2u.ui.island.widget;

import arc.util.*;
import mi2u.ui.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.ui.*;

public class IslandConfigureWidget implements WidgetContent{
    protected Island owner;
    public IslandOverlayAccess access;

    public Island selectedIsland;

    protected IslandConfigurePanel islandConfigurePanel;
    /** island设置面板左侧分支导航面板 */
    protected IslandBranchTable branchTable;

    public IslandConfigureWidget(IslandOverlayAccess access){
        this.access = access;
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
        setSelectedIsland(getAccess().getRoot());
    }

    // 以下是自构建相关方法
    @Override
    public void rebuild(Island island){
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
        if (parent != null && parent.content instanceof ChildrenContent cc) {
            cc.addChild(island);
        }
        branchTable.setTarget(branchTable.getTarget()); // 只刷新左侧分支导航器
    }

    public void removeChild(Island removed){
        if(removed.parentIsland != null && removed.parentIsland.content instanceof ChildrenContent cc){
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
    }
}
