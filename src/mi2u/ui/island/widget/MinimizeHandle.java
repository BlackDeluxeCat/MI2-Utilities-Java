package mi2u.ui.island.widget;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2u.ui.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;

public class MinimizeHandle implements WidgetContent{
    private Island owner;
    public Island target;
    private transient int tempTargetId; // 反序列化过程临时变量
    public TextButton handle;

    public IslandBranchTable targetSelector;

    private ChildrenSelectCapabilityEvent e = new ChildrenSelectCapabilityEvent();

    public MinimizeHandle(){
        handle = new TextButton("-", textb);
        handle.update(() -> {
            var listener = getListenerTabIsland();
            if(listener != null && listener.content instanceof ChildrenContent cc && cc.childrenLayout instanceof TabbedLayout tl){
                handle.setText(tl.index == 0 ? "-" : "□");
            }
        });
        handle.changed(() -> {
            var listener = getListenerTabIsland();
            if(listener != null){
                if(listener.content instanceof ChildrenContent cc && cc.childrenLayout instanceof TabbedLayout tl){
                    int size = cc.getChildren().size;
                    if(size >= 1){
                        if(tl.index != 0){
                            tl.index = 0;
                        }else{
                            tl.index = 1;
                        }
                    }else{
                        tl.index = 0;
                    }
                    cc.getOwner().rebuild();
                }
            }
        });
    }

    @Override
    public void buildSettingsTable(Table table, Island island) {
        table.clear();
        table.label(() -> "发送目标: " + (target == null ? "<自身>" : target.name));
        table.row();
        if(targetSelector == null){
            targetSelector = new IslandBranchTable();
            targetSelector.onConfirm = isle -> {
                target = isle;
                buildSettingsTable(table, isle);
            };
            targetSelector.build();
        }
        targetSelector.setTarget(getOwner());
        table.add(targetSelector);

        // 拷贝显示 target Tab 的设置面板
        var listenerIsland = getListenerTabIsland();
        if(listenerIsland != null){
            table.row();
            table.table(t -> {
                listenerIsland.content.buildSettingsTable(t, listenerIsland);
            });
        }
    }

    public Island getListenerTabIsland(){
        var toFire = target;
        if(toFire == null){
            toFire = getOwner();
        }
        e.reset();
        e.setQuery(true);
        toFire.fire(e);
        if(e.listenerActor instanceof Island island) return island;
        return null;
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
    public void rebuild(Island island) {
        island.background(Styles.black3);
        island.add(handle).size(buttonSize);
    }

    @Override
    public void write(Json json) {
        json.writeValue("targetId", target == null ? -1 : target.id);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        tempTargetId = json.readValue("targetId", int.class, -1, jsonData);
    }

    @Override
    public void onRebindReference(Island root) {
        var island = IslandUtils.findIsland(root, tempTargetId);
        if(island != null){
            target = island;
        }else if(tempTargetId != -1){
            Log.infoTag("MI2U", "MinimizeHandle(ownerId=" + owner.id + ") lost reference to targetId=" + tempTargetId + ", fallback to self.");
        }
    }
}
