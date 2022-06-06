package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.MI2UTmp;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.io.MI2USettings.*;
import mi2u.struct.FloatDataRecorder;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class CoreInfoMindow extends Mindow2{
    protected Interval interval = new Interval();
    protected CoreBuild core;
    protected Team select, team;

    protected PowerGraphTable pg = new PowerGraphTable(330);
    protected PopupTable teamSelect = new PopupTable(), buildPlanTable = new PopupTable(), chartTable;

    protected int[] unitIndex = new int[content.units().size];

    protected ObjectSet<Item> usedItems;
    protected FloatDataRecorder[] itemRecoders;
    protected FloatDataRecorder charting = null;
    
    public CoreInfoMindow(){
        super("@coreInfo.MI2U", "@coreInfo.help");
        itemRecoders = new FloatDataRecorder[content.items().size];
        content.items().each(item -> {
            itemRecoders[item.id] = new FloatDataRecorder(60);
            itemRecoders[item.id].getter = () -> core == null ? 0 : core.items.get(item);
            itemRecoders[item.id].titleGetter = () -> item.localizedName + ": ";
        });

        chartTable = new PopupTable(){
            {
                this.setBackground(Styles.black5);
                this.addCloseButton();
                var ch = new Element(){
                    @Override
                    public void draw(){
                        super.draw();
                        Draw.reset();
                        if(charting != null){
                            charting.defaultDraw(x, y, width, height, true);
                            Draw.color();

                            Font font = Fonts.outline;
                            font.setColor(1f, 1f, 1f, 0.5f);
                            GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

                            String text = UI.formatAmount((long)charting.min());
                            lay.setText(font, text);
                            font.getCache().clear();
                            font.getCache().addText(text, this.x, this.y + lay.height);
                            font.getCache().draw(parentAlpha);

                            text = UI.formatAmount((long)charting.max());
                            lay.setText(font, text);
                            font.getCache().clear();
                            font.getCache().addText(text, this.x, this.y + this.getHeight());
                            font.getCache().draw(parentAlpha);

                            Pools.free(lay);
                        }
                    }
                };
                this.add(ch).fill().size(200f,120f);
                this.row();
                this.label(() -> {
                    if(charting != null && charting.size() > 0){
                        float delta;
                        if(charting.size() < 10){
                            delta = (charting.get(0) - charting.get(charting.size() - 2))/(charting.size() - 1f);
                        }else{
                            delta = (charting.get(0) - charting.get(9))/10f;
                        }
                        return charting.titleGetter.get() + (delta > 0 ? "[green]+":"[red]") + Strings.autoFixed(delta, 2) + "/s (10s)";
                    }
                    return (charting != null ? charting.titleGetter.get():"") + "No Data";
                }).growX();
                this.row();
                this.label(() -> {
                    if(charting != null && charting.size() > 0){
                        float delta;
                        if(charting.size() < charting.cap()){
                            delta = (charting.get(0) - charting.get(charting.size() - 2))/(charting.size() - 1f);
                        }else{
                            delta = (charting.get(0) - charting.get(charting.size() - 1))/(float)charting.size();
                        }
                        return charting.titleGetter.get() + (delta > 0 ? "[green]+":"[red]") + Strings.autoFixed(delta, 2) + "/s (60s)";
                    }
                    return (charting != null ? charting.titleGetter.get():"") + "No Data";
                }).growX();
            }
        };

        Events.on(EventType.ContentInitEvent.class, e -> {
            content.items().each(item -> {
                itemRecoders[item.id].getter = () -> core == null ? 0 : core.items.get(item);
            });
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            content.items().each(item -> {
                itemRecoders[item.id].reset();
            });
        });

        update(() -> {
            if(select == null || !select.active()){
                team = player.team();
            }else{
                team = select;
            }
            core = team.core();
            pg.team = team;

            if(state.isGame() && content.items().count(item -> core != null && core.items.get(item) > 0 && usedItems.add(item)) > 0){
                rebuild();
            }

            if(state.isGame() && core != null && interval.get(60f)){
                for(FloatDataRecorder rec : itemRecoders){
                    if(rec != null) rec.update();
                }
            }

            if(player.unit() != null && player.unit().plans().size <= 0){
                buildPlanTable.hide();
                buildPlanTable.clearChildren();
            }else{
                buildPlanTable.popup();
                buildPlanTable.setPositionInScreen(this.x, this.y - buildPlanTable.getPrefHeight());
            }

            if(chartTable.hasParent()) chartTable.toFront();
        });
    }

    @Override
    public void init() {
        super.init();
        mindowName = "CoreInfo";
        usedItems = new ObjectSet<>();
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(ipt -> {
            ipt.table(utt -> {
                utt.image(Mindow2.white).width(36f).growY().update(i -> i.setColor(team.color));
                utt.button("Select", textb, () -> {
                    rebuildSelect();
                    teamSelect.popup();
                    teamSelect.snapTo(this);
                }).growX().height(48f).update(b -> {
                    b.setText(Core.bundle.get("coreInfo.selectButton.team") + (select == null ? Core.bundle.get("coreInfo.selectButton.playerteam"):team.localized()));
                    b.getLabel().setColor(team == null ? Color.white:team.color);
                });
            }).grow();

            ipt.row();
            if(MI2USettings.getBool(mindowName + ".showCoreItems")){
                ipt.pane(iut -> {
                    int i = 0;

                    for(Item item : content.items()){
                        if(i >= 4 && !usedItems.contains(item)) continue;

                        iut.stack(
                            new Image(item.uiIcon),
                            new Table(t -> t.label(() -> core == null ? "" : (itemRecoders[item.id].get(0) - itemRecoders[item.id].get(1) >= 0 ? "[green]+" : "[red]") + (int)(itemRecoders[item.id].get(0) - itemRecoders[item.id].get(1))).get().setFontScale(0.65f)).right().bottom()
                        ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel)).get().clicked(() -> {
                            charting = itemRecoders[item.id];
                            chartTable.popup();
                            chartTable.setPositionInScreen(this.x - chartTable.getPrefWidth(), this.y);
                        });

                        iut.label(() -> core == null ? "0" :
                                UI.formatAmount(core.items.get(item)))
                                .padRight(3).minWidth(52f).left().get().clicked(() -> {
                            charting = itemRecoders[item.id];
                            chartTable.popup();
                            chartTable.setPositionInScreen(this.x - chartTable.getPrefWidth(), this.y);
                        });;

                        if (++i % 4 == 0) {
                            iut.row();
                        }
                    }
                }).maxHeight(MI2USettings.getInt(mindowName + ".itemsMaxHeight", 150)).update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if (e != null && e.isDescendantOf(p)) {
                        p.requestScroll();
                    } else if (p.hasScroll()) {
                        Core.scene.setScrollFocus(null);
                    }
                }).with(c -> c.setFadeScrollBars(true));
            }

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
                        new Table(t -> t.label(() -> team.data().countType(type) > 0 ? UI.formatAmount(team.data().countType(type)) : "").get().setFontScale(0.65f)).right().bottom()
                        ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel)).get().clicked(() -> {
                            //click to glance unit
                            if(control.input instanceof InputOverwrite inp){
                                if(team.data().unitCache(type) == null || team.data().unitCache(type).isEmpty()) return;
                                unitIndex[type.id]++;
                                if(unitIndex[type.id] >= team.data().unitCache(type).size) unitIndex[type.id] = 0;
                                inp.pan(true, MI2UTmp.v1.set(team.data().unitCache(type).get(unitIndex[type.id]).x(), team.data().unitCache(type).get(unitIndex[type.id]).y()));
                            }
                        });
        
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
                            (team.data().unitCount>1000 ? (team.data().unitCount/1000) + "\n":"") +
                            Mathf.mod(team.data().unitCount, 1000) + "" : "");
                    });
                }}
                ).size(iconSmall).padRight(3);
                uut.stack(new Label("" + Iconc.blockCoreNucleus){{this.setColor(1,0.6f,0,0.5f);}},
                new Table(t -> t.label(() -> core != null && team.data().cores.size > 0 ? UI.formatAmount(team.data().cores.size) : "").get().setFontScale(0.65f)).right().bottom()
                ).size(iconSmall).padRight(3).get().clicked(() -> {
                    if(control.input instanceof InputOverwrite inp && team.cores() != null && !team.cores().isEmpty()){
                        Building b = team.cores().random();
                        inp.pan(true, MI2UTmp.v1.set(b.x, b.y));
                    }
                });
            }).maxHeight(MI2USettings.getInt(mindowName + ".unitsMaxHeight", 200)).update(p -> {
                Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(e != null && e.isDescendantOf(p)){
                    p.requestScroll();
                }else if(p.hasScroll()){
                    Core.scene.setScrollFocus(null);
                }
            }).with(c -> c.setFadeScrollBars(true));
        }

        //buildplan popup table
        if(buildPlanTable == null) buildPlanTable = new PopupTable();
        buildPlanTable.setBackground(Styles.black6);
        buildPlanTable.update(() -> {
            buildPlanTable.setPositionInScreen(this.x, this.y - buildPlanTable.getPrefHeight());
            if(player.unit() == null || player.team().core() == null || player.unit().plans().size > 1000) return;   //Too many plans cause lag
            ItemSeq req = new ItemSeq();
            player.unit().plans().each(plan -> {
                for(ItemStack stack : plan.block.requirements){
                    req.add(stack.item, Mathf.floor(stack.amount * (plan.breaking ? state.rules.deconstructRefundMultiplier*state.rules.buildCostMultiplier:-1*state.rules.buildCostMultiplier)));
                }
            });
            for(Item item : content.items()){
                if(buildPlanTable.getChildren().find(e -> e.name != null && e.name.equals(item.name)) instanceof Label l){
                    l.setText((req.get(item)>0?"+":"") + req.get(item));
                    l.setColor(player.team().core().items.get(item) < -req.get(item) ? Color.red:Color.forest);
                }else{
                    if(req.get(item) == 0) continue;
                    if(buildPlanTable.getCells().size % 8 == 0) buildPlanTable.row();
                    buildPlanTable.image(item.uiIcon).size(16f);
                    buildPlanTable.add("" + req.get(item)).name(item.name);
                }
            }
        });
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
            teamSelect.hideWithoutFocusOn(this, teamSelect);
        });
    }

    /** can be overrided, should use super.initSettings(), called in rebuild() */
    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckEntry(mindowName + ".showCoreItems", "@settings.coreInfo.showCoreItems", true, b -> rebuild()));
        settings.add(new CheckEntry(mindowName + ".showUnits", "@settings.coreInfo.showUnits", true, b -> rebuild()));
        settings.add(new CheckEntry(mindowName + ".showPowerGraphs", "@settings.coreInfo.showPowerGraphs", true, b -> rebuild()));
        settings.add(new FieldEntry(mindowName + ".itemsMaxHeight", "@settings.coreInfo.itemsMaxHeight", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 50 && Strings.parseInt(s) <= 500, s -> rebuild()));
        settings.add(new FieldEntry(mindowName + ".unitsMaxHeight", "@settings.coreInfo.unitsMaxHeight", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 50 && Strings.parseInt(s) <= 500, s -> rebuild()));
    }
}
