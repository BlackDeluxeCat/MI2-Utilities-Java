package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.struct.*;
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

    private static Interval interval = new Interval(2);
    private final Seq<PGInfo> saved = new Seq<>();
    private float totalCap = 0f;

    public PowerGraphTable(float w){
        super();
        barsWidth = w;
        update(() -> {
            saved.each(PGInfo::update);
            if(interval.get(0, 50f)){
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
        totalCap = 0f;

        OrderedSet<PowerGraph> graphs = new OrderedSet<>();
        state.teams.get(team).buildings.each(b -> {
            if(b.block.hasPower) graphs.add(b.power.graph);
        });

        //find and select presented graphs,
        //also select and renew remained graphs. So recorder data is probably get kept.
        //remove others.
        saved.removeAll(pi -> {
            final PowerGraph[] used = new PowerGraph[1];
            boolean remove = !graphs.orderedItems().contains(pgn -> {
                if(pgn == pi.pg || (!pgn.batteries.isEmpty() && pi.pg.batteries.contains(pgn.batteries.find(Building::isValid)))){
                    pi.renew(pgn);
                    used[0] = pgn;
                    return true;
                }
                return false;
            });
            if(used[0] != null) graphs.remove(used[0]);
            return remove;
        });

        graphs.orderedItems().each(p -> {
            if(!saved.contains(pp -> pp.pg == p)) saved.add(new PGInfo(p));
        });

        saved.each(c -> totalCap += c.totalcap);
        
        int index = 0;
        for(var info : saved){
            if(info.totalcap <= 0f) continue;
            info.updateG();
            if(index >= bars.size) bars.add(new MI2Bar());

            MI2Bar bar = bars.get(index);
            bar.set(() -> barsWidth * info.totalcap / totalCap <= 50f ? "" : "" + UI.formatAmount((long)info.pg.getLastPowerStored()) + " " + (info.pg.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(info.pg.getPowerBalance() * 60)), () -> info.pg.getLastPowerStored() / info.totalcap, Pal.accent);
            bar.blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 2f);

            bar.clearListeners();
            bar.clicked(() -> {
                if(control.input instanceof InputOverwrite iow) iow.pan(true, MI2UTmp.v1.set(info.pg.all.random()));
            });
            bar.hovered(() -> showDetailFor(info, bar));

            add(bars.get(index)).width(barsWidth * info.totalcap / totalCap).height(24f);
            index++;
        }
    }

    public void showDetailFor(PGInfo info, MI2Bar bar){
        detailTable.shown = false;
        detailTable.popupDuration = 0.05f;
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
        float ew = 150f;
        table.add(new Element(){
            @Override
            public void draw(){
                super.draw();
                Floatc4 buildText = (min, max, w, color) -> {
                    Font font = Fonts.outline;
                    font.setColor(MI2UTmp.c1.argb8888((int)color));
                    GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

                    String text = UI.formatAmount((long)min);
                    lay.setText(font, text);
                    font.getCache().clear();
                    font.getCache().addText(text, this.x + w, this.y + lay.height);
                    font.getCache().draw(parentAlpha);

                    text = UI.formatAmount((long)max);
                    lay.setText(font, text);
                    font.getCache().clear();
                    font.getCache().addText(text, this.x + w, this.y + this.getHeight());
                    font.getCache().draw(parentAlpha);

                    Pools.free(lay);
                };
                Draw.reset();

                Lines.stroke(4f);
                Draw.color(Pal.power, 0.75f);
                info.stG.defaultDraw(x, y + 20f, width, height - 40f, false);
                buildText.get(info.stG.min(), info.stG.max(), 100f, Pal.power.argb8888());

                float min = Math.min(info.consG.min(), info.genG.min()), max = Math.max(info.consG.max(), info.genG.max());

                Lines.stroke(2f);
                Draw.color(Color.scarlet);
                info.consG.defaultDraw(x, y + 40f, width, height - 80f, true, min, max);
                Draw.color(Color.green);
                info.genG.defaultDraw(x, y + 40f, width, height - 80f, true, min, max);
                buildText.get(min, max, 0f, Color.gray.argb8888());

                Draw.reset();
            };
        }).growX().height(200f).pad(4f);

        table.row();

        table.table(t -> {
            interval.reset(1, 50f);
            t.update(() -> {
                if(!interval.get(1, 15f)) return;
                t.clearChildren();
                t.defaults().left().top().pad(1f);
                t.table(gas  -> {
                    gas.table(tt -> {
                        tt.defaults().left().width(ew);
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
                    }).padBottom(20f);

                    gas.row();

                    gas.table(tt -> {
                        tt.defaults().left().width(ew);
                        tt.labelWrap(() -> Iconc.blockBattery + "(" + Strings.fixed(100*info.pg.getBatteryStored()/info.totalcap, 1) + "%)\n" + (int)info.pg.getBatteryStored()).colspan(2).padBottom(5f).color(Pal.accent);
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

                t.table(tt -> {
                    tt.defaults().left().width(ew);
                    tt.label(() -> Iconc.blockSiliconSmelter + "-" + Strings.fixed(info.totalcons, 1)).colspan(2).padBottom(5f).color(Color.scarlet);
                    int used = 0;
                    for(int m = 0; m < info.blocks.length; m++){
                        if(!Mathf.zero(info.bcons[m], 0.001f)) used++;
                    }

                    int columns = Mathf.ceil(used / (Mathf.maxZero(Core.graphics.getHeight() - 400f) + 1f) * 16f) * 2;

                    for(int m = 0; m < info.blocks.length; m++){
                        int i = m;
                        if(!Mathf.zero(info.bcons[i], 0.001f)){
                            if(Mathf.mod((tt.getCells().size - 1), columns) == 0) tt.row();
                            tt.add(getBlockImage(i, 1, () -> new Image(content.block(i).uiIcon))).size(16f).pad(2f);
                            var l = getBlockImage(i, 4, () -> new Label(""));
                            ((Label)l).setText(() -> "x" + info.blocks[i] + " [gray]-" + Strings.autoFixed(info.bcons[i], 1));
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
        public FloatDataRecorder consG, genG, stG;
        public float totalcons, totalgen, totalcap;
        public float[] bcons = new float[blocksI.length], bgen = new float[blocksI.length], bstore = new float[blocksI.length];
        public int[] blocks = new int[blocksI.length];

        public PGInfo(PowerGraph pg){
            consG = new FloatDataRecorder(90);
            genG = new FloatDataRecorder(90);
            stG = new FloatDataRecorder(90);
            consG.getter = () -> totalcons;
            genG.getter = () -> totalgen;
            renew(pg);
        }

        public void renew(PowerGraph pg){
            this.pg = pg;
            update();
            stG.getter = pg::getBatteryStored;
        }

        public boolean vaild(){
            return pg != null;
        }

        public void update(){
            if(!vaild()) return;
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

        public void updateG(){
            consG.update();
            genG.update();
            stG.update();
        }
    }
}
