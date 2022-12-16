package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.event.Touchable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.Block;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class MapInfoMindow extends Mindow2{

    public boolean showMapAtts = true, showWaves = true, showWaveBars = true;
    private boolean syncCurWave = true;
    public int curWave = 0; //state.wave is display number, spawner number should sub 1
    public WaveBarTable wavebars = new WaveBarTable();
    public Table mapAttsTable;
    public PopupTable attsListPopup;
    public BaseDialog mapAttsDialog;
    private Interval timer = new Interval();
    private long runTime = 0, lastRunTime = 0, realRunTime = 0, lastRealRun = 0;

    public MapInfoMindow() {
        super("@mapInfo.MI2U", "@mapInfo.help");
        Events.on(WorldLoadEvent.class, e -> {
            realRunTime = runTime = 0;
            lastRealRun = lastRunTime = Time.millis();
            rebuild();
            setCurWave(state.wave);
        });

        Events.run(Trigger.update, () -> {
            if(state.isGame()){
                if(!state.isPaused()){
                    realRunTime += Time.timeSinceMillis(lastRealRun);
                }
                runTime += Time.timeSinceMillis(lastRunTime);
                lastRealRun = Time.millis();
                lastRunTime = Time.millis();
            }
        });
    }

    @Override
    public void init(){
        super.init();
        mindowName = "MapInfo";

        mapAttsDialog = new BaseDialog("@mapinfo.buttons.allAttrs");
        mapAttsDialog.shown(this::setupDetailAttsInfo);
        setupDetailAttsInfo();
        mapAttsDialog.addCloseButton();

        mapAttsTable = new Table();
        mapAttsTable.setBackground(Styles.black5);
        setupMapAtts(mapAttsTable);

        attsListPopup = new PopupTable();
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        //if(!state.isGame()) return;
        cont.visible(() -> state.isGame());
        cont.background(Styles.black5);
        cont.table(tt -> {
            tt.defaults().growX().height(36f);
            tt.button("" + Iconc.map, textbtoggle, () -> {
                showMapAtts = !showMapAtts;
                rebuild();
            }).update(b -> {
                b.setChecked(showMapAtts);
            }).with(funcSetTextb);

            tt.button("" + Iconc.teamCrux, textbtoggle, () -> {
                showWaves = !showWaves;
                rebuild();
            }).update(b -> {
                b.setChecked(showWaves);
            }).with(funcSetTextb);

            tt.button("" + Iconc.chartBar, textbtoggle, () -> {
                showWaveBars = !showWaveBars;
                rebuild();
            }).update(b -> {
                b.setChecked(showWaveBars);
            }).with(funcSetTextb);
        }).fillX();
        cont.row();
        if(showMapAtts){
            cont.add(mapAttsTable);
            cont.row();
        }
        if(showWaves){
            cont.table(this::setupWaves);
            cont.row();
        }
        if(showWaveBars){
            cont.pane(wavebars).growX().minWidth(100f).maxHeight(200f).update(p -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            }).with(c -> c.setFadeScrollBars(true));
        }
    }
    
    public void setupMapAtts(Table t){
        t.clear();
        t.table(timet -> {
            timet.label(() -> Iconc.save + Strings.formatMillis(control.saves.getTotalPlaytime())).minWidth(40f).padRight(8f).get().setFontScale(0.8f);
            timet.label(() -> Iconc.play + Strings.formatMillis(runTime)).minWidth(40f).padRight(8f).get().setFontScale(0.8f);
            timet.label(() -> Iconc.pause + Strings.formatMillis(realRunTime)).minWidth(40f).get().setFontScale(0.8f);
        });
        t.row();
        t.table(rules1 -> {
            rules1.table(tt -> {
                addBoolString(() -> state.rules.fire, "" + Iconc.statusBurning, tt);
                addBoolString(() -> state.rules.damageExplosions, "" + Iconc.itemBlastCompound, tt);
                addBoolString(() -> state.rules.reactorExplosions, "" + Iconc.blockThoriumReactor, tt);
                addBoolString(() -> state.rules.schematicsAllowed, "" + Iconc.paste, tt);
                addBoolString(() -> state.rules.coreIncinerates, "" + Iconc.blockIncinerator, tt);
                addBoolString(() -> state.rules.coreCapture, "" + Iconc.blockCoreFoundation, tt);
                addBoolString(() -> state.rules.polygonCoreProtection, "" + Iconc.grid, tt);
                tt.button("@mapinfo.buttons.allAttrs", textb, () -> mapAttsDialog.show()).growX().minSize(32f).with(funcSetTextb);
            }).growX();
        }).growX();
        t.row();
        t.table(rules2 -> {
            rules2.table(tt -> {
                addFloatAttr(() -> Strings.fixed(state.rules.blockHealthMultiplier, 2), "@mapInfo.buildingHpMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.blockDamageMultiplier, 2), "@mapInfo.buildingDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildCostMultiplier, 2), "@mapInfo.buildCostMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildSpeedMultiplier, 2), "@mapInfo.buildSpeedMutil", tt);
            });
            rules2.row();
            rules2.table(tt -> {
                addFloatAttr(() -> Strings.fixed(state.rules.deconstructRefundMultiplier, 2), "@mapInfo.buildRefundMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitDamageMultiplier, 2), "@mapInfo.unitDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitBuildSpeedMultiplier, 2), "@mapInfo.unitConstructSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.solarMultiplier, 2), "@mapInfo.solarMulti", tt);
            });
        });
    }

    public void setupWaves(Table t){
        t.clear();
        t.table(tt -> {
            tt.table(t3 -> {
                t3.label(() -> "Wave: " + curWave).get().setFontScale(0.7f);
                t3.button("" + Iconc.redo, textb, () -> {
                    curWave = Math.max(curWave, 1);
                    state.wave = curWave;
                    wavebars.curWave = curWave;
                }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).size(titleButtonSize);
                t3.button("@mapinfo.buttons.forceRunWave", textb, () -> {
                    logic.runWave();
                }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).height(titleButtonSize);
                t3.button("" + Iconc.refresh, textbtoggle, () -> {
                    if(state.rules.infiniteResources) state.rules.waveTimer = !state.rules.waveTimer;
                }).update(b -> b.setChecked(state.rules.waveTimer)).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).height(titleButtonSize);
            });

            tt.row();

            tt.table(t3 -> {
                t3.button("<<", textb, () -> {
                    syncCurWave = false;
                    setCurWave(curWave - 10);
                }).with(funcSetTextb).size(titleButtonSize);
                t3.button("<", textb, () -> {
                    syncCurWave = false;
                    setCurWave(curWave - 1);
                }).with(funcSetTextb).size(titleButtonSize);
                t3.button("O", textbtoggle, () -> {
                    syncCurWave = !syncCurWave;
                }).with(funcSetTextb).size(titleButtonSize).update(b -> {
                    b.setChecked(syncCurWave);
                    if(syncCurWave) setCurWave(state.wave);
                });
                t3.button(">", textb, () -> {
                    syncCurWave = false;
                    setCurWave(curWave + 1);
                }).with(funcSetTextb).size(titleButtonSize);
                t3.button(">>", textb, () -> {
                    syncCurWave = false;
                    setCurWave(curWave + 10);
                }).with(funcSetTextb).size(titleButtonSize);
                TextField tf = new TextField();
                tf.changed(() -> {
                    if(Strings.canParseInt(tf.getText()) && Strings.parseInt(tf.getText()) > 0){
                        syncCurWave = false;
                        setCurWave(Strings.parseInt(tf.getText()));
                    }
                });
                t3.add(tf).width(80f).height(28f);
            });
        });
        t.row();

        t.pane(p -> {
            p.update(() -> {
                if(!timer.get(60f)) return;
                p.clear();
                int i = 0;
                for(SpawnGroup group : state.rules.spawns){
                    if(group.getSpawned(curWave - 1) < 1) continue;
                    p.table(g -> {
                        g.table(gt -> {
                            gt.image(group.type.uiIcon).size(18f);
                            gt.add("x" + group.getSpawned(curWave - 1)).get().setFontScale(0.7f);
                        });
                        g.row();
                        g.add("" + group.getShield(curWave - 1)).get().setFontScale(0.7f);
                        g.row();
                        g.table(eip -> {
                            if(group.effect != null && group.effect != StatusEffects.none) eip.image(group.effect.uiIcon).size(12f);
                            if(group.items != null) eip.image(group.items.item.uiIcon).size(12f);
                            if(group.payloads!=null && !group.payloads.isEmpty()) eip.add("" + Iconc.units).get().setFontScale(0.7f);
                        });
                    }).pad(2f);
                    if(++i >= 5){
                        i = 0;
                        p.row();
                    }
                }
            });
        }).fillX().maxHeight(200f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)){
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    public void setCurWave(int wave){
        curWave = Math.max(wave, 1);
        boolean updateBars = wavebars.previewedWave != curWave;
        wavebars.previewedWave = curWave;
        if(updateBars){
            wavebars.updateData();
            wavebars.setupBars();
        }
    }

    public void addBoolString(Boolp p, String icon, Table t){
        t.add(icon).update(l -> l.color.set(p.get() ? Color.white : Color.darkGray)).pad(1f);
    }

    public void addFloatAttr(Prov<CharSequence> p, String name, Table t){
        t.table(tt -> {
            tt.add(name).get().setFontScale(0.8f);
            tt.row();
            tt.label(p).get().setFontScale(0.8f);
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
                addFloatAttr(() -> Strings.fixed(state.rules.blockHealthMultiplier, 2), "@mapInfo.buildingHpMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.blockDamageMultiplier, 2), "@mapInfo.buildingDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildCostMultiplier, 2), "@mapInfo.buildCostMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildSpeedMultiplier, 2), "@mapInfo.buildSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.deconstructRefundMultiplier, 2), "@mapInfo.buildRefundMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitDamageMultiplier, 2), "@mapInfo.unitDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitBuildSpeedMultiplier, 2), "@mapInfo.unitConstructSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.solarMultiplier, 2), "@mapInfo.solarMulti", tt);
            });

            t.row();

            t.table(teamt -> {
                teamt.add("@mapInfo.team").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitConstructSpeedMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.unitDamageMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildingHpMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildingDamageMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.buildSpeedMutil").padLeft(5).padRight(5);
                teamt.add("@mapInfo.infAmmo").padLeft(5).padRight(5);
                teamt.add("@mapInfo.infRes").padLeft(5).padRight(5);
                teamt.add("@mapInfo.cheat").padLeft(5).padRight(5);
                teamt.add("@mapInfo.rtsAI").padLeft(5).padRight(5);

                for(TeamData teamData : state.teams.getActive()){
                    teamt.row();
                    var teamRule = state.rules.teams.get(teamData.team);
                    teamt.add("[#" + teamData.team.color + "]" + teamData.team.localized());
                    teamt.add("" + teamRule.unitBuildSpeedMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.unitDamageMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.blockHealthMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.blockDamageMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.buildSpeedMultiplier).color(teamData.team.color);
                    teamt.add("" + teamRule.infiniteAmmo).color(teamData.team.color);
                    teamt.add("" + teamRule.infiniteResources).color(teamData.team.color);
                    teamt.add("" + teamRule.cheat).color(teamData.team.color);
                    //TODO rts ai fit
                    teamt.add("" + (teamRule.rtsAi ? teamRule.rtsMinWeight + "[" + teamRule.rtsMinSquad + "~" + teamRule.rtsMaxSquad + "]" + (teamRule.aiCoreSpawn ? Core.bundle.format("mapInfo.aiCoreSpawn") : "") : "")).color(teamData.team.color);
                }
            });

        });
    }

    public <T> void showIterable(String title, Iterable<T> array, Boolf<T> boolf, Cons2<T, Table> cons){
        attsListPopup.clear();
        attsListPopup.setBackground(Styles.black3);
        attsListPopup.touchable = Touchable.enabled;
        attsListPopup.addDragMove();
        attsListPopup.addCloseButton();
        attsListPopup.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
        attsListPopup.update(() -> attsListPopup.keepInScreen());
        attsListPopup.add(title).padRight(10f);
        attsListPopup.row();
        attsListPopup.pane(cont -> {
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

        attsListPopup.popup();
        attsListPopup.toFront();
    }
}
