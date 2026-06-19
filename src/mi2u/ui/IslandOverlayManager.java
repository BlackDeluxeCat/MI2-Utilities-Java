package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.ui.*;

//TODO将islandConfigureContainer抽离为独立widgetisland
public class IslandOverlayManager {
    public boolean editMode = true;
    // root不会被序列化
    public Island root;
    public Island selectedIsland;

    /** island设置面板 */
    protected Table islandConfigureContainer;
    protected IslandConfigurePanel islandConfigurePanel;
    /** island设置面板左侧分支导航面板 */
    protected IslandBranchTable leftBranchTable;

    public IslandOverlayManager(){}

    public void onClientLoad(){
        root = new Island("root", new ChildrenContent(new RootStackLayout())){
            // 整个overlay的重建似乎由该方法包圆了
            @Override
            public void rebuild() {
                super.rebuild();
                fill(shell -> {
                    shell.top();
                    shell.add(islandConfigureContainer).padTop(100f);
                });
            }
        };
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        leftBranchTable = new IslandBranchTable();
        leftBranchTable.background(Styles.black3);
        leftBranchTable.build();
        leftBranchTable.canConfirmValidation = isle -> selectedIsland != isle;
        leftBranchTable.onConfirm = isle -> setSelectedIsland(isle);

        islandConfigurePanel = new IslandConfigurePanel(
                this::addIsland,
                this::removeIsland,
                this::addIsland
        );
        islandConfigurePanel.setTarget(root);

        islandConfigureContainer = new Table();
        islandConfigureContainer.add(leftBranchTable).padRight(10f);
        islandConfigureContainer.add(islandConfigurePanel);

        setSelectedIsland(root);

        Core.scene.add(root);
        root.rebuild();
    }

    /**推荐添加方法
     *
     * @param island
     * @param parent
     */
    public void addIsland(Island island, @Nullable Island parent){
        if (parent != null && parent.content instanceof ChildrenContent cc) {
            // 尝试移除旧父级再添加
            island.setParentIsland(null);
            island.setParentIsland(parent);
        }
        leftBranchTable.setTarget(leftBranchTable.getTarget());
    }

    public void removeIsland(Island island){
        island.setParentIsland(null);
        if(selectedIsland == island) setSelectedIsland(root);
    }

    public void setSelectedIsland(@Nullable Island island){
        selectedIsland = island;
        islandConfigurePanel.setTarget(selectedIsland);
        leftBranchTable.setTarget(selectedIsland);
    }

}
