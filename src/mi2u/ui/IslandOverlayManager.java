package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandOverlayManager {
    public boolean editMode = true;
    public WidgetGroup root = new WidgetGroup();
    public Seq<Island> rootIslands = new Seq<>();
    @Nullable public Island selectedIsland = null;

    public Table widgetShop = new Table();

    /** 当前选中的island设置一级菜单 */
    protected Table islandConfigureTable = new Table();

    /** 当前选中的island设置二级菜单 */
    protected Table islandConfigureSubTable = new Table();

    public IslandOverlayManager(){}

    public void onClientLoad(){
        rebuildWidgetShop();
        rebuildIslandConfigureTableFor(selectedIsland);
        rebuildOverlay();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        Core.scene.add(root);
    }

    public void rebuildOverlay(){
        root.clear();
        root.fill(shell -> {
            shell.bottom();
            shell.add(islandConfigureTable);
            shell.row();
            shell.add(widgetShop);
        });
        rootIslands.each(island -> {
            island.rebuild();
            root.addChild(island);
        });
    }

    public void addIsland(Island island, @Nullable Island parent){
        if(island == null) return;
        if (parent != null && parent.content instanceof ChildrenContent cc) {
            cc.addChild(island);
        }else{
            rootIslands.add(island);
        }
        rebuildOverlay();
    }

    public void setSelectedIsland(@Nullable Island island){
        selectedIsland = island;
        rebuildIslandConfigureTableFor(island);
    }

    public void rebuildWidgetShop(){
        widgetShop.clear();
        widgetShop.button("测试文本小组件", textb, () -> {
            var widget = new TextWidget();
            widget.name = "Cat Rin";
            var island = new Island("TestTextIsland", widget);
            island.x = root.getWidth() / 2f;
            island.y = root.getHeight() / 2f;
            addIsland(island, null);
        }).with(funcSetTextb);
    }

    public void rebuildIslandConfigureTableFor(Island island){
        var table = islandConfigureTable;
        table.clear();
        table.defaults().growY();

        // 面包屑布局分支
        // 选中方式：父子路径label，子级下拉菜单labels，短按是展开下拉菜单，长按是确认选中
        // 编辑方式：拖拽摇杆在root位移，同级调序上下键，父级更换模式开启键
        // 进入父级更换模式后，面包屑label长按效果变为确认更换

        // 左侧面包屑
        var branchTable = new IslandBranchTable();
        branchTable.build();
        branchTable.onConfirm = isle -> setSelectedIsland(isle);
        branchTable.setTarget(island);
        table.add(branchTable);

        // 中间主菜单
        table.table(t -> {
            t.top();
            t.add(getIslandName(island));
        });

        // 右侧信息菜单
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
                t.defaults().top().growY();
                t.pane(mainColumn);
                t.pane(subColumn);
            });

            row();

            button("", textb, () -> {
                if(onConfirm != null) onConfirm.get(target);
            }).with(funcSetTextb).update(b -> b.setText(confirmTextProvider.get(target)));
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
        }

        public void rebuildMainColumn(Table t){
            t.clear();
            t.defaults().growX().width(100f).left();

            //root
            t.button(getIslandName(null), textb, () -> setTarget(null)).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));

            if(target == null) return;
            var seq = new Seq<Island>();
            Island elem = target;
            do{
                seq.add(elem);
                elem = elem.parent instanceof Island isle ? isle : null;
            }while(elem != null);

            for(int i = seq.size - 1; i >= 0; i--){
                var isle = seq.get(i);
                t.add(">");
                t.row();
                t.button(getIslandName(isle), textb, () -> setTarget(isle)).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));
            }
        }

        public void rebuildSubColumnFor(Table t, Seq<Island> seq){
            t.clear();
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
