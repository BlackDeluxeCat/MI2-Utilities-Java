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
    public boolean scrollable;

    public ColumnLayout(){
    }

    public ColumnLayout(boolean scrollable){
        this.scrollable = scrollable;
    }

    @Override
    public void applyRebuild(Table table, Seq<Island> children){
        for(Island island : children){
            island.rebuild();
        }

        if(scrollable){
            // TODO: 包装 ScrollPane
        }else{
            for(Island child : children){
                table.add(child).growX().row();
            }
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("verticalScrollable", scrollable);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        scrollable = json.readValue("verticalScrollable", boolean.class, false, jsonData);
    }
}
