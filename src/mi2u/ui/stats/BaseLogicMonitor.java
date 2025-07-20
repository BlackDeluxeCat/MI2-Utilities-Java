package mi2u.ui.stats;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.ui.elements.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.blocks.logic.*;

import java.util.*;

import static mi2u.ui.MonitorCanvas.unitSize;

public class BaseLogicMonitor extends BuildingMonitor{
    public transient Interval timer = new Interval();
    public transient int updateCount = 30;
    public transient boolean shouldRebuild;

    @Override
    public void buildCfg(Table table){
        table.clear();
        table.add("" + Iconc.refresh).size(unitSize);
        table.field(String.valueOf(30), s -> updateCount = Math.max(Strings.parseInt(s, 30), 1)).growX();
    }

    public static class ProcessorMonitor extends BaseLogicMonitor{
        public transient @Nullable LExecutor exec;
        public transient int varsHash;
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
                    if(timer.get(updateCount)){
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
                            String str = var.isobj ? LExecutor.PrintI.toString(var.objval) : Math.abs(var.numval - (long)var.numval) < 0.00001 ? (long)var.numval + "" : var.numval + "";
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

    public static class MemoryMonitor extends BaseLogicMonitor{
        public transient double[] memory;
        public transient Seq<Runnable> updaters = new Seq<>();

        public MemoryMonitor(){
            super();
            w = 8 * unitSize;
            h = 8 * unitSize;
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
                    if(timer.get(updateCount)){
                        updaters.each(Runnable::run);
                    }
                });

                if(b instanceof MemoryBlock.MemoryBuild mb){
                    for(int i = 0; i < mb.memory.length; i++){
                        final int id = i;
                        if(i % Math.max(Mathf.floor(w / 2 / unitSize), 1) == 0) t.row();
                        t.add(new CombinationIcon(c -> c.add("").grow().with(l -> {
                            updaters.add(() -> {
                                String num = Strings.autoFixed((float)mb.memory[id], 3);
                                l.setColor(Color.white);
                                if(!l.textEquals(num)){
                                    l.setText(num);
                                    l.setColor(Color.acid);
                                }
                            });
                        })).bottomRight(c -> c.add(String.valueOf(id)).color(Pal.accent).fontScale(0.6f))).size(unitSize * 2, unitSize);
                    }
                }
            }).grow().with(p -> p.setFadeScrollBars(true));
        }

        @Override
        public void validate(){
            super.validate();
            var mb = b instanceof MemoryBlock.MemoryBuild tm ? tm : null;
            if(memory == null && mb != null){
                memory = mb.memory;
                shouldRebuild = true;
            }

            if(memory != null){
                if(mb == null){
                    memory = null;
                    shouldRebuild = true;
                }else if(memory.length != mb.memory.length){
                    memory = mb.memory;
                    shouldRebuild = true;
                }
            }
        }
    }
}