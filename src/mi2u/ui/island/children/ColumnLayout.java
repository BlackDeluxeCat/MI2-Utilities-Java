package mi2u.ui.island.children;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.island.Island;

/**
 * 垂直列布局。children 从上到下排列。
 */
public class ColumnLayout implements ChildrenLayout{
    public boolean verticalScrollable;

    public ColumnLayout(){
    }

    public ColumnLayout(boolean verticalScrollable){
        this.verticalScrollable = verticalScrollable;
    }

    @Override
    public void apply(Table table, Seq<Island> children){
        table.clear();
        for(Island child : children){
            table.add(child).growX().row();
        }
        if(verticalScrollable){
            // TODO: 包装 ScrollPane
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("verticalScrollable", verticalScrollable);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        verticalScrollable = json.readValue("verticalScrollable", boolean.class, false, jsonData);
    }
}
