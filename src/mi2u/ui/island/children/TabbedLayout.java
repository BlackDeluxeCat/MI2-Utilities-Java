package mi2u.ui.island.children;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textbtoggle;

/**
 * 标签页布局。每次只显示一个 child，通过 currentIndex 切换。
 * <p>
 * currentIndex == 0 为默认页（全尺寸）。
 * currentIndex == lastPage 可作为"最小化"语义推导来源。
 */
public class TabbedLayout implements ChildrenLayout{
    public int index;

    @Override
    public void applyRebuild(Table table, ChildrenContent content){
        if(content.children.isEmpty()){
            table.add("<Tabbed>");
            return;
        }

        int idx = Mathf.clamp(index, 0, content.children.size - 1);
        Island selected = content.children.get(idx);
        selected.rebuild();
        table.add(selected).grow();
    }

    @Override
    public void buildSettingsTable(Table table, ChildrenContent content){
        if(content.children.isEmpty()){
            table.add("<没有子级>");
        }

        table.pane(t -> {
            for(int i = 0; i < content.children.size; i++){
                var child = content.children.get(i);
                int finalI = i;
                t.button(child.name, textbtoggle, () -> {
                    index = Mathf.clamp(finalI, 0, content.children.size - 1);
                    content.getOwner().rebuild();
                }).with(funcSetTextb).checked(b -> index == finalI).disabled(b -> index == finalI);
            }
        }).growX().with(p -> p.setScrollingDisabled(false, true));
        // TODO: 显示 tab 切换按钮列表
    }

    @Override
    public void write(Json json){
        json.writeValue("index", index);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        index = json.readValue("index", int.class, 0, jsonData);
    }
}
