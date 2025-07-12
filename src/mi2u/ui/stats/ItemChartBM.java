package mi2u.ui.stats;

import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import mi2u.struct.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.content;

public class ItemChartBM extends BuildingMonitor{
    public final transient FloatDataRecorder[] recorders = new FloatDataRecorder[content.items().size];
    public boolean[] cfg = new boolean[content.items().size];
    public ItemChartBM(){
        super();
        w = 5;
        h = 4;
    }

    @Override
    public void build(Table table){
        table.add(new Element(){
            @Override
            public void draw(){
                super.draw();
                for(int i = 0; i < content.items().size; i++){
                    if(!cfg[i] || recorders[i] == null) continue;
                    Draw.color(content.item(i).color);
                    Lines.stroke(2f);
                    recorders[i].defaultDraw(x, y, getWidth(), getHeight(), true, 0, b.block.itemCapacity);
                }
                Draw.reset();
            }
        }).grow();
    }

    @Override
    public void buildCfg(Table table){
        super.buildCfg(table);
        table.pane(t -> {
            for(int i = 0; i < content.items().size; i++){
                int id = i;
                t.button(content.item(i).localizedName, textbtoggle, () -> cfg[id] = !cfg[id]).checked(tb -> cfg[id]).height(buttonSize).with(funcSetTextb);
            }
        }).with(p -> p.setForceScroll(true, false)).growX();
        table.row();

    }

    @Override
    public void update(){
        super.update();
        for(int i = 0; i < content.items().size; i++){
            if(recorders[i] == null){
                if(cfg[i]){
                    var r = recorders[i] = new FloatDataRecorder(600);
                    r.disableF = fdr -> b == null;
                    int id = i;
                    r.getter = () -> b.items.get(id);
                }
            }else{
                if(cfg[i]) recorders[i].update();
            }
        }
    }

    @Override
    public void reset(){
        for(var r : recorders){
            if(r != null) r.reset();
        }
    }
}
