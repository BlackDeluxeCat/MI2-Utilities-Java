package mi2u.ui.island.widget;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.serialization.*;
import mi2u.ui.*;
import mi2u.ui.capability.*;
import mi2u.ui.elements.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;

public class TabHandle implements WidgetContent{
    private Island owner;
    public Island target;
    private transient int tempTargetId; // 反序列化过程临时变量
    public TextButton handle;

    // 以下设置页均为懒初始化
    public PopupTable selector;
    public IslandBranchTable targetSelector;

    private ChildrenSelectCapabilityEvent e = new ChildrenSelectCapabilityEvent();

    public TabHandle(){
        handle = new TextButton("" + Iconc.list, textb);
        handle.update(() -> {
        });
        handle.changed(() -> {
            popupSelector();
        });
    }

    public void popupSelector(){
        if(selector == null){
            selector = new PopupTable();
            selector.background(Styles.black3);
            selector.margin(10f);
            selector.update(() -> {
                selector.snapTo(getOwner());
            });
        }
        selector.clear();
        selector.margin(10f);
        selector.addCloseButton();
        var listener = getListenerTabIsland();
        if(listener != null){
            if(listener.content instanceof ChildrenContent cc && cc.childrenLayout instanceof TabbedLayout tl){
                var seq = cc.getChildren();
                for(int i = 0; i < seq.size; i++){
                    // 既然构建button需要读取layout.children详情，索性直接控制layout.index，不再发送事件了
                    var child = seq.get(i);
                    final int finalI = i;
                    selector.button(child.name, textb, () -> {
                        tl.index = finalI;
                        cc.getOwner().rebuild();
                    }).with(funcSetTextb);
                    selector.row();
                }
                selector.popup();
                selector.pack();
                return;
            }
            selector.add("<响应者不是Tab框架>");
            selector.popup();
            return;
        }
        selector.add("<未发现Tab框架>");
        selector.popup();
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
            };
            targetSelector.build();
        }
        targetSelector.setTarget(getOwner());
        table.add(targetSelector);
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
        }
    }
}
