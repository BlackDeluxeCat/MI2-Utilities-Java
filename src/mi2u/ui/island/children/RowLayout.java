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
    public boolean scrollable;

    public RowLayout(){
    }

    public RowLayout(boolean scrollable){
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
                table.add(child).growY().row();
            }
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("horizontalScrollable", scrollable);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        scrollable = json.readValue("horizontalScrollable", boolean.class, false, jsonData);
    }
}
