package mi2u.ui.stats;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.blocks.logic.*;

import static mi2u.ui.MonitorCanvas.unitSize;

public class ProcessorMonitor extends BuildingMonitor{
    public transient @Nullable LExecutor exec;
    public transient int varsHash;
    public transient boolean shouldRebuild;
    public transient Interval timer = new Interval();
    public transient int reflushCount = 30;
    public transient ObjectSet<String> starred = new ObjectSet<>();
    public transient Seq<Runnable> updaters = new Seq<>();

    public ProcessorMonitor(){
        super();
        w = 10 * unitSize;
        h = 10 * unitSize;
    }

    @Override
    public void build(Table table){
        table.clear();
        table.pane(t -> {
            t.update(() -> {
                validate();
                if(shouldRebuild){
                    build(table);
                    shouldRebuild = false;
                }
                if(timer.get(reflushCount)){
                    updaters.each(Runnable::run);
                }
            });

            updaters.clear();

            if(exec == null || exec.build == null) return;

            var seq = new Seq<>(exec.vars);

            Cons<LVar> varBuilder = var -> {
                t.labelWrap(() -> (starred.contains(var.name) ? "[accent]" : "") + var.name).width(Mathf.clamp(w / 3f, unitSize, unitSize * 4)).get().clicked(() -> {
                    if(starred.contains(var.name)){
                        starred.remove(var.name);
                    }else{
                        starred.add(var.name);
                    }
                    shouldRebuild = true;
                });
                t.add("").with(l -> {
                    updaters.add(() -> {
                        var str = var.isobj ? LExecutor.PrintI.toString(var.objval) : Math.abs(var.numval - (long)var.numval) < 0.00001 ? (long)var.numval + "" : var.numval + "";
                        l.setColor(Color.white);
                        if(!l.textEquals(str)){
                            l.setText(str);
                            l.setColor(Color.acid);
                        }
                    });
                }).growX();
                t.row();
            };

            seq.each(lvar -> starred.contains(lvar.name), varBuilder);
            seq.each(lvar -> !starred.contains(lvar.name), varBuilder);
        }).grow();
    }

    @Override
    public void buildCfg(Table table){
        table.clear();
        table.add("" + Iconc.refresh).size(unitSize).row();
        table.field(String.valueOf(30), s -> reflushCount = Math.max(Strings.parseInt(s, 30), 1)).growX();
    }

    @Override
    public void reflush(){
        super.reflush();
        shouldRebuild = true;
        starred.clear();
    }

    @Override
    public void validate(){
        super.validate();
        exec = b instanceof LogicBlock.LogicBuild lb ? lb.executor : null;
        int hash = exec == null ? 0 : exec.hashCode();
        if(hash != varsHash){
            varsHash = hash;
            reflush();
        }
    }
}
