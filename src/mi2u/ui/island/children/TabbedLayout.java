package mi2u.ui.island.children;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

/**
 * 标签页布局。每次只显示一个 child，通过 currentIndex 切换。
 * <p>
 * currentIndex == 0 为默认页（全尺寸）。
 * currentIndex == lastPage 可作为"最小化"语义推导来源。
 */
public class TabbedLayout implements ChildrenLayout{
    public int currentIndex;

    public TabbedLayout(){
    }

    public TabbedLayout(int currentIndex){
        this.currentIndex = currentIndex;
    }

    @Override
    public void applyRebuild(Table table, Seq<Island> children){
        if(children.size == 0) return;

        int idx = Mathf.clamp(currentIndex, 0, children.size - 1);
        Island selected = children.get(idx);
        selected.rebuild();
        table.add(selected).grow();
    }

    @Override
    public void buildSettingsTable(Table table){
        // TODO: 显示 tab 切换按钮列表
    }

    @Override
    public void write(Json json){
        json.writeValue("currentIndex", currentIndex);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        currentIndex = json.readValue("currentIndex", int.class, 0, jsonData);
    }
}
