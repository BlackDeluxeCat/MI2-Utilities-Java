package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.elements.*;
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
    public WidgetGroup root = new WidgetGroup();
    public Seq<Island> rootIslands = new Seq<>();
    @Nullable public Island selectedIsland = null;

    public Table widgetShop = new Table();

    /** island设置面板 */
    protected Table islandConfigureTable = new Table();
    /** island设置面板左侧分支导航面板 */
    protected IslandBranchTable leftBranchTable;

    /** island设置面板详细表单面板 */
    protected Table cfgSubTable;

    public IslandOverlayManager(){}

    public void onClientLoad(){
        leftBranchTable = new IslandBranchTable();
        leftBranchTable.background(Styles.black3);
        leftBranchTable.build();
        leftBranchTable.onConfirm = isle -> setSelectedIsland(isle);
        leftBranchTable.setTarget(null);

        cfgSubTable = new Table();
        cfgSubTable.background(Styles.black3);

        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        Core.scene.add(root);
        rebuildOverlay();
    }

    // 全量重建
    public void rebuildOverlay(){
        leftBranchTable.setTarget(leftBranchTable.target);
        rebuildIslandConfigureTableFor(selectedIsland);
        rebuildRoot();
    }

    // 重建布局树
    public void rebuildRoot(){
        root.clear();

        root.fill(shell -> {
            shell.top();
            shell.add(islandConfigureTable).padTop(100f);
        });

        rootIslands.each(island -> {
            island.rebuild();
            root.addChild(island);
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
        rebuildOverlay();
    }

    public void removeIsland(Island island){
        island.setParentIsland(null);
        rootIslands.remove(island);
        if(selectedIsland == island) selectedIsland = null;
        if(leftBranchTable.target == island) leftBranchTable.target = null;
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
        widgetShop.button("测试文本小组件", textb, () -> {
            var widget = new TextWidget();
            widget.name = "Cat Rin";
            var island = new Island("TestTextIsland", widget);
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

                    if(island == null){
                        t.button("添加子级组件", textb, () -> {
                            rebuildWidgetShop(isle -> {
                                isle.layout.x = root.getWidth() / 2f;
                                isle.layout.y = root.getHeight() / 2f;
                                addIsland(isle, null);
                            });
                            cfgSubTable.clear();
                            cfgSubTable.add(widgetShop);
                        }).with(funcSetTextb);

                    }else{
                        t.button("岛屿设置", textb, () -> island.layout.buildSettingsTable(cfgSubTable)).with(funcSetTextb);

                        t.button("内容设置", textb, () -> island.content.buildSettingsTable(cfgSubTable, island)).with(funcSetTextb);

                        t.button("更改父级", textb, null).with(funcSetTextb);

                        if(island.content instanceof ChildrenContent){
                            t.button("添加子级组件", textb, () -> {
                                rebuildWidgetShop(isle -> {
                                    addIsland(isle, island);
                                });
                                cfgSubTable.clear();
                                cfgSubTable.add(widgetShop);
                            });
                        }

                        if(!(island.parent instanceof Island)){
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
                        }).with(funcSetTextb);
                    }

                });
            }).padBottom(10f);

            main.row();

            main.pane(cfgSubTable).grow().maxHeight(600f);
        });

        // 其他靠Popup承担
    }

    /**
     * 第一列：从root到target的分支列表
     * 第二列：点击某个父级显示所有子级，折叠列表
     * 底部：确认按钮
     */
    public class IslandBranchTable extends Table{
        public Island target;
        public Cons<Island> onConfirm;
        public Func<Island, String> confirmTextProvider = island -> "切换到[accent]" + getIslandName(island);

        Table mainColumn = new Table();
        Table subColumn = new Table();

        public void build(){
            clear();

            table(t -> {
                t.defaults().growY();
                t.pane(mainColumn);
                t.pane(subColumn);
            });

            row();

            button("", textb, () -> {
                if(onConfirm != null) onConfirm.get(target);
            }).with(funcSetTextb).wrapLabel(true).growX().update(b -> b.setText(confirmTextProvider.get(target)));
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            //pack();
        }

        public void setTarget(Island island){
            target = island;
            rebuildMainColumn(mainColumn);
            if(island != null && island.content instanceof ChildrenContent cc){
                rebuildSubColumnFor(subColumn, cc.children);
            }else if(island == null){
                rebuildSubColumnFor(subColumn, rootIslands);
            }else{
                rebuildSubColumnFor(subColumn, null);
            }
            pack();
        }

        public void rebuildMainColumn(Table t){
            t.clear();
            t.top();
            t.defaults().growX().width(100f);

            //root
            t.button(getIslandName(null) + " > ", textb, () -> setTarget(null)).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));

            if(target == null) return;
            var seq = new Seq<Island>();
            Island elem = target;
            do{
                seq.add(elem);
                elem = elem.parent instanceof Island isle ? isle : null;
            }while(elem != null);

            for(int i = seq.size - 1; i >= 0; i--){
                var isle = seq.get(i);
                t.row();
                t.button(getIslandName(isle) + " > ", textb, () -> setTarget(isle)).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));
            }
        }

        public void rebuildSubColumnFor(Table t, Seq<Island> seq){
            t.clear();
            t.top();
            if(seq == null) return;
            t.defaults().growX().width(80f);
            for(var isle : seq){
                t.button(getIslandName(isle), textb, () -> {
                    setTarget(isle);
                }).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));
                t.row();
            }
        }
    }

    public static String getIslandName(Island island){
        return island == null ? "root" : island.name == null || island.name.isEmpty() ? "<No Name>" : island.name;
    }
}
