package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

/**
 * 根节点专用布局
 */
public class RootStackLayout implements ChildrenLayout{
    public RootStackLayout(){
    }

    @Override
    public void applyRebuild(Table table, Seq<Island> children){
        for(Island child : children){
            child.rebuild();
            table.addChild(child);
        }
    }

    @Override
    public void write(Json json){
    }

    @Override
    public void read(Json json, JsonValue jsonData){
    }
}
