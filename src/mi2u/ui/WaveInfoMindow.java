package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.struct.*;
import mi2u.ui.elements.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mi2u.MI2UVars.*;
import static mi2u.struct.UnitsData.*;
import static mindustry.Vars.*;

public class WaveInfoMindow extends Mindow2{
    Table barsTable;
    private final MI2Utils.IntervalMillis waveUpd = new MI2Utils.IntervalMillis();
    private boolean syncCurWave = true;
    /** start from 0 */
    public static int curWave = 0, curSpawn = -1;

    protected Seq<WaveBar> hpBars = new Seq<>(), pool = new Seq<>(), tmp = new Seq<>();

    public WaveInfoMindow(){
        super("WaveInfo", true);
        hasCloseButton = true;
        setVisibleInGame();

        titlePane.button("≪", textb, () -> {
            syncCurWave = false;
            curWave = Math.max(curWave - 10, 0);
        }).with(funcSetTextb).size(buttonSize);
        titlePane.button("<", textb, () -> {
            syncCurWave = false;
            curWave = Math.max(curWave - 1, 0);
        }).with(funcSetTextb).size(buttonSize);
        titlePane.button("O", textbtoggle, () -> {
            syncCurWave = !syncCurWave;
        }).with(funcSetTextb).size(buttonSize).update(b -> {
            b.setChecked(syncCurWave);
            if(syncCurWave) curWave = Math.max(state.wave - 1, 0);
        });
        titlePane.button(">", textb, () -> {
            syncCurWave = false;
            curWave = Math.max(curWave + 1, 0);
        }).with(funcSetTextb).size(buttonSize);
        titlePane.button("≫", textb, () -> {
            syncCurWave = false;
            curWave = Math.max(curWave + 10, 0);
        }).with(funcSetTextb).size(buttonSize);
        TextField tf = new TextField(String.valueOf(curWave));
        tf.changed(() -> {
            if(Strings.canParseInt(tf.getText()) && Strings.parseInt(tf.getText()) > 0){
                syncCurWave = false;
                curWave = Math.max(Strings.parseInt(tf.getText()) - 1, 0);
            }
        });
        tf.update(() -> {
            if(!tf.hasKeyboard()) tf.setText(curWave + 1 + "");
        });
        titlePane.add(tf).width(80f).height(buttonSize);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            clearData();
            WorldData.updateSpanwer();
            //reset hpbar pools
            hpBars.each(c -> {
                c.wave = -1;
                pool.add(c);
            });
            hpBars.clear();
            tmp.clear();
            Time.run(Math.min(state.rules.waveSpacing, 30f), UnitsData::catchWave);
        });

        Events.on(EventType.CoreChangeEvent.class, e -> WorldData.updateSpanwer());

        Events.on(EventType.WaveEvent.class, e -> Time.run(Math.min(state.rules.waveSpacing, 30f), () -> {
            WorldData.updateSpanwer();
            catchWave();
        }));
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();

        cont.table(t -> {
            //管理员功能
            t.defaults().height(buttonSize);
            t.button(Iconc.admin + Core.bundle.get("waveinfo.buttons.setWave"), textb, () -> {
                curWave = Math.max(curWave, 0);
                state.wave = curWave + 1;
            }).with(funcSetTextb).disabled(b -> net.client());
            t.button(Iconc.admin + Core.bundle.get("waveinfo.buttons.forceRunWave"), textb, () -> {
                logic.runWave();
            }).with(funcSetTextb).disabled(b -> net.client());
            t.button(Iconc.admin + Core.bundle.get("rules.wavetimer"), textbtoggle, () -> {
                if(state.rules.infiniteResources) state.rules.waveTimer = !state.rules.waveTimer;
            }).update(b -> b.setChecked(state.rules.waveTimer)).with(funcSetTextb).disabled(b -> net.client());
        }).growX().row();

        //显示设置
        cont.table(t4 -> {
            t4.defaults().fillY();
            t4.button("@waveinfo.buttons.expand", textb, () -> {
                hpBars.each(bar -> bar.collapser.setCollapsed(false, true));
            }).with(funcSetTextb);
            t4.button("@waveinfo.buttons.fold", textb, () -> {
                hpBars.each(bar -> bar.collapser.setCollapsed(true, true));
            }).with(funcSetTextb);

            t4.button(Iconc.blockSpawn + Core.bundle.get("waves.spawn.all"), textb, null).growX().with(b -> {
                b.setDisabled(() -> WorldData.usedSpawns.isEmpty());
                b.clicked(() -> {
                    var p = new PopupTable();
                    p.popup();
                    p.snapTo(b);
                    p.background(Styles.grayPanel).margin(10f);
                    p.defaults().size(70f, 32f);
                    int i = 0;
                    int cols = 4;

                    for(var spawn : WorldData.usedSpawns){
                        int x = Point2.x(spawn), y = Point2.y(spawn);
                        p.button(x + ", " + y, Styles.flatTogglet, () -> {
                            boolean rebuild = curSpawn != spawn;
                            curSpawn = spawn;
                            if(rebuild) hpBars.each(WaveBar::buildPreview);
                            b.setText(Iconc.blockSpawn + ": " + x + ", " + y);
                            p.hide();
                        }).checked(spawn == curSpawn);

                        if(++i % cols == 0) p.row();
                    }

                    p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                        boolean rebuild = curSpawn != -1;
                        curSpawn = -1;
                        if(rebuild) hpBars.each(WaveBar::buildPreview);
                        b.setText(Iconc.blockSpawn + Core.bundle.get("waves.spawn.all"));
                        p.hide();
                    }).checked(-1 == curSpawn);
                });
            });
        }).growX().row();

        final float[] h = new float[1];
        h[0] = 300f;
        cont.pane(t -> barsTable = t).fillX().self(c -> {
            c.update(p -> {
                h[0] = Mathf.clamp(h[0], 20f, (Core.graphics.getHeight() - 400f)/Scl.scl());
                c.height(h[0]);
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
        }).row();
    }

    @Override
    public void act(float delta){
        if(!state.isGame()){
            clearData();
            return;
        }

        if(waveUpd.get(800)){
            updateData();
            buildBars(barsTable);
        }

        super.act(delta);
    }

    public void buildBars(Table t){
        tmp.clear();
        t.clearChildren();
        final int[] i = {0};
        allwaves.each(d -> {
            if(i[0]++ > 50) return;
            if(d.totalHp + d.totalHp <= 0f) return;
            var found = hpBars.find(b -> b.wave == d.wave);//复用相同wave
            if(found != null){
                hpBars.remove(found);
                tmp.add(found);
            }else{
                var bar = pool.isEmpty() ? new WaveBar() : pool.remove(0);
                bar.setWave(d);
                tmp.add(bar);
            }
        });

        pool.add(hpBars);
        hpBars.clear();
        hpBars.add(tmp);

        hpBars.sort(b -> b.wave);
        hpBars.each(b -> {
            t.add(b).growX();
            b.setup();
            t.row();
        });

        layout();
    }


    //single wave details
    //TODO for future use
    public void buildDetailBars(Table t, WaveData data){
        t.pane(p -> {
            for(int id = 0; id < data.totalsByType.length; id++){
                if(data.totalsByType[id] <= 1f) continue;
                var type = content.unit(id);
                p.image(type.uiIcon).size(18f);
                p.add(new MI2Bar()).with(bar -> {
                    bar.set(() -> {
                            Seq<UnitData> units = data.unitsByType[type.id];
                            if(units == null) return UI.formatAmount((long) data.totalsByType[type.id]);
                            float hp = data.unitsByType[type.id].sumf(udata -> udata.unit.health + udata.unit.shield);
                            return units.size + "|" + UI.formatAmount((long) hp) + "/" + UI.formatAmount((long) data.totalsByType[type.id]);
                        },
                        () -> {
                            Seq<UnitData> units = data.unitsByType[type.id];
                            if(units == null) return 1f;
                            float hp = data.unitsByType[type.id].sumf(udata -> udata.unit.health + udata.unit.shield);
                            return hp / data.totalsByType[type.id];
                        }, Color.scarlet);
                    bar.setFontScale(0.8f).blink(Color.white);
                }).height(10f).minWidth(100f).growX();
                p.row();
            }
        }).maxHeight(200f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    public static class WaveBar extends Table{
        MI2Bar bar;
        Table table;
        MCollapser collapser;
        public int wave;
        public WaveBar(){
            bar = new MI2Bar();
            table = new Table();
            collapser = new MCollapser(table, true).setDirection(false, true).setDuration(0.2f);
            bar.clicked(() -> collapser.toggle(true));
            table.setFillParent(true);
            table.background(Styles.black3);
        }

        public void setup(){
            clear();
            add(bar).growX().height(24f).minWidth(250f);
            row();
            add(collapser);
        }

        public void setWave(WaveData d){
            wave = d.wave;
            bar.set(() -> "Wave " + (d.wave + 1) + ": " + "(" + d.units.size + ") " + UI.formatAmount((long)d.sumHp()) + "/" + UI.formatAmount((long)(d.totalHp + d.totalShield)),
                () -> {
                    if(state.wave - 1 <= d.wave && d.sumHp() <= 0f) return 1f;
                    return d.sumHp() / (d.totalHp + d.totalShield);
                },
                () -> (state.wave - 1 > d.wave || d.sumHp() > 0) ? Color.scarlet : Color.darkGray);
            bar.blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 1f).setFontScale(0.8f);
            buildPreview();
        }

        public void buildPreview(){
            table.clear();
            table.table(tt -> {
                int i = 0;
                for(SpawnGroup group : state.rules.spawns){
                    if(curSpawn != -1 && group.spawn != -1 && curSpawn != group.spawn) continue;
                    if(group.getSpawned(wave) < 1) continue;
                    tt.table(g -> {
                        g.table(gt -> {
                            gt.image(group.type.uiIcon).size(18f);
                            gt.add("x" + group.getSpawned(wave)).get().setFontScale(0.7f);
                        });
                        g.row();
                        g.add(String.valueOf(group.getShield(wave))).get().setFontScale(0.7f);
                        g.row();
                        g.table(eip -> {
                            if(group.effect != null && group.effect != StatusEffects.none) eip.image(group.effect.uiIcon).size(12f);
                            if(group.items != null) eip.stack(new Image(group.items.item.uiIcon), new Label(String.valueOf(group.items.amount)){{
                                this.setFillParent(true);
                                this.setAlignment(Align.bottomRight);
                                this.setFontScale(0.7f);
                            }}).size(12f);
                            if(group.payloads != null && !group.payloads.isEmpty()) eip.add("" + Iconc.units).get().setFontScale(0.7f);
                            if(group.spawn != -1) eip.add(Iconc.blockSpawn + "").get().clicked(() -> {
                                InputUtils.panStable(group.spawn);
                            });
                        });
                    }).pad(2f);
                    if(++i >= 5){
                        i = 0;
                        tt.row();
                    }
                }
            });
        }
    }
}
