package mi2u.ui.island;

import arc.scene.ui.layout.Table;
import arc.util.serialization.Json;
import arc.util.serialization.Json.JsonSerializable;
import arc.util.serialization.JsonValue;

/**
 * Island 外壳的尺寸和布局约束。
 * 包括固定尺寸、延展标记和权重。
 */
public class IslandLayout implements JsonSerializable{
    public float width = -1, height = -1;
    public boolean expandX, expandY;
    public float widthWeight = 1f, heightWeight = 1f;

    public IslandLayout(){
    }

    public IslandLayout(float width, float height){
        this.width = width;
        this.height = height;
    }

    /** 将布局设置 UI 构建到传入的 table 中。 */
    public void buildSettingsTable(Table table){
    }

    @Override
    public void write(Json json){
        json.writeValue("width", width);
        json.writeValue("height", height);
        json.writeValue("expandX", expandX);
        json.writeValue("expandY", expandY);
        json.writeValue("widthWeight", widthWeight);
        json.writeValue("heightWeight", heightWeight);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        width = json.readValue("width", float.class, width, jsonData);
        height = json.readValue("height", float.class, height, jsonData);
        expandX = json.readValue("expandX", boolean.class, expandX, jsonData);
        expandY = json.readValue("expandY", boolean.class, expandY, jsonData);
        widthWeight = json.readValue("widthWeight", float.class, widthWeight, jsonData);
        heightWeight = json.readValue("heightWeight", float.class, heightWeight, jsonData);
    }
}
