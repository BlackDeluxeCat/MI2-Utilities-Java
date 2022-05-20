package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.*;
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

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class MapInfoMindow extends Mindow2{

    public boolean showMapAtts = true, showWaves = true, showWaveBars = true;
    private boolean syncCurWave = true;
    public int curWave = 0; //state.wave is display number, spawner number should sub 1
    public WaveBarTable wavebars = new WaveBarTable();
    public Table mapAttsTable;
    private Interval timer = new Interval();

    public MapInfoMindow() {
        super("@mapInfo.MI2U", "@mapInfo.help");
        Events.on(WorldLoadEvent.class, e -> {
            Time.run(60f, this::rebuild);
            curWave = state.wave;
        });
    }

    @Override
    public void init() {
        super.init();
        mindowName = "MapInfo";
        mapAttsTable = new Table();
        mapAttsTable.setBackground(Styles.black5);
        setupMapAtts(mapAttsTable);
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
            cont.table(t -> setupWaves(t));
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
        t.table(rules1 -> {
            rules1.table(tt -> {
                addBoolIcon(() -> state.rules.pvp , "" + Iconc.modePvp, tt);
                addBoolIcon(() -> state.rules.cleanupDeadTeams , "" + Iconc.teamDerelict, tt);
                addBoolIcon(() -> state.rules.attackMode , "" + Iconc.modeAttack, tt);
                addBoolIcon(() -> state.rules.editor , "" + Iconc.edit, tt);
                addBoolIcon(() -> state.rules.infiniteResources , "" + Iconc.box, tt);
                addBoolIcon(() -> state.rules.canGameOver , "" + Iconc.blockReconstructorBasis, tt);
                tt.label(() -> "Size: " + world.width() + "x" + world.height()).pad(2f).get().setFontScale(0.7f);
                addBoolIcon(() -> state.rules.waves, "" + Iconc.waves, tt);
                addBoolIcon(() -> state.rules.waveTimer, "" + Iconc.rotate, tt);
                addBoolIcon(() -> state.rules.waitEnemies, "" + Iconc.pause, tt);
                tt.label(() -> Strings.fixed(state.rules.waveSpacing / 60, 1) + "s").pad(2f).get().setFontScale(0.7f);
            });
            rules1.row();
            rules1.table(tt -> {
                addBoolIcon(() -> state.rules.fire, "" + Iconc.statusBurning, tt);
                addBoolIcon(() -> state.rules.damageExplosions, "" + Iconc.itemBlastCompound, tt);
                addBoolIcon(() -> state.rules.reactorExplosions, "" + Iconc.blockThoriumReactor, tt);
                addBoolIcon(() -> state.rules.logicUnitBuild, "" + Iconc.blockLogicProcessor, tt);
                addBoolIcon(() -> state.rules.schematicsAllowed, "" + Iconc.paste, tt);
                addBoolIcon(() -> state.rules.coreIncinerates, "" + Iconc.blockIncinerator, tt);
                addBoolIcon(() -> state.rules.unitAmmo, "" + Iconc.unitCorvus + Iconc.statusElectrified, tt);
                addBoolIcon(() -> state.rules.coreCapture, "" + Iconc.blockCoreFoundation, tt);
                addBoolIcon(() -> state.rules.polygonCoreProtection, "" + Iconc.grid, tt);
                tt.label(() -> "Cap: " + state.rules.unitCap + (state.rules.unitCapVariable ? "+"+Iconc.blockCoreShard:"")).pad(2f).get().setFontScale(0.7f);
                tt.button("@mapinfo.buttons.teams", textb, this::showTeamInfo).growX().get().getLabel().setWrap(false);
            });
        });
        t.row();
        t.table(rules2 -> {
            rules2.table(tt -> {
                addFloatAttr(() -> Strings.fixed(state.rules.blockHealthMultiplier, 2), "@mapInfo.buildingHpMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.blockDamageMultiplier, 2), "@mapInfo.buildingDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildCostMultiplier, 2), "@mapInfo.buildCostMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.buildSpeedMultiplier, 2), "@mapInfo.buildSpeedMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.deconstructRefundMultiplier, 2), "@mapInfo.buildRefundMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitDamageMultiplier, 2), "@mapInfo.unitDamageMutil", tt);
                addFloatAttr(() -> Strings.fixed(state.rules.unitBuildSpeedMultiplier, 2), "@mapInfo.unitConstructSpeedMutil", tt);
            });
            rules2.row();
        });
    }

    public void setupWaves(Table t){
        t.clear();
        t.table(tt -> {
            tt.label(() -> "Wave: " + curWave).get().setFontScale(0.7f);
            tt.button("<<", textb, () -> {
                syncCurWave = false;
                curWave -= 10;
                curWave = Math.max(curWave, 1);
            }).with(funcSetTextb).size(titleButtonSize);
            tt.button("<", textb, () -> {
                syncCurWave = false;
                curWave -= 1;
                curWave = Math.max(curWave, 1);
            }).with(funcSetTextb).size(titleButtonSize);
            tt.button("O", textbtoggle, () -> {
                syncCurWave = !syncCurWave;
            }).with(funcSetTextb).size(titleButtonSize).update(b -> {
                b.setChecked(syncCurWave);
                if(syncCurWave) curWave = state.wave;
            });
            tt.button(">", textb, () -> {
                syncCurWave = false;
                curWave += 1;
                curWave = Math.max(curWave, 1);
            }).with(funcSetTextb).size(titleButtonSize);
            tt.button(">>", textb, () -> {
                syncCurWave = false;
                curWave += 10;
                curWave = Math.max(curWave, 1);
            }).with(funcSetTextb).size(titleButtonSize);
            TextField tf = new TextField();
            tf.changed(() -> {
                if(Strings.canParseInt(tf.getText()) && Strings.parseInt(tf.getText()) > 0){
                    syncCurWave = false;
                    curWave = Strings.parseInt(tf.getText());
                }
            });
            tt.add(tf).width(80f).height(28f);
            tt.button("" + Iconc.redo, textb, () -> {
                curWave = Math.max(curWave, 1);
                state.wave = curWave;
            }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).size(titleButtonSize);
            tt.button("@mapinfo.buttons.forceRunWave", textb, () -> {
                logic.runWave();
            }).with(funcSetTextb).with(b -> b.setDisabled(() -> net.client())).height(titleButtonSize);
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

    public void addBoolIcon(Boolp p, String icon, Table t){
        t.label(() -> icon).color(p.get() ? Color.white : Color.red).pad(1f);
    }

    public void addFloatAttr(Prov<CharSequence> p, String name, Table t){
        t.table(tt -> {
            tt.add(name).get().setFontScale(0.7f);
            tt.row();
            tt.label(p).get().setFontScale(0.7f);
        }).pad(2);
    }

    public void showTeamInfo(){
        BaseDialog dialog = new BaseDialog("@mapinfo.buttons.teams");
        dialog.cont.pane(tmap -> {
            tmap.add("@mapInfo.team").padLeft(5).padRight(5);
            tmap.add("@mapInfo.unitConstructSpeedMutil").padLeft(5).padRight(5);
            tmap.add("@mapInfo.unitDamageMutil").padLeft(5).padRight(5);
            tmap.add("@mapInfo.buildingHpMutil").padLeft(5).padRight(5);
            tmap.add("@mapInfo.buildingDamageMutil").padLeft(5).padRight(5);
            tmap.add("@mapInfo.buildSpeedMutil").padLeft(5).padRight(5);
            tmap.add("@mapInfo.infAmmo").padLeft(5).padRight(5);
            tmap.add("@mapInfo.infRes").padLeft(5).padRight(5);
            tmap.add("@mapInfo.cheat").padLeft(5).padRight(5);
            tmap.add("@mapInfo.ai").padLeft(5).padRight(5);
            for(TeamData teamData : state.teams.getActive()){
                tmap.row();
                var teamRule = state.rules.teams.get(teamData.team);
                tmap.add("[#" + teamData.team.color + "]" + teamData.team.localized());
                tmap.add("" + teamRule.unitBuildSpeedMultiplier).color(teamData.team.color);
                tmap.add("" + teamRule.unitDamageMultiplier).color(teamData.team.color);
                tmap.add("" + teamRule.blockHealthMultiplier).color(teamData.team.color);
                tmap.add("" + teamRule.blockDamageMultiplier).color(teamData.team.color);
                tmap.add("" + teamRule.buildSpeedMultiplier).color(teamData.team.color);
                tmap.add("" + teamRule.infiniteAmmo).color(teamData.team.color);
                tmap.add("" + teamRule.infiniteResources).color(teamData.team.color);
                tmap.add("" + teamRule.cheat).color(teamData.team.color);
                tmap.add("" + (teamRule.ai ? Core.bundle.format("mapInfo.aiAndTier", teamRule.aiTier) + (teamRule.aiCoreSpawn ? "" + Core.bundle.format("mapInfo.aiCoreSpawn") : "") : "")).color(teamData.team.color);
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }
}
