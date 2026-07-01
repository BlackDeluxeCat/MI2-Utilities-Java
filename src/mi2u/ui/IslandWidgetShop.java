package mi2u.ui;

import arc.func.*;
import arc.scene.ui.layout.*;
import mi2u.ui.island.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

public class IslandWidgetShop extends Table {
    public Cons<Island> onCreate;

    public IslandWidgetShop(){
        rebuild();
    }

    public void click(Island island){
        onCreate.get(island);
    }

    /** 重建组件商城 */
    public void rebuild(){
        clear();

        defaults().uniformX();

        button("文本", textb, () -> {
            var island = new Island("Text", new TextWidget("Cat Rin"));
            click(island);
        }).with(funcSetTextb);

        button("Column框架", textb, () -> {
            var island = new Island("Column", new ChildrenContent(new ColumnLayout()));
            click(island);
        }).with(funcSetTextb);

        button("Row框架", textb, () -> {
            var island = new Island("Row", new ChildrenContent(new RowLayout()));
            click(island);
        }).with(funcSetTextb);

        button("Tab框架", textb, () -> {
            var island = new Island("Tab", new ChildrenContent(new TabbedLayout()));
            island.addCapability(new TabSelectCapability());
            click(island);
        }).with(funcSetTextb);

        row();

        button("拖拽把手", textb, () -> {
            var island = new Island("DragHandle", new DragHandle());
            click(island);
        }).with(funcSetTextb);

        button("标签页把手", textb, () -> {
            var island = new Island("TabHandle", new TabHandle());
            click(island);
        }).with(funcSetTextb);

        button("最小化把手", textb, () -> {
            var island = new Island("MinimizeHandle", new MinimizeHandle());
            click(island);
        }).with(funcSetTextb);
    }
}
