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
import mi2u.ui.elements.*;
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
    public PopupTable detailTable = new PopupTable();
    public final AlluvialDiagram diagram = new AlluvialDiagram();
    public boolean powerIOBars;

    //n x 6 Element array with icons and labels from generator, consumer, storage.
    static Element[][] blocksI = new Element[content.blocks().size][6];

    private static Interval interval = new Interval(2);
    private final Seq<PGInfo> pgInfos = new Seq<>();
    private float totalCap = 0f, totalCons = 0f, totalGen = 0f;

    public PowerGraphTable(){
        super();
        detailTable.touchable = Touchable.disabled;
    }

    @Override
    public void act(float delta){
        super.act(delta);
        pgInfos.each(PGInfo::update);
        if(interval.get(0, 60f)){
            pgInfos.each(PGInfo::updateG);
            rebuild();
        }
    }

    public void rebuild(){
        clear();
        if(!state.isGame()) return;
        if(team == null) return;
        if(state.teams.get(team).buildings == null) return;
        totalCap = 0f;
        totalCons = 0f;
        totalGen = 0f;

        OrderedSet<PowerGraph> graphs = new OrderedSet<>();
        state.teams.get(team).buildings.each(b -> {
            if(b.block.hasPower) graphs.add(b.power.graph);
        });

        //find and select presented graphs,
        //also select and renew remained graphs. So recorder data is probably get kept.
        //remove others.
        pgInfos.removeAll(pi -> {
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
            if(remove){
                diagram.removeChild(pi.barGen);
                diagram.removeChild(pi.barStore);
                diagram.removeChild(pi.barCons);
                Pools.free(pi);
            }
            return remove;
        });

        graphs.orderedItems().each(p -> {
            if(!pgInfos.contains(pp -> pp.pg == p)) pgInfos.add(Pools.obtain(PGInfo.class, PGInfo::new).renew(p));
        });

        pgInfos.each(c -> {
            totalCap += c.totalcap;
            totalGen += c.totalgen;
            totalCons += c.totalcons;
        });
        setBackground(Styles.black5);
        add(diagram).growX();
    }

    public void showDetailFor(PGInfo info, MI2Bar bar){
        detailTable.shown = false;
        detailTable.popupDuration = 0.05f;
        detailTable.background(Styles.black6);
        detailTable.clearChildren();
        buildInfo(detailTable, info);

        detailTable.update(() -> {
            if(info.pg == null) detailTable.clear();
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

    public class AlluvialDiagram extends WidgetGroup{
        MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis();
        public AlluvialDiagram(){
            setFillParent(true);
            setClip(true);
        }

        @Override
        public float getPrefHeight(){
            return powerIOBars ? 160f : totalCap < 1f ? 0f : 32f;
        }

        @Override
        public void draw(){
            if(!state.isGame()){
                clear();
                return;
            }

            //三行放条，设置位置
            //IO条在简洁版会被移除
            if(timer.get(400)){
                float h = powerIOBars ? 24f : 32f, yc = 0f, yg = (getHeight() - h) / 2.5f, ys = getHeight() - h;
                float x1 = 0f, x2 = 0f, x3 = 0f;
                float maxIO = Math.max(totalGen, totalCons);
                for(var info : pgInfos){
                    addChild(info.barStore);
                    info.barStore.setSize(totalCap < 10f ? 0f : width * info.ltotalcap / totalCap, h);
                    info.barStore.x = x2;
                    x2 += info.barStore.getWidth();
                    info.barStore.y = ys;

                    if(!powerIOBars) continue;
                    addChild(info.barGen);
                    info.barGen.setSize(maxIO < 0.1f ? 0f : (width * info.ltotalgen / maxIO), h);
                    info.barGen.x = x1;
                    x1 += info.barGen.getWidth();
                    info.barGen.y = yg;

                    addChild(info.barCons);
                    info.barCons.setSize(maxIO < 0.1f ? 0f : (width * info.ltotalcons / maxIO), h);
                    info.barCons.x = x3;
                    x3 += info.barCons.getWidth();
                    info.barCons.y = yc;
                }
            }

            float dist = getHeight() / 6f;
            //对应条之间画曲线
            if(powerIOBars){
                for(var info : pgInfos){
                    float fromx, fromy, tox, toy;
                    if(info.totalcap < 10f && (info.barCons.getWidth() >= 1f && info.barGen.getWidth() >= 1f)){
                        Draw.color(info.ltotalcons > info.ltotalgen ? Color.scarlet : Color.green, 0.5f);
                        Lines.stroke(2f);
                        fromx = info.barGen.getX(Align.center);
                        fromy = info.barGen.getY(Align.center);
                        tox = info.barCons.getX(Align.center);
                        toy = info.barCons.getY(Align.center);
                        Lines.curve(fromx, fromy, fromx, fromy - dist, tox, toy + dist, tox, toy, 8);
                    }

                    if(info.barStore.getWidth() >= 1f){
                        if(info.ltotalgen > info.ltotalcons){
                            Draw.color(Pal.accent, 0.5f);
                            fillBetweenBars(info.barGen, 0f, info.ltotalcons/info.ltotalgen, info.barCons, 0f, 1f, dist);
                            fillBetweenBars(info.barStore, 0f, 1f, info.barGen, info.ltotalcons/info.ltotalgen, 1f, dist);
                        }
                        if(info.ltotalgen < info.ltotalcons){
                            Draw.color(Pal.accent, 0.5f);
                            fillBetweenBars(info.barGen, 0f, 1f, info.barCons, 0f, info.ltotalgen/info.ltotalcons, dist);
                            Draw.color(Color.scarlet, 0.5f);
                            fillBetweenBars(info.barStore, 0f, 1f, info.barCons, info.ltotalgen/info.ltotalcons, 1f, dist);
                        }
                    }
                }
            }

            //画条
            super.draw();
        }

        public void fillBetweenBars(MI2Bar barUp, float upl, float upr, MI2Bar barDown, float downl, float downr, float dist){
            fillCurve(barUp.x + barUp.getWidth() * upl, barUp.getY(Align.bottom), barUp.x + barUp.getWidth() * upr, barUp.getY(Align.bottom), barDown.x + barDown.getWidth() * downl, barDown.getY(Align.top), barDown.x + barDown.getWidth() * downr, barDown.getY(Align.top), 8, dist);
        }

        /**   ---------
         *    |       \
         *   |         \
         *  /            \
         * ----------------
         * */
        public static void fillCurve(float fx1, float fy1, float fx2, float fy2, float tx1, float ty1, float tx2, float ty2, float segments, float dist){
            float cfx1 = fx1, ctx1 = tx1, cfy1 = fy1 - dist, cty1 = ty1 + dist;
            float cfx2 = fx2, ctx2 = tx2, cfy2 = fy2 - dist, cty2 = ty2 + dist;

            float subdiv_step = 1f / segments;
            float subdiv_step2 = subdiv_step * subdiv_step;
            float subdiv_step3 = subdiv_step * subdiv_step * subdiv_step;

            float pre1 = 3 * subdiv_step;
            float pre2 = 3 * subdiv_step2;
            float pre4 = 6 * subdiv_step2;
            float pre5 = 6 * subdiv_step3;

            float tmp1x1 = fx1 - cfx1 * 2 + ctx1;
            float tmp1y1 = fy1 - cfy1 * 2 + cty1;
            float tmp1x2 = fx2 - cfx2 * 2 + ctx2;
            float tmp1y2 = fy2 - cfy2 * 2 + cty2;

            float tmp2x1 = (cfx1 - ctx1) * 3 - fx1 + tx1;
            float tmp2y1 = (cfy1 - cty1) * 3 - fy1 + ty1;
            float tmp2x2 = (cfx2 - ctx2) * 3 - fx2 + tx2;
            float tmp2y2 = (cfy2 - cty2) * 3 - fy2 + ty2;

            float f1x = fx1;
            float f1y = fy1;
            float f2x = fx2;
            float f2y = fy2;

            float dfx1 = (cfx1 - fx1) * pre1 + tmp1x1 * pre2 + tmp2x1 * subdiv_step3;
            float dfy1 = (cfy1 - fy1) * pre1 + tmp1y1 * pre2 + tmp2y1 * subdiv_step3;
            float dfx2 = (cfx2 - fx2) * pre1 + tmp1x2 * pre2 + tmp2x2 * subdiv_step3;
            float dfy2 = (cfy2 - fy2) * pre1 + tmp1y2 * pre2 + tmp2y2 * subdiv_step3;

            float ddfx1 = tmp1x1 * pre4 + tmp2x1 * pre5;
            float ddfy1 = tmp1y1 * pre4 + tmp2y1 * pre5;
            float ddfx2 = tmp1x2 * pre4 + tmp2x2 * pre5;
            float ddfy2 = tmp1y2 * pre4 + tmp2y2 * pre5;

            float dddfx1 = tmp2x1 * pre5;
            float dddfy1 = tmp2y1 * pre5;
            float dddfx2 = tmp2x2 * pre5;
            float dddfy2 = tmp2y2 * pre5;

            float l1x, l1y, l2x, l2y;
            while(segments-- > 0){
                l1x = f1x;
                l1y = f1y;
                l2x = f2x;
                l2y = f2y;

                f1x += dfx1;
                f1y += dfy1;
                dfx1 += ddfx1;
                dfy1 += ddfy1;
                ddfx1 += dddfx1;
                ddfy1 += dddfy1;

                f2x += dfx2;
                f2y += dfy2;
                dfx2 += ddfx2;
                dfy2 += ddfy2;
                ddfx2 += dddfx2;
                ddfy2 += dddfy2;
                Fill.polyBegin();
                Fill.polyPoint(l1x, l1y);
                Fill.polyPoint(l2x, l2y);
                Fill.polyPoint(f2x, f2y);
                Fill.polyPoint(f1x, f1y);
                Fill.polyEnd();
            }
        }
    }

    public class PGInfo implements Pool.Poolable{
        public PowerGraph pg;
        public FloatDataRecorder consG, genG, stG;
        public float totalcons, totalgen, totalcap;
        public float ltotalcons, ltotalgen, ltotalcap;
        public float[] bcons = new float[blocksI.length], bgen = new float[blocksI.length], bstore = new float[blocksI.length];
        public int[] blocks = new int[blocksI.length];

        MI2Bar barStore;
        MI2Bar barGen;
        MI2Bar barCons;

        public PGInfo(){
            consG = new FloatDataRecorder(90);
            genG = new FloatDataRecorder(90);
            stG = new FloatDataRecorder(90);
            consG.getter = () -> totalcons;
            genG.getter = () -> totalgen;

            Runnable clicked = () -> {
                if(control.input instanceof InputOverwrite iow) iow.pan(true, MI2UTmp.v1.set(pg.all.random()));
            };

            barStore = new MI2Bar().blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 2f);
            barStore.clicked(clicked);
            barStore.hovered(() -> showDetailFor(this, barStore));

            barGen = new MI2Bar().blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 2f);
            barGen.set(() -> barGen.getWidth() <= 50f ? "" : "+" + UI.formatAmount((long)totalgen), () -> 1f, Pal.items);
            barGen.clicked(clicked);
            barGen.hovered(() -> showDetailFor(this, barGen));

            barCons = new MI2Bar().blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 2f);
            barCons.set(() -> barCons.getWidth() <= 50f ? "" : "-" + UI.formatAmount((long)totalcons), () -> 1f, Pal.health);
            barCons.clicked(clicked);
            barCons.hovered(() -> showDetailFor(this, barCons));
        }

        public PGInfo renew(PowerGraph pg){
            this.pg = pg;

            barStore.set(() -> barStore.getWidth() <= 50f ? "" : UI.formatAmount((long)pg.getLastPowerStored()) + (barStore.getWidth() <= 100f ? "" : (" " + (pg.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(pg.getPowerBalance() * 60)))), () -> pg.getLastPowerStored() / totalcap, Pal.accent);

            update();
            stG.getter = pg::getBatteryStored;
            return this;
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
            ltotalcap = Mathf.lerp(ltotalcap, totalcap, 0.9f);
            ltotalgen = Mathf.lerp(ltotalgen, totalgen, 0.9f);
            ltotalcons = Mathf.lerp(ltotalcons, totalcons, 0.9f);
        }

        public void updateG(){
            consG.update();
            genG.update();
            stG.update();
        }

        @Override
        public void reset(){
            pg = null;
            Arrays.fill(blocks, 0);
            Arrays.fill(bcons, 0);
            Arrays.fill(bgen, 0);
            Arrays.fill(bstore, 0);
            totalcons = totalcap = totalgen = 0;
            genG.reset();
            genG.resize(90);
            consG.reset();
            consG.resize(90);
            stG.reset();
            stG.resize(90);
        }
    }
}
