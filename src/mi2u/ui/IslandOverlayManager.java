package mi2u.ui;

import arc.*;
import arc.scene.*;
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

    public Table islandConfigureTable = new Table();

    public IslandOverlayManager(){}

    public void onClientLoad(){
        rebuildWidgetShop();
        rebuildIslandConfigureTableFor(null);
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

        // 面包屑布局路径
        // 选中方式：父子路径label，子级下拉菜单labels，短按是展开下拉菜单，长按是确认选中
        // 编辑方式：拖拽摇杆在root位移，同级调序上下键，父级更换模式开启键
        // 进入父级更换模式后，面包屑label长按效果变为确认更换

        // 构建面包屑
        table.table(t -> {

            t.button("root", () -> {
                setSelectedIsland(null);
            });

            if(island == null) return;
            var seq = new Seq<Island>();
            Island elem = island;
            do{
                seq.add(elem);
                elem = elem.parent instanceof Island isle ? isle : null;
            }while(elem != null);

            for(int i = seq.size - 1; i >= 0; i--){
                var isle = seq.get(i);
                t.add(">");
                t.button(isle.name, () -> setSelectedIsland(isle))
                    .disabled(selectedIsland == isle);
            }
        });

        table.row();

        if(island == null){
            table.add("未选中");
        }else{
            table.add("已选中"+island.name);
        }


        // 二级设置入口
    }
}
