package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

/**
 * 根节点专用布局
 */
public class RootStackLayout implements ChildrenLayout{
    public transient IslandConfigurer configurer;
    public int savedX, savedY;

    public RootStackLayout(){
    }

    public void setConfigurer(IslandConfigurer configurer){
        this.configurer = configurer;
        if(savedX != 0 && savedY != 0){
            configurer.setPosition(savedX, savedY);
        }
    }

    @Override
    public void applyRebuild(Table table, ChildrenContent content){
        for(Island child : content.children){
            child.layout.positionManaged = false;
            child.rebuild();
            table.addChild(child);
        }

        if(configurer != null) table.addChild(configurer);
    }

    @Override
    public void write(Json json){
        if(configurer != null){
            savedX = (int)configurer.x;
            savedY = (int)configurer.y;
            json.writeValue("configurer-x", savedX);
            json.writeValue("configurer-y", savedY);
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        savedX = json.readValue("configurer-x", int.class, 0, jsonData);
        savedY = json.readValue("configurer-y", int.class, 0, jsonData);
    }
}
