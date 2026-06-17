package mi2u.ui;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.island.*;
import mi2u.ui.island.children.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;

/**
 * 第一列：从root到target的分支列表
 * 第二列：点击某个父级显示所有子级，折叠列表
 * 底部：确认按钮
 */
public class IslandBranchTable extends Table {
    protected Island target;
    public Cons<Island> onConfirm;
    public Func<Island, String> confirmTextProvider = island -> "切换到[accent]" + IslandOverlayManager.getIslandName(island);

    Table mainColumn = new Table();
    Table subColumn = new Table();

    public void build() {
        clear();

        table(t -> {
            t.defaults().growY();
            t.pane(mainColumn);
            t.pane(subColumn);
        });

        row();

        button("", textb, () -> {
            if (onConfirm != null) onConfirm.get(target);
        }).with(funcSetTextb).wrapLabel(true).growX().update(b -> b.setText(confirmTextProvider.get(target)));
    }

    public Island getTarget(){
        return target;
    }

    public void setTarget(Island island) {
        target = island;
        rebuildMainColumn(mainColumn);
        if (island != null && island.content instanceof ChildrenContent cc) {
            rebuildSubColumnFor(subColumn, cc.children);
        } else {
            rebuildSubColumnFor(subColumn, null);
        }
        pack();
    }

    public void rebuildMainColumn(Table t) {
        t.clear();
        t.top();
        t.defaults().growX().width(100f);

        if (target == null) return;
        var seq = new Seq<Island>();
        Island elem = target;
        do {
            seq.add(elem);
            elem = elem.getParentIsland();
        } while (elem != null);

        for (int i = seq.size - 1; i >= 0; i--) {
            var isle = seq.get(i);
            t.row();
            t.button(IslandOverlayManager.getIslandName(isle) + " > ", textb, () -> setTarget(isle)).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));
        }
    }

    public void rebuildSubColumnFor(Table t, Seq<Island> seq) {
        t.clear();
        t.top();
        if (seq == null) return;
        t.defaults().growX().width(80f);
        for (var isle : seq) {
            t.button(IslandOverlayManager.getIslandName(isle), textb, () -> {
                setTarget(isle);
            }).with(funcSetTextb).with(b -> b.getLabel().setAlignment(Align.left));
            t.row();
        }
    }
}
