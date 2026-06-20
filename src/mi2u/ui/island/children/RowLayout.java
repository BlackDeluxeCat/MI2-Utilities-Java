package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

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
            island.layout.positionManaged = true;
            island.rebuild();
        }

        if(children.isEmpty()){
            table.add("<Row>");
        }

        if(scrollable){
            // TODO: 包装 ScrollPane
        }else{
            for(Island child : children){
                table.add(child).growY();
            }
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("scrollable", scrollable);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        scrollable = json.readValue("scrollable", boolean.class, false, jsonData);
    }
}
