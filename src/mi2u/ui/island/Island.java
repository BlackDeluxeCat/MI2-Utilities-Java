package mi2u.ui.island;

import arc.func.*;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.Json.JsonSerializable;
import arc.util.serialization.JsonValue;
import mi2u.ui.capability.ElementCapability;

/**
 * Island 是唯一的运行时 UI 外壳。
 * 继承 Table，持有内容、布局和能力，是被选中、拖拽、缩放、序列化的基本单位。
 */
public class Island extends Table implements JsonSerializable{
    public String name;
    /** 显示条件。为 Boolp 类型，TODO使用kv序列化。 */
    public transient Boolp showCondition = () -> true;
    public IslandContent content;
    public IslandLayout layout;

    /** 能力列表。JSON 序列化由 write/read 手动处理。 */
    protected transient Seq<ElementCapability> capabilities = new Seq<>();

    public Island(){
    }

    public Island(String name, IslandContent content, IslandLayout layout){
        this.name = name;
        this.content = content;
        this.layout = layout;
    }

    public Seq<ElementCapability> capabilities(){
        return capabilities;
    }

    /** 委托给 content.build(this) 重建 UI。由调用方在适当时机触发。 */
    public void rebuild(){
        clear();
        for(ElementCapability cap : capabilities){
            addListener(cap);
        }
        if(content != null){
            content.build(this);
        }
    }

    // -- serialization ---------------------------------------------------

    @Override
    public void write(Json json){
        json.writeValue("name", name);
        json.writeValue("content", content, IslandContent.class);
        json.writeValue("layout", layout);
        json.writeValue("capabilities", capabilities, Seq.class, ElementCapability.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        name = json.readValue("name", String.class, "", jsonData);

        content = json.readValue("content", IslandContent.class, jsonData);

        layout = json.readValue("layout", IslandLayout.class, jsonData);

        // 手动反序列化 capabilities
        capabilities.clear();
        JsonValue capsJson = jsonData.get("capabilities");
        if(capsJson != null && capsJson.isArray()){
            for(JsonValue child = capsJson.child; child != null; child = child.next){
                ElementCapability cap = json.readValue(ElementCapability.class, null, child);
                if(cap != null){
                    capabilities.add(cap);
                }
            }
        }

        // read 之后调用方应调用 rebuild() 重建 UI
    }
}
