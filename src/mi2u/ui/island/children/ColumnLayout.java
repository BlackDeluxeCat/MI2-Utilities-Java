package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

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

    /**
     *
     * @param table an empty table, usually island
     * @param children children list
     */
    @Override
    public void applyRebuild(Table table, Seq<Island> children){
        for(Island island : children){
            island.layout.positionManaged = true;
            island.rebuild();
        }

        if(children.isEmpty()){
            table.add("<Column>");
        }

        if(scrollable){
            // TODO: 包装 ScrollPane
        }else{
            for(Island child : children){
                table.add(child).growX().row();
            }
            table.pack();
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
