package mi2u.ui.island;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.children.*;

/**
 * Island 是唯一的运行时 UI 外壳。
 * 继承 Table，持有内容、布局和能力，是被选中、拖拽、缩放、序列化的基本单位。
 */
public class Island extends Table implements JsonSerializable{
    public transient Island parentIsland;  // 仅根节点为null

    public IslandContent content;
    public IslandLayout layout;
    /** 能力列表。JSON 序列化由 write/read 手动处理。 */
    protected transient Seq<ElementCapability> capabilities = new Seq<>();

    public Island(String name, IslandContent content){
        this.name = name;
        this.content = content;
        content.attach(this);
        this.layout = new IslandLayout(this);
    }

    public Seq<ElementCapability> capabilities(){
        return capabilities;
    }

    public Island getParentIsland(){
        return parentIsland;
    }

    @Override
    public void act(float delta){
        actApplyIslandLayout();
        super.act(delta);
    }

    protected void actApplyIslandLayout(){
        if(!layout.positionManaged){
            this.x = layout.x;
            this.y = layout.y;
        }
    }

    @Deprecated
    public boolean setParentIsland(Island pare){
        if(pare == null){
            if(parentIsland != null && parentIsland.content instanceof ChildrenContent cc){
                cc.removeChild(this);
                parentIsland.rebuild();
            }
            parentIsland = null;
            return true;
        }else if(pare.content instanceof ChildrenContent cc){
            boolean added = cc.addChild(this);
            if(added){
                parentIsland = pare;
                parentIsland.rebuild();
            }
            return added;
        }else{
            return false;
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

    public static String getIslandName(Island island){
        return island == null || island.name == null || island.name.isEmpty() ? "<Invalid>" : island.name;
    }
}
