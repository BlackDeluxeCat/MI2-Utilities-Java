package mi2u.ui.stats;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.struct.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;
import static mi2u.ui.MonitorCanvas.unitSize;
import static mindustry.Vars.content;

public class ItemChartBM extends BuildingMonitor{
    public final transient FloatDataRecorder[] recorders = new FloatDataRecorder[content.items().size];
    public boolean[] cfg = new boolean[content.items().size];
    public boolean label = true, chart = true;
    public ItemChartBM(){
        super();
        w = 6 * unitSize;
        h = 4 * unitSize;
    }

    @Override
    public void build(Table table){
        table.clear();
        table.add(new Element(){
            @Override
            public void draw(){
                super.draw();

                float dy = y, dx = x;
                int used = 0;
                for(int i = 0; i < content.items().size; i++){
                    var rec = recorders[i];
                    if(!cfg[i] || rec == null) continue;

                    if(chart){
                        Draw.color(content.item(i).color);
                        Lines.stroke(2f);
                        rec.defaultDraw(x, y, getWidth(), getHeight(), true);
                    }

                    if(label){
                        Draw.reset();
                        var icon = content.item(i).uiIcon;
                        Draw.rect(icon, dx + unitSize / 2f, dy + unitSize / 2f, unitSize, unitSize * icon.height / icon.width);
                        Font font = Fonts.outline;
                        String text = UI.formatAmount((long)rec.get(0));
                        font.getColor().set(content.item(i).color).a(0.6f);
                        font.getCache().clear();
                        font.getCache().addText(text, dx + unitSize, dy + unitSize, 0, Align.left, false);
                        font.getCache().draw(parentAlpha);
                        dy += unitSize;
                        if(Mathf.mod(used, h / unitSize) == h / unitSize - 1){
                            dx += unitSize * 4f;
                            dy = y;
                        }
                    }
                    used++;
                }
                Draw.reset();
            }
        }).grow();
    }

    @Override
    public void buildCfg(Table table){
        table.clear();
        table.table(t -> {
            t.defaults().height(unitSize).growX();
            t.button("" + Iconc.chartBar, textbtoggle, () -> {
                chart = !chart;
            }).checked(chart);
            t.button("" + Iconc.list, textbtoggle, () -> {
                label = !label;
            }).checked(label);
        }).growX();
        table.row();
        table.pane(t -> {
            for(var item : content.items()){
                int id = item.id;
                if(id % Math.max(w / unitSize - 1, 1) == 0) t.row();
                t.button(b -> b.image(item.uiIcon).scaling(Scaling.fit), textbtoggle, () -> {
                    cfg[id] = !cfg[id];
                }).checked(tb -> cfg[id]).size(unitSize).with(tb -> MI2Utils.tooltip(tb, content.item(id).localizedName));
            }
        }).growX();
    }

    @Override
    public void update(){
        validate();
        for(int i = 0; i < content.items().size; i++){
            if(cfg[i]) recorders[i].update();
        }
    }

    @Override
    public void validate(){
        super.validate();
        if(b == null || !b.block.hasItems || b.items == null) return;
        for(int i = 0; i < content.items().size; i++){
            if(recorders[i] == null && cfg[i]){
                var r = recorders[i] = new FloatDataRecorder(600);
                r.disableF = fdr -> b == null;
                int id = i;
                r.getter = () -> b.items.get(id);
            }
        }
    }

    @Override
    public void reflush(){
        for(var r : recorders){
            if(r != null) r.reset();
        }
    }
}
