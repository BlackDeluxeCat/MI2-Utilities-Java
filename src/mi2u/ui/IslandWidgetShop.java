package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import mi2u.io.*;
import mi2u.ui.island.*;
import mi2u.ui.island.capability.*;
import mi2u.ui.island.children.*;
import mi2u.ui.island.widget.*;
import mindustry.*;
import mindustry.gen.*;

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

        table(t -> {

            t.button(Iconc.paste + "导入分支作为子级", textb, () -> {
                try{
                    var jsonIsland = IslandUtils.json2Island(Core.app.getClipboardText());
                    if(IslandUtils.isRoot(jsonIsland)){
                        throw new RuntimeException("Root island is not acceptable.");
                    }
                    IslandUtils.rebuildLinks(jsonIsland);
                    IslandUtils.runRecursive(jsonIsland, island -> {
                        if(island.content instanceof WidgetContent wc){
                            wc.onRebindReference(jsonIsland);
                        }
                    });
                    IslandUtils.regenerateIdsRecursive(jsonIsland);
                    click(jsonIsland);
                }catch (Exception e){
                    Vars.ui.showException("导入失败", e);
                }
            }).with(funcSetTextb);

            //t.add("从剪切板数据生成副本，追加到子级列表中");
        });

        row();

        table(t -> {

            t.button("文本", textb, () -> {
                var island = new Island("Text", new TextWidget("Cat Rin"));
                click(island);
            }).with(funcSetTextb);

            t.button("Column框架", textb, () -> {
                var island = new Island("Column", new ChildrenContent(new ColumnLayout()));
                click(island);
            }).with(funcSetTextb);

            t.button("Row框架", textb, () -> {
                var island = new Island("Row", new ChildrenContent(new RowLayout()));
                click(island);
            }).with(funcSetTextb);

            t.button("Tab框架", textb, () -> {
                var island = new Island("Tab", new ChildrenContent(new TabbedLayout()));
                island.addCapability(new TabSelectCapability());
                click(island);
            }).with(funcSetTextb);

        });

        row();

        table(t -> {

            t.button("拖拽把手", textb, () -> {
                var island = new Island("DragHandle", new DragHandle());
                click(island);
            }).with(funcSetTextb);

            t.button("标签页把手", textb, () -> {
                var island = new Island("TabHandle", new TabHandle());
                click(island);
            }).with(funcSetTextb);

            t.button("最小化把手", textb, () -> {
                var island = new Island("MinimizeHandle", new MinimizeHandle());
                click(island);
            }).with(funcSetTextb);

        });


    }
}
