package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandOverlayManager {
    public boolean editMode = true;
    // root不会被序列化
    public Island root;
    public Seq<Island> rootIslands = new Seq<>();
    public Island selectedIsland;

    public Table widgetShop = new Table();

    /** island设置面板 */
    protected Table islandConfigureTable = new Table();
    /** island设置面板左侧分支导航面板 */
    protected IslandBranchTable leftBranchTable;

    /** island设置面板详细表单面板 */
    protected Table cfgSubTable;

    public IslandOverlayManager(){}

    public void onClientLoad(){
        root = new Island("root", new ChildrenContent(new RootStackLayout()));
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;

        leftBranchTable = new IslandBranchTable();
        leftBranchTable.background(Styles.black3);
        leftBranchTable.build();
        leftBranchTable.onConfirm = isle -> setSelectedIsland(isle);
        leftBranchTable.setTarget(root);

        setSelectedIsland(root);

        cfgSubTable = new Table();
        cfgSubTable.background(Styles.black3);

        Core.scene.add(root);
        rebuildOverlay();
    }

    // 全量重建
    public void rebuildOverlay(){
        root.rebuild();

        root.fill(shell -> {
            shell.top();
            shell.add(islandConfigureTable).padTop(100f);
        });
    }

    /**推荐添加方法
     *
     * @param island
     * @param parent
     */
    public void addIsland(Island island, @Nullable Island parent){
        removeIsland(island);// 尝试移除旧父级再添加
        if (parent != null && parent.content instanceof ChildrenContent cc) {
            island.setParentIsland(parent);
        }else{
            rootIslands.add(island);
        }
        leftBranchTable.setTarget(leftBranchTable.getTarget());
        rebuildOverlay();
    }

    public void removeIsland(Island island){
        island.setParentIsland(null);
        rootIslands.remove(island);
        if(selectedIsland == island) setSelectedIsland(root);
        if(leftBranchTable.getTarget() == island) leftBranchTable.setTarget(root);
        rebuildOverlay();
    }

    public void setSelectedIsland(@Nullable Island island){
        selectedIsland = island;
        rebuildIslandConfigureTableFor(island);
    }

    /** 重建组件商城
     *
     * @param islandCons 对创建的组件进行后处理和添加到指定位置
     */
    public void rebuildWidgetShop(Cons<Island> islandCons){
        widgetShop.clear();
        widgetShop.button("测试文本", textb, () -> {
            var widget = new TextWidget();
            widget.name = "Cat Rin";
            var island = new Island("TestText", widget);
            islandCons.get(island);
        }).with(funcSetTextb);
    }

    // 重建孤岛设置面板
    public void rebuildIslandConfigureTableFor(Island island){
        var table = islandConfigureTable;
        table.clear();
        table.defaults().growY();

        // 左侧布局分支
        table.add(leftBranchTable).padRight(10f);

        // 中间主菜单
        table.table(main -> {
            main.top();

            // islandlayout设置入口
            // islandcontent设置入口
            // 拖拽摇杆
            // 调换父级
            // 删除
            main.table(topt -> {
                topt.background(Styles.black3);
                topt.add("已选中的控件：" + getIslandName(island)).left();
                topt.row();
                topt.table(t -> {

                    if(island != null){
                        t.button("岛屿设置", textb, () -> island.layout.buildSettingsTable(cfgSubTable)).with(funcSetTextb);

                        t.button("内容设置", textb, () -> island.content.buildSettingsTable(cfgSubTable, island)).with(funcSetTextb);

                        t.button("更改父级", textb, null).with(funcSetTextb);

                        if(island.content instanceof ChildrenContent){
                            t.button("添加子级组件", textb, () -> {
                                rebuildWidgetShop(isle -> {
                                    if(island == root){
                                        isle.layout.x = root.getWidth() / 2f;
                                        isle.layout.y = root.getHeight() / 2f;
                                    }
                                    addIsland(isle, island);
                                });
                                cfgSubTable.clear();
                                cfgSubTable.add(widgetShop);
                            }).with(funcSetTextb);
                        }

                        if(island.parent instanceof Island){
                            t.add("拖拽移动").with(l -> {
                                l.addListener(new InputListener(){
                                    float touchDownX, touchDownY;
                                    float originalX, originalY;

                                    @Override
                                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                                        touchDownX = x;
                                        touchDownY = y;
                                        originalX = island.layout.x;
                                        originalY = island.layout.y;
                                        l.setColor(Pal.accent);
                                        Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand);
                                        return true;
                                    }

                                    @Override
                                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                                        super.touchDragged(event, x, y, pointer);
                                        island.layout.x = originalX + x - touchDownX;
                                        island.layout.y = originalY + y - touchDownY;
                                    }

                                    @Override
                                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                                        super.touchUp(event, x, y, pointer, button);
                                        l.setColor(Color.white);
                                        Core.graphics.restoreCursor();
                                    }
                                });
                            });
                        }
                    }

                    if(island != null){
                        t.spacerX(() -> 20f);   // 删除键视觉远离其他功能键
                        t.button("[scarlet]删除", textb, () -> {
                            Vars.ui.showConfirm("将移除该岛屿及其包含的所有内容，确定吗？", () -> removeIsland(island));
                        }).disabled(b -> island == root).with(funcSetTextb);
                    }

                });
            }).padBottom(10f);

            main.row();

            main.pane(cfgSubTable).grow().maxHeight(600f);
        });

        // 其他靠Popup承担
    }

    public static String getIslandName(Island island){
        return island == null || island.name == null || island.name.isEmpty() ? "<Invalid>" : island.name;
    }
}
