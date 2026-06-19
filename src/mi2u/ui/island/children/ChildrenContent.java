package mi2u.ui.island.children;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.island.Island;
import mi2u.ui.island.IslandContent;

/**
 * Children 内容实现。
 * 持有子 Island 序列和一个可更换的 ChildrenLayout，作为逻辑容器的数据模型。
 * <p>
 * Island 外壳保持统一，只是内容策略不同。
 */
public class ChildrenContent implements IslandContent{
    public Seq<Island> children = new Seq<>();
    public ChildrenLayout childrenLayout;

    public ChildrenContent(ChildrenLayout childrenLayout){
        this.childrenLayout = childrenLayout;
    }

    public int childrenCount(){
        return children.size;
    }

    public boolean canAddChild(Island island){
        return true;
    }

    public boolean addChild(Island island){
        if(canAddChild(island)){
            children.add(island);
            return true;
        }
        return false;
    }

    public void removeChild(Island island){
        children.remove(island);
    }

    @Override
    public void rebuild(Island island){
        if(childrenLayout != null){
            childrenLayout.applyRebuild(island, children);
        }
    }

    @Override
    public boolean hasSetting(){
        return childrenLayout != null;
    }

    @Override
    public void buildSettingsTable(Table table, Island island){
        if(childrenLayout != null){
            childrenLayout.buildSettingsTable(table);
        }
    }

    // -- JsonSerializable ------------------------------------------------

    @Override
    public void write(Json json){
        json.writeValue("children", children, Seq.class, Island.class);
        json.writeValue("childrenLayout", childrenLayout, ChildrenLayout.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        json.readField(this, "children", jsonData);
        childrenLayout = json.readValue("childrenLayout", ChildrenLayout.class, jsonData);
    }
}
