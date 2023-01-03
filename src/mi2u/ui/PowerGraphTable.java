package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.*;

import java.util.*;

import static mindustry.Vars.*;

public class PowerGraphTable extends Table{
    public Team team;
    public float barsWidth;
    public PopupTable detailTable = new PopupTable();
    private Seq<MI2Bar> bars = new Seq<>();
    static Element[][] blocksI = new Element[content.blocks().size][6];

    private static MI2Utils.IntervalMillis interval = new MI2Utils.IntervalMillis(2);
    //P.G. set for ui generation, to keep P.G. order（让多电网的次序不会每次都变化）
    private Seq<PGInfo> saved = new Seq<>();
    private float totalCap = 0f;

    public PowerGraphTable(float w){
        super();
        barsWidth = w;
        update(() -> {
            saved.each(PGInfo::update);
            if(interval.get(0, 800)){
                rebuild();
            }
        });
        detailTable.touchable = Touchable.disabled;
    }

    public void rebuild(){
        clear();
        if(!state.isGame()) return;
        if(team == null) return;
        if(state.teams.get(team).buildings == null) return;
        ObjectSet<PowerGraph> graphs = new ObjectSet<>();
        Seq<Building> builds = new Seq<>(state.teams.get(team).buildings);
        totalCap = 0f;

        for(Building build : builds){
            if(build.block.hasPower){
                graphs.add(build.power.graph);
            }
        }

        for(PowerGraph p : graphs){
            if(!saved.contains(pp -> pp.pg.getID() == p.getID())) saved.add(new PGInfo(p));
            totalCap += p.getLastCapacity();
        }

        saved.removeAll(s -> !graphs.contains(s.pg));
        
        int index = 0;
        for(var info : saved){
            if(info.pg.getLastCapacity() <= 0f) continue;
            info.update();
            if(index >= bars.size) bars.add(new MI2Bar());

            MI2Bar bar = bars.get(index);
            bar.set(() -> barsWidth * info.pg.getLastCapacity() / totalCap <= 50f ? "" : "" + UI.formatAmount((long)info.pg.getLastPowerStored()) + " " + (info.pg.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(info.pg.getPowerBalance() * 60)), () -> info.pg.getLastPowerStored() / info.pg.getLastCapacity(), Pal.accent);
            bar.blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 2f);

            bar.clicked(() -> {
                if(control.input instanceof InputOverwrite iow) iow.pan(true, MI2UTmp.v1.set(info.pg.all.random()));
            });
            bar.hovered(() -> showDetailFor(info, bar));

            add(bars.get(index)).width(barsWidth * info.pg.getLastCapacity() / totalCap).height(24f);
            index++;
        }
    }

    public void showDetailFor(PGInfo info, MI2Bar bar){
        detailTable.shown = false;
        detailTable.background(Styles.black6);
        detailTable.clearChildren();
        buildInfo(detailTable, info);

        detailTable.update(() -> {
            detailTable.hideWithoutFocusOn(bar);
            detailTable.setPositionInScreen(Core.input.mouseX() - 20f, Core.input.mouseY() - detailTable.getPrefHeight() - 20f);
        });
        detailTable.popup();
        detailTable.toFront();
    }

    public static void buildInfo(Table table, PGInfo info){
        table.table(t -> {
            t.update(() -> {
                if(!interval.get(1, 200)) return;
                t.clearChildren();
                t.defaults().left().top().pad(1f);
                t.table(tt -> {
                    tt.defaults().left().width(150f);
                    tt.label(() -> Iconc.blockSolarPanel + "+" + Strings.fixed(info.totalgen, 1)).colspan(2).padBottom(5f).color(Color.green);
                    for(int m = 0; m < info.blocks.length; m++){
                        int i = m;
                        if(!Mathf.zero(info.bgen[i], 0.001f)){
                            tt.row();
                            tt.add(getBlockImage(i, 0, () -> new Image(content.block(i).uiIcon))).size(16f).pad(2f);
                            var l = getBlockImage(i, 3, () -> new Label(""));
                            ((Label)l).setText(() -> "x" + info.blocks[i] + " [gray]+" + Strings.autoFixed(info.bgen[i], 1));
                            tt.add(l);
                        }
                    }
                });

                t.table(tt -> {
                    tt.defaults().left().width(150f);
                    tt.label(() -> Iconc.blockSiliconSmelter + "-" + Strings.fixed(info.totalcons, 1)).colspan(2).padBottom(5f).color(Color.scarlet);
                    for(int m = 0; m < info.blocks.length; m++){
                        int i = m;
                        if(!Mathf.zero(info.bcons[i], 0.001f)){
                            tt.row();
                            tt.add(getBlockImage(i, 1, () -> new Image(content.block(i).uiIcon))).size(16f).pad(2f);
                            var l = getBlockImage(i, 4, () -> new Label(""));
                            ((Label)l).setText(() -> "x" + info.blocks[i] + " [gray]-" + Strings.autoFixed(info.bcons[i], 1));
                            tt.add(l);
                        }
                    }
                });

                t.table(tt -> {
                    tt.defaults().left().width(150f);
                    tt.label(() -> Iconc.blockBattery + "(" + Strings.fixed(100*info.pg.getBatteryStored()/info.totalcap, 1) + "%)" + (int)info.pg.getBatteryStored()).colspan(2).padBottom(5f).color(Pal.accent);
                    for(int m = 0; m < info.blocks.length; m++){
                        int i = m;
                        if(!Mathf.zero(info.bstore[i], 0.001f)){
                            tt.row();
                            tt.add(getBlockImage(i, 2, () -> new Image(content.block(i).uiIcon))).size(16f).pad(2f);
                            var l = getBlockImage(i, 5, () -> new Label(""));
                            ((Label)l).setText(() -> "x" + info.blocks[i] + "  [gray]" + Strings.autoFixed(info.bstore[i], 1));
                            tt.add(l);
                        }
                    }
                });


            });
        });
    }

    public static Element getBlockImage(int id, int i3, Prov<Element> getter){
        return blocksI[id][i3] != null ? blocksI[id][i3] : (blocksI[id][i3] = getter.get());
    }

    public static class PGInfo{
        public PowerGraph pg;
        public float totalcons, totalgen, totalcap;
        public float[] bcons = new float[blocksI.length], bgen = new float[blocksI.length], bstore = new float[blocksI.length];
        public int[] blocks = new int[blocksI.length];

        public PGInfo(PowerGraph pg){
            this.pg = pg;
        }

        public boolean vaild(){
            return pg != null;
        }

        public void update(){
            if(!vaild()){
                pg = null; return;
            }
            Arrays.fill(blocks, 0);
            Arrays.fill(bcons, 0);
            Arrays.fill(bgen, 0);
            Arrays.fill(bstore, 0);
            totalcons = totalcap = totalgen = 0;

            pg.all.each(b -> blocks[b.block.id] += 1);
            pg.batteries.each(b -> bstore[b.block.id] += b.block.consPower.capacity);
            pg.consumers.each(b -> bcons[b.block.id] += Mathf.zero(b.block.consPower.requestedPower(b)) ? 0f : b.block.consPower.usage * (b.shouldConsume() ? b.efficiency() * b.timeScale() : 0f) * 60f);
            pg.producers.each(b -> bgen[b.block.id] += b.getPowerProduction() * 60f * b.efficiency() * b.timeScale());

            for(int i = 0; i < blocks.length; i++){
                totalcap += bstore[i];
                totalcons += bcons[i];
                totalgen += bgen[i];
            }
        }
    }
}
