package mi2u.ui.island.children;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mi2u.ui.island.Island;

/**
 * 水平行布局。children 从左到右排列。
 */
public class RowLayout implements ChildrenLayout{
    public boolean horizontalScrollable;

    public RowLayout(){
    }

    public RowLayout(boolean horizontalScrollable){
        this.horizontalScrollable = horizontalScrollable;
    }

    @Override
    public void apply(Table table, Seq<Island> children){
        table.clear();
        for(Island child : children){
            table.add(child).growY();
        }
        if(horizontalScrollable){
            // TODO: 包装 ScrollPane
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("horizontalScrollable", horizontalScrollable);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        horizontalScrollable = json.readValue("horizontalScrollable", boolean.class, false, jsonData);
    }
}
