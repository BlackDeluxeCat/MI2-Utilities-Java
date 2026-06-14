package mi2u.ui;

import arc.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mi2u.ui.island.*;
import mi2u.ui.island.widget.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandOverlayManager {
    public boolean editMode = true;
    public WidgetGroup root = new WidgetGroup();
    public Seq<Island> islands = new Seq<>();

    public Table widgetShop = new Table();

    public Table islandConfigureTable = new Table();

    public IslandOverlayManager(){}

    public void onClientLoad(){
        rebuildWidgetShop();
        rebuildIslandConfigureTable();
        rebuildOverlay();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        Core.scene.add(root);
    }

    public void rebuildOverlay(){
        root.clear();
        root.fill(shell -> {
            shell.bottom();
            shell.add(widgetShop);
        });
        islands.each(island -> {
            island.rebuild();
            root.addChild(island);
        });
    }

    public void addIsland(Island island){
        islands.add(island);
        rebuildOverlay();
    }

    public void rebuildWidgetShop(){
        widgetShop.clear();
        widgetShop.button("测试文本小组件", textb, () -> {
            var widget = new TextWidget();
            widget.name = "Cat Rin";
            var island = new Island("TestTextIsland", widget);
            island.x = root.getWidth() / 2f;
            island.y = root.getHeight() / 2f;
            addIsland(island);
        }).with(funcSetTextb);
    }

    public void rebuildIslandConfigureTable(){
    }
}
