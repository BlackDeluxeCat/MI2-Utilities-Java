package mi2u.ui.island;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.capability.*;

/**
 * Island 是唯一的运行时 UI 外壳。
 * 继承 Table，持有内容、布局和能力，是被选中、拖拽、缩放、序列化的基本单位。
 */
public class Island extends Table implements JsonSerializable{
    public int id;
    protected transient Island parentIsland;  // 仅根节点的此项为null

    public IslandContent content;
    public IslandLayout layout;
    /** 能力列表。JSON 序列化由 write/read 手动处理。 */
    protected transient Seq<IslandCapability> capabilities;

    protected Island(){
        id = newId();
        capabilities = new Seq<>();
    }

    public Island(String name, IslandContent content){
        this();
        this.name = name;
        this.content = content;
        content.attach(this);
        this.layout = new IslandLayout();
    }

    /** 添加能力。这个便捷方法对所添加的cap做了完整后处理。 */
    public void addCapability(IslandCapability cap){
        capabilities.add(cap);
        cap.attach(this);
    }

    public Seq<IslandCapability> getCapabilities(){
        return capabilities;
    }

    public Island getParentIsland(){
        return parentIsland;
    }

    public void setParentIsland(Island island){
        this.parentIsland = island;
    }

    @Override
    public void act(float delta){
        actApplyIslandLayout();
        super.act(delta);
    }

    protected void actApplyIslandLayout(){
        boolean needsLayout = false;
        if(!layout.positionManaged){
            this.x = layout.x;
            this.y = layout.y;
            needsLayout = true;
        }

        if(needsLayout){
            keepInStage();
            pack();
        }
    }

    /** 委托给 content.build(this) 轻操作重建岛屿。 */
    public void rebuild(){
        clear();
        for(ElementCapability cap : capabilities){
            addListener(cap);
        }
        if(content != null){
            content.rebuild(this);
        }
        pack();
    }

    // -- serialization ---------------------------------------------------

    @Override
    public void write(Json json){
        json.writeValue("id", id);
        json.writeValue("name", name);
        json.writeValue("content", content, IslandContent.class);
        json.writeValue("layout", layout, IslandLayout.class);
        if(!capabilities.isEmpty()) json.writeValue("capabilities", capabilities, Seq.class, ElementCapability.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        id = json.readValue("id", Integer.class, newId(), jsonData);
        name = json.readValue("name", String.class, "", jsonData);

        content = json.readValue("content", IslandContent.class, jsonData);
        if(content != null){
            content.attach(this);
        }

        layout = json.readValue("layout", IslandLayout.class, jsonData);
        if(layout == null){
            Log.infoTag("MI2U", "Loading island layout null at island: " + id);
            layout = new IslandLayout();
        }

        // 手动反序列化 capabilities
        capabilities.clear();
        JsonValue capsJson = jsonData.get("capabilities");
        if(capsJson != null && capsJson.isArray()){
            for(JsonValue child = capsJson.child; child != null; child = child.next){
                IslandCapability cap = json.readValue(IslandCapability.class, null, child);
                if(cap != null){
                    addCapability(cap);
                }
            }
        }

        // read 之后调用方应调用 rebuild() 重建 UI
    }

    public static String getIslandName(Island island){
        return island == null || island.name == null || island.name.isEmpty() ? "<Invalid>" : island.name;
    }

    // 有极小概率id碰撞
    public static int newId(){
        return Mathf.random(Integer.MAX_VALUE - 1);
    }
}
