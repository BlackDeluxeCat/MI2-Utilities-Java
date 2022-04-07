package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.input.InputOverwrite;
import mi2u.io.*;
import mindustry.core.*;
import mindustry.game.Team;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class CoreInfoMindow extends Mindow2{
    protected Interval interval = new Interval();
    protected ObjectIntMap<Item> lastItemsAmt = new ObjectIntMap<>();
    protected ObjectIntMap<Item> lastLastItemsAmt = new ObjectIntMap<>();
    protected CoreBuild core;
    protected Team select, team;
    protected PowerGraphTable pg = new PowerGraphTable(330);
    protected PopupTable teamSelect = new PopupTable();
    protected int[] unitIndex = new int[content.units().size];
    
    public CoreInfoMindow(){
        super("@coreInfo.MI2U", "@coreInfo.help");
        mindowName = "CoreInfo";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.update(() -> {
            if(select == null || !select.active()){
                team = player.team();
            }else{
                team = select;
            }
            core = team.core();
            pg.team = team;
            if(state.isGame() && core != null && interval.get(60f)){
                for(Item item : content.items()){
                    lastLastItemsAmt.put(item, lastItemsAmt.get(item));
                    lastItemsAmt.put(item, core.items.get(item));
                }
            }
        });
        if(!MI2USettings.getBool(mindowName + ".showCoreItems")){
            cont.add().size(80f,20f);
            cont.row();
        }
        
        cont.table(ipt -> {
            ipt.table(utt -> {
                utt.image(Mindow2.white).width(36f).growY().update(i -> {
                    i.setColor(team.color);
                });
                utt.button("Select", textb, () -> {
                    rebuildSelect();
                    teamSelect.popup(Align.left);
                    teamSelect.snapTo(this);
                }).growX().height(48f).update(b -> {
                    b.setText(Core.bundle.get("coreInfo.selectButton.team") + (select == null ? Core.bundle.get("coreInfo.selectButton.playerteam"):team.localized()));
                    b.getLabel().setColor(team == null ? Color.white:team.color);
                });
            }).grow();

            ipt.row();

            ipt.table(iut -> {
                int i = 0;
                if(MI2USettings.getBool(mindowName + ".showCoreItems")){
                    for(Item item : content.items()){
                        iut.stack(
                            new Image(item.uiIcon),
                            new Table(t -> t.label(() -> core == null ? "" : (lastItemsAmt.get(item) - lastLastItemsAmt.get(item) >= 0 ? "[green]+" : "[red]") + (lastItemsAmt.get(item) - lastLastItemsAmt.get(item))).get().setFontScale(0.65f)).right().bottom()
                            ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
                        //image(item.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
                        //TODO leaks garbage
                        iut.label(() -> core == null ? "0" : 
                            UI.formatAmount(core.items.get(item)))
                        .padRight(3).minWidth(52f).left();
            
                        if(++i % 4 == 0){
                            iut.row();
                        }
                    }
                }
            }).maxHeight(200f);
            if(MI2USettings.getBool(mindowName + ".showPowerGraphs")){
                ipt.row();
                ipt.add(pg).fillX();
            }
        });


        //cont.row();

        if(MI2USettings.getBool(mindowName + ".showUnits")){
            cont.pane(uut -> {
                int i = 0;
                for(UnitType type : content.units()){
                    if(type.isHidden()) continue;
                    uut.stack(new Image(type.uiIcon){{this.setColor(1f,1f,1f,0.8f);}},
                        new Table(t -> t.label(() -> core != null && team.data().countType(type) > 0 ? UI.formatAmount(team.data().countType(type)) : "").get().setFontScale(0.65f)).right().bottom()
                        ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel)).get().clicked(() -> {
                            //click to glance unit
                            if(control.input instanceof InputOverwrite inp){
                                if(team.data().unitCache(type) == null || team.data().unitCache(type).isEmpty()) return;
                                unitIndex[type.id]++;
                                if(unitIndex[type.id] >= team.data().unitCache(type).size) unitIndex[type.id] = 0;
                                inp.pan(true, MI2UTmp.v1.set(team.data().unitCache(type).get(unitIndex[type.id]).x(), team.data().unitCache(type).get(unitIndex[type.id]).y()));
                            }
                        });
                    //uut.image(type.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel));
                    //uut.label(() -> core == null ? "0" : UI.formatAmount(team.data().countType(type))).padRight(3).minWidth(52f).left();
        
                    if(++i % 5 == 0){
                        uut.row();
                    }
                }
                uut.stack(new Image(Icon.unitsSmall){{this.setColor(1,0.6f,0,0.5f);}},
                new Label(""){{
                    this.setFillParent(true);
                    this.setAlignment(Align.bottomRight);
                    this.setFontScale(0.65f);
                    this.update(() -> {
                        //this.setFontScale(team.data().unitCount <= 1000 ? 0.65f : 0.5f);
                        this.setText(core != null && team.data().unitCount > 0 ? 
                            (team.data().unitCount>1000?(int)(team.data().unitCount/1000) + "\n":"") + 
                            Mathf.mod(team.data().unitCount, 1000) + "" : "");
                    });
                }}
                ).size(iconSmall).padRight(3);
                uut.stack(new Label("" + Iconc.blockCoreNucleus){{this.setColor(1,0.6f,0,0.5f);}},
                new Table(t -> t.label(() -> core != null && team.data().cores.size > 0 ? UI.formatAmount(team.data().cores.size) : "").get().setFontScale(0.65f)).right().bottom()
                ).size(iconSmall).padRight(3);
            }).maxHeight(200f).update(p -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            });
        }
    }

    public void rebuildSelect(){
        teamSelect.clear();
        teamSelect.table(p -> {
            p.button(Iconc.cancel + "", textb, () -> {
                select = null;
                rebuild();
                teamSelect.hide();
            }).minSize(titleButtonSize).grow().disabled(select == null).get().getLabel().setWrap(false);
            int i = 1;
            for(TeamData t : state.teams.getActive()){
                p.button(t.team.localized(), textb, () -> {
                    select = t.team;
                    rebuild();
                    teamSelect.hide();
                }).minSize(titleButtonSize * 2f).disabled(select == t.team).with(b -> {
                    b.getLabel().setWrap(false);
                    b.getLabel().setColor(t.team.color);
                });
                if(++i < 4) continue;
                p.row();
                i = 0;
            }
        }).maxHeight(300f);
        teamSelect.update(() -> {
            teamSelect.toFront();
            teamSelect.hideWithoutFocusOn(this);
        });
    }



    /** can be overrided, should use super.initSettings(), called in rebuild() */
    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckSettingEntry(mindowName + ".showCoreItems", "@settings.coreInfo.showCoreItems", b -> rebuild()));
        settings.add(new CheckSettingEntry(mindowName + ".showUnits", "@settings.coreInfo.showUnits", b -> rebuild()));
        settings.add(new CheckSettingEntry(mindowName + ".showPowerGraphs", "@settings.coreInfo.showPowerGraphs", b -> rebuild()));
    }
}
