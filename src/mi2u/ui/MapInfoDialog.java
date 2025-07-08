package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.ui.elements.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import java.util.*;

import static mi2u.MI2UVars.funcSetTextb;
import static mi2u.MI2UVars.textb;
import static mindustry.Vars.*;
import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;

public class MapInfoDialog extends BaseDialog{
    protected PopupTable attrsListPopup = new PopupTable();
    public MapInfoDialog(){
        super("MapInfo");
        shown(this::setupDetailAttsInfo);
        addCloseButton();
    }

    public void addBoolString(Boolp p, String icon, Table t){
        t.add(icon).update(l -> l.color.set(p.get() ? Color.white : Color.darkGray)).pad(1f);
    }

    public void setupDetailAttsInfo(){
        cont.clear();
        cont.pane(t -> {
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
                    tt.label(() -> Core.bundle.get("mapInfo.winWave") + ": " + state.rules.winWave).pad(2f).get().setColor(state.rules.waves ? Color.white : Color.darkGray);
                    tt.row();
                    addBoolString(() -> state.rules.waveTimer, "@rules.wavetimer", tt);
                    tt.row();
                    addBoolString(() -> state.rules.waitEnemies, "@rules.waitForWaveToEnd", tt);
                    tt.row();
                    addBoolString(() -> state.rules.waveSending, "@rules.wavesending", tt);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.wavespacing") + ": " + Strings.fixed(state.rules.waveSpacing / 60, 1) + "s").pad(2f).get().setColor(state.rules.waves && state.rules.waveTimer ? Color.white : Color.darkGray);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.initialwavespacing") + ": " + Strings.fixed(state.rules.initialWaveSpacing / 60, 1) + "s").pad(2f).get().setColor(state.rules.waves && state.rules.waveTimer ? Color.white : Color.darkGray);
                    tt.row();
                    tt.label(() -> Core.bundle.get("rules.dropzoneradius") + ": " + Strings.fixed(state.rules.dropZoneRadius / tilesize, 1)).pad(2f);
                    tt.row();
                    addBoolString(() -> state.rules.showSpawns, "@mapInfo.showSpawns", tt);
                    tt.row();
                    addBoolString(() -> state.rules.randomWaveAI, "@rules.randomwaveai", tt);
                    tt.row();
                    addBoolString(() -> state.rules.airUseSpawns, "@rules.airUseSpawns", tt);
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
                    addBoolString(() -> state.rules.allowEditWorldProcessors, "@rules.alloweditworldprocessors", tt);
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
                    tt.label(() -> Core.bundle.get("rules.unitcap") + ": " + (state.rules.disableUnitCap ? Iconc.cancel : (state.rules.unitCap + (state.rules.unitCapVariable ? "+"+Iconc.blockCoreShard:"")))).pad(2f);
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

                tt.button("@mapInfo.buttons.revealedBlocks", textb, () -> {
                    showIterable("@mapInfo.buttons.revealedBlocks", state.rules.revealedBlocks, null, (block, table) -> {
                        table.image(block.uiIcon).size(32f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.revealedBlocks.isEmpty());

                tt.row();

                tt.button("@mapInfo.buttons.objectives", textb, () -> {
                    showIterable("@mapInfo.buttons.objectives", state.rules.objectives, null, (objective, table) -> {
                        table.table(obj -> {
                            int idx = state.rules.objectives.all.indexOf(objective);
                            obj.left().label(() -> "[" + idx + "]" + objective.typeName()).growX().row();
                            obj.table(info -> {
                                info.label(() -> (objective.hidden? "": "[lightgray]") + "Hidden: " + objective.hidden).growX().row();
                                info.label(() -> (objective.qualified()? "": "[lightgray]") + "Qualified: " + objective.qualified()).growX().row();
                                if (objective.flagsAdded.length != 0) info.label(() -> "Added Flags: " + Arrays.toString(objective.flagsAdded)).growX().row();
                                if (objective.flagsRemoved.length != 0) info.label(() -> "Removed Flags: " + Arrays.toString(objective.flagsRemoved)).growX().row();
                            }).growX().padLeft(8f);
                        }).left().growX().padTop(4f).padBottom(4f);
                    }, 1);
                }).with(funcSetTextb).disabled(b -> state.rules.objectives.all.isEmpty());

                tt.button("@mapInfo.buttons.objectiveFlags", textb, () -> {
                    showIterable("@mapInfo.buttons.objectiveFlags", state.rules.objectiveFlags, null, (str, table) -> {
                        table.add(str).size(0, 32f);
                    }, 1);
                }).with(funcSetTextb).disabled(b -> state.rules.objectiveFlags.isEmpty());

                tt.button("@mapInfo.buttons.mapTags", textb, () -> {
                    showIterable("@mapInfo.buttons.mapTags", state.rules.tags, null, (str, table) -> {
                        table.add(str.key).size(64f);
                        table.add(str.value).size(128f);
                    });
                }).with(funcSetTextb).disabled(b -> state.rules.tags.isEmpty());
            });

            t.row();

            t.table(tt -> {
                tt.defaults().left().pad(5f);
                tt.add("@rules.buildcostmultiplier");
                tt.label(() -> Strings.fixed(state.rules.buildCostMultiplier, 2));
                tt.add("@rules.deconstructrefundmultiplier");
                tt.label(() -> Strings.fixed(state.rules.deconstructRefundMultiplier, 2));
                tt.add("@rules.solarmultiplier");
                tt.label(() -> Strings.fixed(state.rules.solarMultiplier, 2));
            });

            t.row();

            t.pane(teamt -> {
                teamt.defaults().pad(5f).width(60f);
                teamt.add("@mapInfo.team").labelAlign(Align.right);
                teamt.labelWrap("@rules.blockhealthmultiplier");
                teamt.labelWrap("@rules.blockdamagemultiplier");
                teamt.labelWrap("@rules.buildspeedmultiplier");
                teamt.labelWrap("@rules.unitbuildspeedmultiplier");
                teamt.labelWrap("@rules.unitcostmultiplier");
                teamt.labelWrap("@rules.unitminespeedmultiplier");
                teamt.labelWrap("@rules.unithealthmultiplier");
                teamt.labelWrap("@rules.unitdamagemultiplier");
                teamt.labelWrap("@rules.unitcrashdamagemultiplier");
                teamt.labelWrap("@rules.infiniteresources");
                teamt.labelWrap("@mapInfo.infAmmo");
                teamt.labelWrap("@mapInfo.cheat");
                teamt.labelWrap("@mapInfo.rtsAI");

                teamt.row();

                teamt.add("" + Iconc.planet).labelAlign(Align.right);
                teamt.add(String.valueOf(state.rules.blockHealthMultiplier));
                teamt.add(String.valueOf(state.rules.blockDamageMultiplier));
                teamt.add(String.valueOf(state.rules.buildSpeedMultiplier));
                teamt.add(String.valueOf(state.rules.unitBuildSpeedMultiplier));
                teamt.add(String.valueOf(state.rules.unitCostMultiplier));
                teamt.add(String.valueOf(state.rules.unitMineSpeedMultiplier));
                teamt.add(String.valueOf(state.rules.unitHealthMultiplier));
                teamt.add(String.valueOf(state.rules.unitDamageMultiplier));
                teamt.add(String.valueOf(state.rules.unitCrashDamageMultiplier));
                teamt.add(state.rules.infiniteResources ? "" + Iconc.ok:"");
                teamt.add("-");
                teamt.add("-");

                for(Teams.TeamData teamData : state.teams.present){
                    teamt.row();
                    var teamRule = state.rules.teams.get(teamData.team);
                    teamt.add("[#" + teamData.team.color + "]" + teamData.team.localized()).labelAlign(Align.right).wrap();
                    teamt.add((Mathf.equal(teamRule.blockHealthMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.blockHealthMultiplier);
                    teamt.add((Mathf.equal(teamRule.blockDamageMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.blockDamageMultiplier);
                    teamt.add((Mathf.equal(teamRule.buildSpeedMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.buildSpeedMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitBuildSpeedMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitBuildSpeedMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitCostMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitCostMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitMineSpeedMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitMineSpeedMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitHealthMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitHealthMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitDamageMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitDamageMultiplier);
                    teamt.add((Mathf.equal(teamRule.unitCrashDamageMultiplier, 1f) ? "[gray]" : "[#" + teamData.team.color + "]") + teamRule.unitCrashDamageMultiplier);

                    teamt.add(teamRule.infiniteResources ? "✔" + Iconc.ok:"");
                    //teamt.add(teamRule.infiniteAmmo ? "✔" + Iconc.ok:"");
                    teamt.add(teamRule.cheat ? "✔" + Iconc.ok:"");
                    teamt.add(teamRule.rtsAi ? teamRule.rtsMinWeight + "[" + teamRule.rtsMinSquad + "~" + teamRule.rtsMaxSquad + "]" + (teamRule.aiCoreSpawn ? Core.bundle.format("mapInfo.aiCoreSpawn") : "") : "");
                }
            }).grow();

        });
    }

    public <T> void showIterable(String title, Iterable<T> array, Boolf<T> boolf, Cons2<T, Table> cons, int itemPerRow){
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
                cont.table(t -> cons.get(item, t)).growX();
                if(++index >= itemPerRow){
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

    public <T> void showIterable(String title, Iterable<T> array, Boolf<T> boolf, Cons2<T, Table> cons){
        showIterable(title, array, boolf, cons, 8);
    }
}
