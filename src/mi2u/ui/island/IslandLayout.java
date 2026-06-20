package mi2u.ui.island;

import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

/**
 * Island 外壳的尺寸、位置和布局约束。
 * <p>
 * 包括固定尺寸、延展标记、权重、位置和吸附状态。
 */
public class IslandLayout implements JsonSerializable{
    public final transient Island owner;
    public float width = -1, height = -1;
    public boolean expandX, expandY;
    public float widthWeight = 1f, heightWeight = 1f;

    /** 位置。独立保存，不要直接复用 Element.x / y。 */
    public transient boolean positionManaged = false;
    public float x, y;

    /**
     * 吸附方向标识。
     * 无目标的方向应空置（= center 表示无吸附）。
     * target 为 null 指代屏边吸附。
     */
    public int snapAlign = Align.center;

    /** 水平吸附目标。null 指代屏边吸附。 */
    @Nullable public Island snapHorizontalTarget;

    /** 垂直吸附目标。null 指代屏边吸附。 */
    @Nullable public Island snapVerticalTarget;

    public IslandLayout(Island owner){
        this.owner = owner;
    }

    /** 将布局设置 UI 构建到传入的 table 中。 */
    public void buildSettingsTable(Table table){
        table.clear();
        table.add("岛屿设置");
    }

    @Override
    public void write(Json json){
        json.writeValue("width", width);
        json.writeValue("height", height);
        json.writeValue("expandX", expandX);
        json.writeValue("expandY", expandY);
        json.writeValue("widthWeight", widthWeight);
        json.writeValue("heightWeight", heightWeight);
        json.writeValue("x", x);
        json.writeValue("y", y);
        json.writeValue("snapAlign", snapAlign);
        json.writeValue("snapHorizontalTarget", snapHorizontalTarget.name, Island.class);
        json.writeValue("snapVerticalTarget", snapVerticalTarget.name, Island.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        width = json.readValue("width", float.class, width, jsonData);
        height = json.readValue("height", float.class, height, jsonData);
        expandX = json.readValue("expandX", boolean.class, expandX, jsonData);
        expandY = json.readValue("expandY", boolean.class, expandY, jsonData);
        widthWeight = json.readValue("widthWeight", float.class, widthWeight, jsonData);
        heightWeight = json.readValue("heightWeight", float.class, heightWeight, jsonData);
        x = json.readValue("x", float.class, 0f, jsonData);
        y = json.readValue("y", float.class, 0f, jsonData);
        snapAlign = json.readValue("snapAlign", int.class, 0, jsonData);
        // 需要加一些查找方式
        //snapHorizontalTarget = json.readValue("snapHorizontalTarget", Island.class, jsonData);
        //snapVerticalTarget = json.readValue("snapVerticalTarget", Island.class, jsonData);
    }
}
