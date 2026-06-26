package mi2u.ui.island.children;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.ui.island.*;

/**
 * Children 内容实现。
 * 持有子 Island 序列和一个可更换的 ChildrenLayout，作为逻辑容器的数据模型。
 * <p>
 * Island 外壳保持统一，只是内容策略不同。
 */
public class ChildrenContent implements IslandContent{
    public transient Island owner;
    protected Seq<Island> children = new Seq<>();
    public ChildrenLayout childrenLayout;

    public ChildrenContent(ChildrenLayout childrenLayout){
        this.childrenLayout = childrenLayout;
    }

    public Seq<Island> getChildren() {
        return children;
    }

    public boolean canAddChild(Island island){
        return island.getParentIsland() != getOwner();
    }

    /**
     * ChildrenContent的添加子级能力
     * @param island
     * @return 添加是否真实发生
     */
    public boolean addChild(Island island){
        var oldParent = island.getParentIsland();
        // 旧父级脱绑
        if(oldParent != null){
            if(oldParent.content instanceof ChildrenContent cc){
                boolean removed = cc.removeChild(island);
                if(!removed){
                    Log.warn("An island can not be removed from its parent, please check out.");
                    return false;
                }
            }else{
                Log.warn("An island has an illegal parent, please check out.");
                return false;
            }
        }
        // 绑定新父级
        island.parentIsland = owner;
        children.add(island);
        owner.rebuild();

        return true;
    }

    /**
     * ChildrenContent的删除子级能力
     * @param island
     * @return 删除是否真实发生
     */
    public boolean removeChild(Island island){
        var oldParent = island.getParentIsland();
        if(oldParent != owner) return false;
        island.parentIsland = null;
        children.remove(island);
        oldParent.rebuild();
        return true;
    }

    @Override
    public void attach(Island owner) {
        this.owner = owner;
    }

    @Override
    public Island getOwner() {
        return owner;
    }

    @Override
    public void rebuild(Island island){
        if(childrenLayout != null){
            childrenLayout.applyRebuild(island, this);
        }
    }

    @Override
    public boolean hasSetting(){
        return childrenLayout != null;
    }

    @Override
    public void buildSettingsTable(Table table, Island island){
        if(childrenLayout != null){
            childrenLayout.buildSettingsTable(table, this);
        }
    }

    // -- JsonSerializable ------------------------------------------------

    @Override
    public void write(Json json){
        json.writeValue("children", children, Seq.class, Island.class);
        json.writeValue("childrenLayout", childrenLayout, ChildrenLayout.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        json.readField(this, "children", jsonData);
        childrenLayout = json.readValue("childrenLayout", ChildrenLayout.class, jsonData);
    }
}
