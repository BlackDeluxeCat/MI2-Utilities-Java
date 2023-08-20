package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;

import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.struct.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import static mi2u.MI2UVars.*;
import static mi2u.struct.UnitsData.*;
import static mindustry.Vars.*;

public class MapInfoTable extends Table{
    protected Seq<WaveBar> hpBars = new Seq<>(), pool = new Seq<>(), tmp = new Seq<>();
    protected PopupTable detailt = new PopupTable(), wavesPopup = new PopupTable(), attrsListPopup = new PopupTable(), spawnerSelect = new PopupTable();
    public BaseDialog mapAttsDialog;
    Table barsTable;

    private static int lastWave = -1, lastSpawn = -1;
    private MI2Utils.IntervalMillis millTimer = new MI2Utils.IntervalMillis();
    private boolean syncCurWave = true, showWaveDetail = false;
    public static int curWave = 0, curSpawn = -1;
    public MapInfoTable(){
        super();
        Events.on(EventType.WorldLoadEvent.class, e -> {
            clearData();
            WorldData.clear();
            WorldData.scanWorld(world.tiles.height * world.tiles.width);
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

        update(() -> {
            if(!state.isGame()){
                clearData();
                return;
            }
            if(!millTimer.get(500)) return;
            updateData();
            //waves update
            if(barsTable != null && barsTable.hasParent()) buildBars(barsTable);
        });

        //ui on MI2U
        button("@mapInfo.buttons.allAttrs", textb , () -> mapAttsDialog.show()).with(funcSetTextb).growX();
        row();
        button("@mapInfo.buttons.allWaves", textb, () -> {
            wavesPopup.popup(Align.top);
            wavesPopup.snapTo(this);
            wavesPopup.keepInScreen();
        }).with(funcSetTextb).growX();

        //map attributes
        mapAttsDialog = new BaseDialog("@mapInfo.buttons.allAttrs");
        mapAttsDialog.shown(this::setupDetailAttsInfo);
        mapAttsDialog.addCloseButton();

        //wave tools
        wavesPopup.addInGameVisible();
        wavesPopup.update(() -> {
            wavesPopup.keepInScreen();
        });
        wavesPopup.touchable = Touchable.enabled;
        wavesPopup.addCloseButton();
        wavesPopup.addDragMove();
        wavesPopup.background(Styles.black3);
        wavesPopup.table(t -> {
            t.label(() -> "Wave: " + (curWave + 1)).growX();
            t.row();
            t.table(t3 -> {
                t3.button("@mapInfo.buttons.setWave", textb, () -> {
                    curWave = Math.max(curWave, 0);
                    state.wave = curWave + 1;
                }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).minSize(titleButtonSize);
                t3.button("@mapInfo.buttons.forceRunWave", textb, () -> {
                    logic.runWave();
                }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).height(titleButtonSize);
                t3.button("" + Iconc.refresh, textbtoggle, () -> {
                    if(state.rules.infiniteResources) state.rules.waveTimer = !state.rules.waveTimer;
                }).update(b -> b.setChecked(state.rules.waveTimer)).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).height(titleButtonSize);
            });
        }).growX().row();

        wavesPopup.table(t3 -> {
            t3.button("<<", textb, () -> {
                syncCurWave = false;
                curWave = Math.max(curWave - 10, 0);
            }).with(funcSetTextb).size(titleButtonSize);
            t3.button("<", textb, () -> {
                syncCurWave = false;
                curWave = Math.max(curWave - 1, 0);
            }).with(funcSetTextb).size(titleButtonSize);
            t3.button("O", textbtoggle, () -> {
                syncCurWave = !syncCurWave;
            }).with(funcSetTextb).size(titleButtonSize).update(b -> {
                b.setChecked(syncCurWave);
                if(syncCurWave) curWave = Math.max(state.wave - 1, 0);
            });
            t3.button(">", textb, () -> {
                syncCurWave = false;
                curWave = Math.max(curWave + 1, 0);
            }).with(funcSetTextb).size(titleButtonSize);
            t3.button(">>", textb, () -> {
                syncCurWave = false;
                curWave = Math.max(curWave + 10, 0);
            }).with(funcSetTextb).size(titleButtonSize);
            TextField tf = new TextField();
            tf.changed(() -> {
                if(Strings.canParseInt(tf.getText()) && Strings.parseInt(tf.getText()) > 0){
                    syncCurWave = false;
                    curWave = Math.max(Strings.parseInt(tf.getText()) - 1, 0);
                }
            });
            t3.add(tf).width(80f).height(28f);
        }).row();

        wavesPopup.table(t4 -> {
            t4.defaults().fillY();
            t4.button("@mapInfo.buttons.preview", textbtoggle, () -> showWaveDetail = !showWaveDetail).checked(showWaveDetail).with(funcSetTextb);
            t4.button(Iconc.blockSpawn + Core.bundle.get("waves.spawn.all"), textb, null).growX().with(b -> {
                b.clicked(() -> {
                    var p = spawnerSelect;
                    p.clear();
                    p.popup();
                    p.snapTo(b);
                    p.background(Styles.grayPanel).margin(10f);
                    int i = 0;
                    int cols = 4;
                    int max = 32;

                    if(spawner.getSpawns().size >= max){
                        p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                    }

                    for(var spawn : spawner.getSpawns()){
                        p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                            curSpawn = Point2.pack(spawn.x, spawn.y);
                            b.setText(Iconc.blockSpawn + ": " + spawn.x + ", " + spawn.y);
                            p.hide();
                        }).size(50, 32f).checked(spawn.pos() == curSpawn);

                        if(++i % cols == 0) p.row();

                        //only display first 20 spawns, you don't need to see more.
                        if(i >= max) break;
                    }

                    if(spawner.getSpawns().isEmpty()){
                        p.hide();
                    }else{
                        p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                            curSpawn = -1;
                            b.setText(Iconc.blockSpawn + Core.bundle.get("waves.spawn.all"));
                            p.hide();
                        }).size(110f, 45f).checked(-1 == curSpawn);
                    }
                });
                b.setDisabled(() -> spawner.getSpawns().isEmpty());
            });
        }).height(24f).growX().row();


        wavesPopup.pane(p -> {
            p.update(() -> {
                if(lastWave == curWave && lastSpawn == curSpawn) return;
                if(lastSpawn != curSpawn && showWaveDetail) hpBars.each(WaveBar::setupPrev);
                lastWave = curWave;
                lastSpawn = curSpawn;
                p.clear();
                buildPreview(p, curWave, curSpawn);
            });
        }).fillX().maxHeight(Core.graphics.getHeight()*0.1f/Scl.scl()).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        }).row();

        wavesPopup.pane(t -> barsTable = t).fillX().self(c -> {
            c.update(p -> {
                c.maxHeight(Core.graphics.getHeight()*0.5f/Scl.scl());
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
        });

    }

    //single wave details
    public void buildBars(Table t){
        tmp.clear();
        t.clearChildren();
        final int[] i = {0};
        allwaves.each(d -> {
            if(i[0]++ > 50) return;
            if(d.totalHp + d.totalHp <= 0f) return;
            var found = hpBars.find(b -> b.wave == d.wave);
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
            b.setup(showWaveDetail);
            t.row();
        });

        layout();
    }

    public void buildPreview(Table t, int curWave, int spawn){
        t.table(tt -> {
            int i = 0;
            for(SpawnGroup group : state.rules.spawns){
                if(spawn != -1 && group.spawn != -1 && spawn != group.spawn) continue;
                if(group.getSpawned(curWave) < 1) continue;
                tt.table(g -> {
                    g.table(gt -> {
                        gt.image(group.type.uiIcon).size(18f);
                        gt.add("x" + group.getSpawned(curWave)).get().setFontScale(0.7f);
                    });
                    g.row();
                    g.add("" + group.getShield(curWave)).get().setFontScale(0.7f);
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
                            if(control.input instanceof InputOverwrite iow) iow.pan(true, MI2UTmp.v1.set(Point2.unpack(group.spawn).x, Point2.unpack(group.spawn).y).scl(tilesize));
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

    //single wave details
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

    public void addBoolString(Boolp p, String icon, Table t){
        t.add(icon).update(l -> l.color.set(p.get() ? Color.white : Color.darkGray)).pad(1f);
    }

    public void addFloatAttr(Prov<CharSequence> p, String name, Table t){
        t.table(tt -> {
            tt.add(name).get().setFontScale(1f);
            tt.row();
            tt.label(p).get().setFontScale(1f);
        }).pad(2);
    }

    public void setupDetailAttsInfo(){
        mapAttsDialog.cont.clear();
        mapAttsDialog.cont.pane(t -> {
            t.table(rules -> {
                rules.table(tt -> {
                    tt.defaults().left();
                    tt.table(ttt -> {
                        addBoolString(() -> state.rules.pvp , "" + Iconc.modePvp, ttt);
                        addBoolString(() -> state.rules.attackMode , "" + Iconc.modeAttack, ttt);
                        addBoolString(() -> state.rules.editor , "" + Iconc.edit, ttt);
                        addBoolString(() -> state.rules.canGameOver , "" + Iconc.lockOpen, ttt);
                    });
                    tt.row();
                    addBoolString(() -> state.rules.infiniteResources , "@rules.infiniteresources", tt);
                    tt.row();
                    tt.label(() -> "Size: " + world.width() + "x" + world.height()).pad(2f);
                    tt.row();

                    addBoolString(() -> state.rules.waves, "@rules.waves", tt);
                    tt.row();
                    tt.label(() -> Core.bundle.get("mapInfo.winWave") + ": " + state.rules.winWave).pad(2f).get().setColor(state.rules.waves ? Color.white : Color.darkGray);;
                    tt.row();
                    addBoolString(() -> state.rules.waveTimer, "@rules.wavetimer", tt);
                    tt.row();
                    addBoolString(() -> state.rules.waitEnemies, "@rules.waitForWaveToEnd", tt);
                    tt.row();
                    addBoolString(() -> state.rules.waveSending, "@rules.wavesending", tt);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.wavespacing") + ": " + Strings.fixed(state.rules.waveSpacing / 60, 1) + "s").pad(2f).get().setColor(state.rules.waves && state.rules.waveTimer ? Color.white : Color.darkGray);;
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.initialwavespacing") + ": " + Strings.fixed(state.rules.initialWaveSpacing / 60, 1) + "s").pad(2f).get().setColor(state.rules.waves && state.rules.waveTimer ? Color.white : Color.darkGray);;
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.dropzoneradius") + ": " + Strings.fixed(state.rules.dropZoneRadius / tilesize, 1)).pad(2f);
                    tt.row();
                    addBoolString(() -> state.rules.showSpawns, "@mapInfo.showSpawns", tt);
                    tt.row();

                    tt.label(() -> Core.bundle.get("rules.fog") + ": " + state.rules.dynamicColor.toString()).pad(2f).get().setColor(state.rules.fog ? Color.white : Color.darkGray);
                    tt.row();
                    tt.label(() -> Core.bundle.get("mapInfo.staticColor") + ": " + state.rules.staticColor.toString()).pad(2f).get().setColor(state.rules.staticFog ? Color.white : Color.darkGray);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.ambientlight") + ": " + state.rules.ambientLight.toString()).pad(2f).get().setColor(state.rules.lighting ? Color.white : Color.darkGray);
                });

                rules.table(tt -> {
                    tt.defaults().left();
                    addBoolString(() -> state.rules.fire, "@rules.fire", tt);
                    tt.row();
                    addBoolString(() -> state.rules.damageExplosions, "@rules.explosions", tt);
                    tt.row();
                    addBoolString(() -> state.rules.reactorExplosions, "@rules.reactorexplosions", tt);
                    tt.row();
                    addBoolString(() -> state.rules.logicUnitBuild, "@mapInfo.logicUnitBuild", tt);
                    tt.row();
                    addBoolString(() -> state.rules.disableWorldProcessors, "@rules.disableworldprocessors", tt);
                    tt.row();
                    addBoolString(() -> state.rules.schematicsAllowed, "@rules.schematic", tt);
                    tt.row();
                    addBoolString(() -> state.rules.coreIncinerates, "@rules.coreincinerates", tt);
                    tt.row();
                    addBoolString(() -> state.rules.onlyDepositCore, "@rules.onlydepositcore", tt);
                    tt.row();
                    addBoolString(() -> state.rules.unitAmmo, "@rules.unitammo", tt);
                    tt.row();
                    addBoolString(() -> state.rules.possessionAllowed, "@mapInfo.possessionAllowed", tt);
                    tt.row();
                    addBoolString(() -> state.rules.coreCapture, "@rules.corecapture", tt);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.enemycorebuildradius") + ": " + Strings.fixed(state.rules.enemyCoreBuildRadius / tilesize, 1)).pad(2f);
                    tt.row();
                    addBoolString(() -> state.rules.polygonCoreProtection, "@rules.polygoncoreprotection", tt);
                    tt.row();
                    addBoolString(() -> state.rules.coreDestroyClear, "@mapInfo.coreDestroyClear", tt);
                    tt.row();
                    addBoolString(() -> state.rules.cleanupDeadTeams , "@rules.cleanupdeadteams", tt);
                    tt.row();
                    addBoolString(() -> state.rules.placeRangeCheck , "@rules.placerangecheck", tt);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.unitcap") + ": " + state.rules.unitCap + (state.rules.unitCapVariable ? "+"+Iconc.blockCoreShard:"")).pad(2f);
                });
            });

            t.row();

            t.table(tt -> {
                tt.defaults().uniform().fill();

                tt.button("@mapInfo.buttons.bannedBlocks", textb, () -> {
                    showIterable("@mapInfo.buttons.bannedBlocks", state.rules.bannedBlocks, Block::isVisible, (block, table) -> {
                        table.image(block.uiIcon).size(32f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.bannedBlocks.isEmpty());

                tt.button("@mapInfo.buttons.bannedUnits", textb, () -> {
                    showIterable("@mapInfo.buttons.bannedUnits", state.rules.bannedUnits, null, (unit, table) -> {
                        table.image(unit.uiIcon).size(32f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.bannedUnits.isEmpty());

                tt.button("@mapInfo.buttons.hiddenBuildItems", textb, () -> {
                    showIterable("@mapInfo.buttons.hiddenBuildItems", state.rules.hiddenBuildItems, null, (item, table) -> {
                        table.image(item.uiIcon).size(32f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.hiddenBuildItems.isEmpty());

                tt.row();

                tt.button("@mapInfo.buttons.revealedBlocks", textb, () -> {
                    showIterable("@mapInfo.buttons.revealedBlocks", state.rules.revealedBlocks, null, (block, table) -> {
                        table.image(block.uiIcon).size(32f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.revealedBlocks.isEmpty());

                tt.button("@mapInfo.buttons.objectiveFlags", textb, () -> {
                    showIterable("@mapInfo.buttons.objectiveFlags", state.rules.objectiveFlags, null, (str, table) -> {
                        table.add(str).size(48f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.objectiveFlags.isEmpty());

                tt.button("@mapInfo.buttons.mapTags", textb, () -> {
                    showIterable("@mapInfo.buttons.mapTags", state.rules.objectiveFlags, null, (str, table) -> {
                        table.add(str).size(48f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.objectiveFlags.isEmpty());
            });

            t.row();

            t.table(tt -> {
                tt.defaults().left();
                addFloatAttr(() -> Strings.fixed(state.rules.buildCostMultiplier, 2), "@mapInfo.buildCostMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.deconstructRefundMultiplier, 2), "@mapInfo.buildRefundMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.blockHealthMultiplier, 2), "@mapInfo.buildingHpMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.blockDamageMultiplier, 2), "@mapInfo.buildingDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildSpeedMultiplier, 2), "@mapInfo.buildSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitHealthMultiplier, 2), "@mapInfo.unitHealthMultiplier", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitDamageMultiplier, 2), "@mapInfo.unitDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitCrashDamageMultiplier, 2), "@mapInfo.unitCrashDamageMultiplier", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitBuildSpeedMultiplier, 2), "@mapInfo.unitConstructSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.solarMultiplier, 2), "@mapInfo.solarMulti", tt);
            });

            t.row();

            t.table(teamt -> {
                teamt.add("@mapInfo.team").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitConstructSpeedMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitHealthMultiplier").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitDamageMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitCrashDamageMultiplier").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildingHpMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildingDamageMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildSpeedMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.infAmmo").padLeft(5).padRight(5);
                teamt.add("@mapInfo.infRes").padLeft(5).padRight(5);
                teamt.add("@mapInfo.cheat").padLeft(5).padRight(5);
                teamt.add("@mapInfo.rtsAI").padLeft(5).padRight(5);

                for(Teams.TeamData teamData : state.teams.present){
                    teamt.row();
                    var teamRule = state.rules.teams.get(teamData.team);
                    teamt.add("[#" + teamData.team.color + "]" + teamData.team.localized());
                    teamt.add("" + teamRule.unitBuildSpeedMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.unitHealthMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.unitDamageMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.unitCrashDamageMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.blockHealthMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.blockDamageMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.buildSpeedMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.infiniteAmmo).color(teamData.team.color);
                    teamt.add("" + teamRule.infiniteResources).color(teamData.team.color);
                    teamt.add("" + teamRule.cheat).color(teamData.team.color);
                    teamt.add("" + (teamRule.rtsAi ? teamRule.rtsMinWeight + "[" + teamRule.rtsMinSquad + "~" + teamRule.rtsMaxSquad + "]" + (teamRule.aiCoreSpawn ? Core.bundle.format("mapInfo.aiCoreSpawn") : "") : "")).color(teamData.team.color);
                }
            });

        });
    }

    public <T> void showIterable(String title, Iterable<T> array, Boolf<T> boolf, Cons2<T, Table> cons){
        attrsListPopup.clear();
        attrsListPopup.setBackground(Styles.black3);
        attrsListPopup.touchable = Touchable.enabled;
        attrsListPopup.addDragMove();
        attrsListPopup.addCloseButton();
        attrsListPopup.addInGameVisible();
        attrsListPopup.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
        attrsListPopup.update(() -> attrsListPopup.keepInScreen());
        attrsListPopup.add(title).padRight(10f);
        attrsListPopup.row();
        attrsListPopup.pane(cont -> {
            int index = 0;
            for(T item : array){
                if(boolf != null && !boolf.get(item)) continue;
                cont.table(t -> cons.get(item, t));
                if(++index >= 8){
                    index = 0;
                    cont.row();
                }
            }
        }).maxHeight(200f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        }).with(c -> c.setFadeScrollBars(true));

        attrsListPopup.popup();
        attrsListPopup.toFront();
    }

    public class WaveBar extends Table{
        MI2Bar bar;
        Table table;
        public int wave;
        public WaveBar(){
            bar = new MI2Bar();
            table = new Table();
            table.setFillParent(true);
            table.background(Styles.black3);
        }

        public void setup(boolean detail){
            clear();
            if(detail){
                stack(bar, table).growX();
            }else{
                add(bar).growX().height(18f).minWidth(250f);
            }
        }

        public void setupPrev(){
            table.clear();
            buildPreview(table, wave, curSpawn);
        }

        public void setWave(WaveData d){
            wave = d.wave;
            bar.clearListeners();
            bar.set(() -> "Wave " + (d.wave + 1) + ": " + "(" + d.units.size + ") " + UI.formatAmount((long)d.sumHp()) + "/" + UI.formatAmount((long)(d.totalHp + d.totalShield)),
                    () -> {
                        if(state.wave - 1 <= d.wave && d.sumHp() <= 0f) return 1f;
                        return d.sumHp() / (d.totalHp + d.totalShield);
                    },
                    () -> (state.wave - 1 > d.wave || d.sumHp() > 0) ? Color.scarlet : Color.darkGray);
            bar.blink(Color.white).outline(MI2UTmp.c2.set(0.3f, 0.3f, 0.6f, 0.3f), 1f).setFontScale(0.8f);
            bar.tapped(() -> {
                if(detailt.shown) detailt.hide();
                detailt.clear();
                detailt.background(Styles.black8);
                detailt.addCloseButton();
                detailt.addDragMove();
                detailt.addInGameVisible();
                detailt.add("Wave " + (d.wave + 1)).growX().minSize(80f, 36f);
                detailt.row();
                buildPreview(detailt, d.wave, -1);
                detailt.row();
                buildDetailBars(detailt, d);
                detailt.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                detailt.popup();
            });
            setupPrev();
        }
    }
}
