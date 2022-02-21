package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;

import static mindustry.Vars.*;

public class PowerGraphTable extends Table{
    public Team team;
    public float barsWidth = 400f;

    private Interval interval = new Interval();
    //P.G. set for ui generation, to keep P.G. order（让多电网的次序不会每次都变化）
    private Seq<PowerGraph> saved = new Seq<PowerGraph>();
    private Seq<MI2Bar> bars = new Seq<MI2Bar>();
    private float totalCap = 0f;

    public PowerGraphTable(float w){
        super();
        barsWidth = w;
        update(() -> {
            if(interval.get(0, 120)){
                rebuild();
            }
            
        });
    }

    public void rebuild(){
        clear();
        if(!state.isGame()) return;
        if(state.teams.get(player.team()).buildings == null) return;
        ObjectSet<PowerGraph> graphs = new ObjectSet<PowerGraph>();
        Seq<Building> builds = new Seq<Building>();
        state.teams.get(player.team()).buildings.getObjects(builds);
        totalCap = 0f;
        Prov<String> powertext;

        for(Building build : builds){
            if(build.block.hasPower){
                graphs.add(build.power.graph);
            }
        }

        for(PowerGraph p : graphs){
            if(!saved.contains(p, false)) saved.add(p);
            totalCap += p.getLastCapacity();
        }

        for(PowerGraph s : saved){
            if(!graphs.contains(s)) saved.remove(s);
        }
        
        int index = 0;
        for(PowerGraph p : saved){
            if(p.getLastCapacity() <= 0f) continue;
            powertext = () -> barsWidth * p.getLastCapacity() / totalCap <= 50f ? "" : "" + UI.formatAmount((long)p.getLastPowerStored()) + " " + (p.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(p.getPowerBalance() * 60));
            
            if(index >= bars.size){
                MI2Bar newBar = new MI2Bar();
                newBar.userObject = p;
                newBar.set(powertext, () -> p.getLastPowerStored() / p.getLastCapacity(), Pal.accent);
                newBar.blink(Color.white).outline(new Color(0.3f, 0.3f, 0.6f, 0.3f), 2f);
                newBar.addListener(new Tooltip(tt -> {
                    tt.label(() -> "" + UI.formatAmount((long)((PowerGraph)newBar.userObject).getLastPowerStored()) + "/" + UI.formatAmount((long)((PowerGraph)newBar.userObject).getLastCapacity()) + "  " + (((PowerGraph)newBar.userObject).getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(((PowerGraph)newBar.userObject).getPowerBalance() * 60)));
                    tt.row();
                    tt.table(ttt ->{
                        ttt.background(Styles.black6);
                        ttt.update(() -> {
                            ttt.clear();
                            ttt.add("Prod");
                            ttt.add("Cons");
                            ttt.row();
                            PowerGraph ppp = (PowerGraph)newBar.userObject;
                            ttt.table(tttp -> {
                                OrderedMap<Block, Float> blocks = new OrderedMap<Block, Float>();
                                OrderedMap<Block, Float> values = new OrderedMap<Block, Float>();
                                
                                for(Building producer : ppp.producers){
                                    blocks.put(producer.block, (blocks.containsKey(producer.block) ? blocks.get(producer.block):0f) + 1f);
                                    values.put(producer.block, (values.containsKey(producer.block) ? values.get(producer.block):0f) + producer.getPowerProduction() * producer.delta() * 60f);
                                }

                                int cols = (int)(blocks.size / (Core.scene.getHeight() / 36f));
                                int ir = 0;
                                
                                for(Block b : blocks.keys()){
                                    tttp.image(b.uiIcon).size(iconMed);
                                    tttp.add("x" + blocks.get(b).intValue()).labelAlign(Align.right).padRight(8f).fontScale(1f);
                                    tttp.add("+" + values.get(b).longValue()).labelAlign(Align.right).padRight(8f).fontScale(1f);
                                    if(ir++ < cols) continue;
                                    tttp.row();
                                    ir = 0;
                                }
                            }).top();

                            ttt.table(tttp -> {
                                OrderedMap<Block, Float> blocks = new OrderedMap<Block, Float>();
                                OrderedMap<Block, Float> values = new OrderedMap<Block, Float>();
                                
                                for(Building consumer : ppp.consumers){
                                    if(!consumer.block.consumes.hasPower()) continue;
                                    blocks.put(consumer.block, (blocks.containsKey(consumer.block) ? blocks.get(consumer.block):0f) + 1f);
                                    values.put(consumer.block, (values.containsKey(consumer.block) ? values.get(consumer.block):0f) + consumer.power.status * consumer.block.consumes.getPower().usage * 60 * consumer.timeScale());
                                }

                                int cols = (int)(blocks.size / (Core.scene.getHeight() / 48f));
                                int ir = 0;
                                
                                for(Block b : blocks.keys()){
                                    tttp.image(b.uiIcon).size(iconMed);
                                    tttp.add("x" + blocks.get(b).intValue()).labelAlign(Align.right).padRight(8f).fontScale(1f);
                                    tttp.add("-" + values.get(b).longValue()).labelAlign(Align.right).padRight(8f).fontScale(1f);
                                    if(ir++ < cols) continue;
                                    tttp.row();
                                    ir = 0;
                                }
                            }).top();

                        });
                    });
                }));
                bars.add(newBar);
            }else{
                bars.get(index).userObject = p;
                bars.get(index).set(powertext, () -> p.getLastPowerStored() / p.getLastCapacity(), Color.white);
                bars.get(index).setColor(Pal.accent);
                if(((PowerGraph)bars.get(index).userObject).getPowerBalance() < 0) bars.get(index).flash();
            }
            add(bars.get(index)).width(barsWidth * p.getLastCapacity() / totalCap).height(24f);
            index++;
        }
    }
}
