package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.io.MI2USettings.*;
import mi2u.struct.*;
import mi2u.ui.elements.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class CoreInfoMindow extends Mindow2{
    protected Interval interval = new Interval(2);
    protected CoreBuild core;
    protected Team select, team;

    protected PowerGraphTable pg = new PowerGraphTable();
    protected PopupTable teamSelect = new PopupTable(), buildPlanTable = new PopupTable();
    protected int[] unitIndex = new int[content.units().size];

    protected ObjectSet<UnitType> usedUnits;
    protected ObjectSet<Item> usedItems;
    protected FloatDataRecorder[] itemRecoders;
    public PopupTable[] itemCharts;
    public int itemTimerInt = 1;

    public Prov<String> rollingTitle;

    public CoreInfoMindow(){
        super("CoreInfo", "@coreInfo.MI2U", "@coreInfo.help");
        usedItems = new ObjectSet<>();
        usedUnits = new ObjectSet<>();

        titlePane.table(teamt -> {
            teamt.add(new MCollapser(t -> {
                t.label(() -> rollingTitle == null ? "" : rollingTitle.get()).minWidth(80f);
                t.image().width(2f).growY().color(Color.white);
            }, true).setCollapsed(true, () -> rollingTitle == null).setDirection(true, false));
            teamt.button(itemTimerInt + "s", textb, null).size(titleButtonSize).with(b -> {
                b.clicked(() -> {
                    switch(itemTimerInt){
                        case 1 -> itemTimerInt = 10;
                        case 10 -> itemTimerInt = 30;
                        case 30 -> itemTimerInt = 60;
                        default -> itemTimerInt = 1;
                    }
                    b.setText(itemTimerInt + "s");
                });
            });
            teamt.button("Select", textb, () -> {
                rebuildSelect();
                teamSelect.popup();
                teamSelect.snapTo(this);
            }).grow().update(b -> {
                b.setText(team.localized() + (select == null ? Core.bundle.get("coreInfo.selectButton.playerteam"):""));
                b.getLabel().setColor(team == null ? Color.white:team.color);
                b.getLabel().setFontScale(0.8f);
            });
            teamt.button(t -> t.label(() -> Iconc.power + String.valueOf(pg.powerIOBars ? Iconc.list : Iconc.line)), textb, () -> {
                pg.powerIOBars = !pg.powerIOBars;
                pg.diagram.clearChildren();
            }).growY();
        }).height(titleButtonSize).growX();

        itemRecoders = new FloatDataRecorder[content.items().size];
        itemCharts = new PopupTable[content.items().size];
        content.items().each(item -> {
            itemRecoders[item.id] = new FloatDataRecorder(60);
            itemRecoders[item.id].getter = () -> core == null ? 0 : core.items.get(item);
            itemRecoders[item.id].titleGetter = () -> item.localizedName + ": ";
            itemCharts[item.id] = getItemChart(item);
        });

        Events.on(EventType.ContentInitEvent.class, e -> {
            itemCharts = new PopupTable[content.items().size];
            content.items().each(item -> {
                itemRecoders[item.id].getter = () -> core == null ? 0 : core.items.get(item);
                itemCharts[item.id] = getItemChart(item);
            });
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            content.items().each(item -> {
                itemRecoders[item.id].reset();
            });
        });

        Events.on(EventType.ResetEvent.class, e -> {
            usedItems.clear();
            usedUnits.clear();
        });

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame() && core != null && interval.get(0, 60f)){
                for(FloatDataRecorder rec : itemRecoders){
                    if(rec != null) rec.update();
                }
            }
        });
    }

    @Override
    public void act(float delta){
        if(select == null || !select.active()){
            team = player.team();
        }else{
            team = select;
        }
        core = team.core();
        pg.team = team;
        super.act(delta);

        if(state.isGame() && ((content.items().count(item -> core != null && core.items.get(item) > 0 && usedItems.add(item)) > 0) || (content.units().count(type -> team.data().countType(type) > 0 && usedUnits.add(type)) > 0))){
            rebuild();
        }

        if(player.unit() != null && player.unit().plans().isEmpty() && control.input.selectPlans.isEmpty() || !(this.visible && this.hasParent())){
            buildPlanTable.hide();
            buildPlanTable.clearChildren();
        }else{
            buildPlanTable.popup();
            buildPlanTable.setPositionInScreen(this.x, this.y - buildPlanTable.getPrefHeight());
        }
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();

        cont.table(ipt -> {
            if(MI2USettings.getBool(mindowName + ".showCoreItems", true)){
                ipt.pane(iut -> {
                    int i = 0;

                    for(Item item : content.items()){
                        if(!usedItems.contains(item)) continue;

                        var ir = itemRecoders[item.id];

                        var l = new Label(() -> core == null ? "" : (ir.get(0) - ir.get(itemTimerInt) >= 0 ? "[green]+" : "[coral]-") + Strings.autoFixed(Math.abs(ir.get(0) - ir.get(itemTimerInt))/(float)Math.min(itemTimerInt, ir.size()) , 1));
                        l.setAlignment(Align.bottomRight);
                        l.setFontScale(0.7f);
                        l.setFillParent(true);
                        l.setColor(1f,1f,1f,0.7f);

                        iut.stack(new Table(t -> {
                                t.image(item.uiIcon).size(iconSmall);
                                t.label(() -> core == null ? "0" : UI.formatAmount(core.items.get(item))).padRight(3).minWidth(52f).left();
                        }), l).padLeft(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel)).with(s -> {
                            s.clicked(() -> {
                                var chart = getItemChart(item);
                                if(chart != null){
                                    chart.setPositionInScreen(this.x - chart.getPrefWidth(), this.y);
                                    chart.popup();
                                }else{
                                    Log.infoTag("MI2U", "Item Chart error. Contact the develop.");
                                }
                            });
                            s.hovered(() -> rollingTitle = () -> item.localizedName + (core == null ? "" : ": " + core.items.get(item)));
                            s.exited(() -> rollingTitle = null);
                        });

                        if(++i % 4 == 0){
                            iut.row();
                        }
                    }
                }).minWidth(300f).maxHeight(MI2USettings.getInt(mindowName + ".itemsMaxHeight", 150)).update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)){
                        p.requestScroll();
                    }else if(p.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                }).with(c -> c.setFadeScrollBars(true));
            }

            if(MI2USettings.getBool(mindowName + ".showPowerGraphs", true)){
                ipt.row();
                ipt.add(pg).growX().minWidth(300f);
            }
        }).grow();

        //cont.row();

        if(MI2USettings.getBool(mindowName + ".showUnits")){
            cont.pane(uut -> {
                int i = 0, column = Mathf.clamp(usedUnits.size / Math.max(1, MI2USettings.getInt(mindowName + ".unitsMaxHeight", 200) / 24 - 2), 2, 8);
                for(UnitType type : content.units()){
                    //if(type.isHidden()) continue;
                    if(!usedUnits.contains(type)) continue;
                    uut.stack(new Image(type.uiIcon){{this.setColor(1f,1f,1f,0.8f);}},
                        new Table(t -> t.label(() -> team.data().countType(type) > 0 ? UI.formatAmount(team.data().countType(type)) : "").get().setFontScale(0.7f)).right().bottom()
                        ).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(type.localizedName).style(Styles.outlineLabel)).get().clicked(() -> {
                            //click to glance unit
                            if(control.input instanceof InputOverwrite inp){
                                if(team.data().unitCache(type) == null || team.data().unitCache(type).isEmpty()) return;
                                unitIndex[type.id]++;
                                if(unitIndex[type.id] >= team.data().unitCache(type).size) unitIndex[type.id] = 0;
                                inp.pan(true, MI2UTmp.v1.set(team.data().unitCache(type).get(unitIndex[type.id]).x(), team.data().unitCache(type).get(unitIndex[type.id]).y()));
                            }
                        });

                    if(++i % column == 0){
                        uut.row();
                    }
                }
                uut.row();
                uut.stack(new Image(Icon.unitsSmall){{this.setColor(1,0.6f,0,0.5f);}},
                new Label(""){{
                    this.setFillParent(true);
                    this.setAlignment(Align.bottomRight);
                    this.setFontScale(0.7f);
                    this.update(() -> {
                        //this.setFontScale(team.data().unitCount <= 1000 ? 0.65f : 0.5f);
                        this.setText(core != null && team.data().unitCount > 0 ?
                            (team.data().unitCount>1000 ? (team.data().unitCount/1000) + "\n":"") +
                            Mathf.mod(team.data().unitCount, 1000) + "" : "");
                    });
                }}
                ).size(iconSmall).padRight(3);
                uut.stack(new Label("" + Iconc.blockCoreNucleus){{this.setColor(1,0.6f,0,0.5f);}},
                new Table(t -> t.label(() -> core != null && team.data().cores.size > 0 ? UI.formatAmount(team.data().cores.size) : "").get().setFontScale(0.7f)).right().bottom()
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

            ItemSeq req = new ItemSeq(), req2 = new ItemSeq();
            final float[] time = {0f, 0f};
            player.unit().plans().each(plan -> {
                boolean cbhas = false;
                float cbprog = 0f;
                float cbb = 0f;
                if(world.build(plan.x, plan.y) instanceof ConstructBlock.ConstructBuild cb){
                    cbhas = true;
                    cbprog = cb.team == player.team() ? cb.progress : 0f;
                    cbb = cb.buildCost;
                }

                for(ItemStack stack : plan.block.requirements){
                    req.add(stack.item, Mathf.floor(stack.amount * (plan.breaking ? (cbhas ? cbprog : 1f) * state.rules.deconstructRefundMultiplier*state.rules.buildCostMultiplier : (cbhas ? cbprog - 1f : -1f) * state.rules.buildCostMultiplier)));
                }

                time[0] += (cbhas?cbb:plan.block.buildCost) * state.rules.buildCostMultiplier * (cbhas ? (plan.breaking ? cbprog : 1f - cbprog) : 1f) / 60f / (player.unit().type.buildSpeed * player.unit().buildSpeedMultiplier * state.rules.buildSpeed(player.team()));
            });

            control.input.selectPlans.each(plan -> {
                boolean cbhas = false;
                float cbprog = 0f;
                float cbb = 0f;
                if(world.build(plan.x, plan.y) instanceof ConstructBlock.ConstructBuild cb){
                    cbhas = true;
                    cbprog = cb.team == player.team() ? cb.progress : 0f;
                    cbb = cb.buildCost;
                }

                for(ItemStack stack : plan.block.requirements){
                    req2.add(stack.item, Mathf.floor(stack.amount * (plan.breaking ? (cbhas ? cbprog : 1f) * state.rules.deconstructRefundMultiplier*state.rules.buildCostMultiplier : (cbhas ? cbprog - 1f : -1f) * state.rules.buildCostMultiplier)));
                }

                time[1] += (cbhas?cbb:plan.block.buildCost) * state.rules.buildCostMultiplier * (cbhas ? (plan.breaking ? cbprog : 1f - cbprog) : 1f) / 60f / (player.unit().type.buildSpeed * player.unit().buildSpeedMultiplier * state.rules.buildSpeed(player.team()));
            });

            if(buildPlanTable.getChildren().find(e -> e.name != null && e.name.equals("bpt-time")) instanceof Label l){
                l.setText(Strings.fixed(time[0], 1) + (time[1]>0f?"s+"+Strings.fixed(time[1], 1)+"s":"s"));
            }else{
                buildPlanTable.add("").name("bpt-time").colspan(3).row();
            }

            for(Item item : content.items()){
                if(buildPlanTable.getChildren().find(e -> e.name != null && e.name.equals(item.name)) instanceof Label l){
                    l.setText((req.get(item)>0?"+":"") + req.get(item) + (req2.get(item)>0?"+":"") + (req2.get(item)==0?"":req2.get(item)));
                    l.setColor(player.team().core().items.get(item) < -(req.get(item) + req2.get(item)) ? Color.red:Color.forest);
                }else{
                    if(req.get(item) == 0 && req2.get(item) == 0) continue;
                    if((buildPlanTable.getCells().size - 1) % 8 == 0) buildPlanTable.row();
                    buildPlanTable.image(item.uiIcon).size(16f);
                    buildPlanTable.add("").name(item.name).left();
                }
            }
        });
    }

    public void rebuildSelect(){
        teamSelect.clear();
        teamSelect.table(p -> {
            p.button(Iconc.players + "", textb, () -> {
                select = null;
                rebuild();
                teamSelect.hide();
            }).minSize(titleButtonSize * 2f).grow().disabled(select == null).with(b -> {
                b.getLabel().setWrap(false);
                b.getLabel().setColor(player.team().color);
            });
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

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new CheckEntry(mindowName + ".showCoreItems", "@settings.coreInfo.showCoreItems", true, b -> rebuild()));
        settings.add(new CheckEntry(mindowName + ".showUnits", "@settings.coreInfo.showUnits", true, b -> rebuild()));
        settings.add(new CheckEntry(mindowName + ".showPowerGraphs", "@settings.coreInfo.showPowerGraphs", true, b -> rebuild()));
        settings.add(new FieldEntry(mindowName + ".itemsMaxHeight", "@settings.coreInfo.itemsMaxHeight", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 50 && Strings.parseInt(s) <= 500, s -> rebuild()));
        settings.add(new FieldEntry(mindowName + ".unitsMaxHeight", "@settings.coreInfo.unitsMaxHeight", String.valueOf(140), TextField.TextFieldFilter.digitsOnly, s -> Strings.canParseInt(s) && Strings.parseInt(s) >= 50 && Strings.parseInt(s) <= 500, s -> rebuild()));
    }

    public @Nullable PopupTable getItemChart(Item item){
        if(itemCharts[item.id] == null){
            itemCharts[item.id] = new PopupTable(){
                {
                    this.setBackground(Styles.black8);
                    this.addCloseButton();
                    this.addDragMove();
                    this.addInGameVisible();
                    this.update(this::toFront);
                    var ch = new Element(){
                        @Override
                        public void draw(){
                            super.draw();
                            Draw.reset();
                            var chart = itemRecoders[item.id];
                            if(chart != null){
                                Lines.stroke(2f);
                                chart.defaultDraw(x, y, width, height, true);
                                Draw.color();

                                Font font = Fonts.outline;
                                font.setColor(1f, 1f, 1f, 0.5f);
                                GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

                                String text = UI.formatAmount((long)chart.min());
                                lay.setText(font, text);
                                font.getCache().clear();
                                font.getCache().addText(text, this.x, this.y + lay.height);
                                font.getCache().draw(parentAlpha);

                                text = UI.formatAmount((long)chart.max());
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
                        var chart = itemRecoders[item.id];
                        if(chart != null && chart.size() > 0){
                            float delta;
                            if(chart.size() < 10){
                                delta = (chart.get(0) - chart.get(chart.size() - 2))/(chart.size() - 1f);
                            }else{
                                delta = (chart.get(0) - chart.get(9))/10f;
                            }
                            return chart.titleGetter.get() + (delta > 0 ? "[green]+":"[red]") + Strings.autoFixed(delta, 2) + "/s(10s)";
                        }
                        return (chart != null ? chart.titleGetter.get():"") + "No Data";
                    }).growX();
                    this.row();
                    this.label(() -> {
                        var chart = itemRecoders[item.id];
                        if(chart != null && chart.size() > 0){
                            float delta;
                            if(chart.size() < chart.cap()){
                                delta = (chart.get(0) - chart.get(chart.size() - 2))/(chart.size() - 1f);
                            }else{
                                delta = (chart.get(0) - chart.get(chart.size() - 1))/(float)chart.size();
                            }
                            return chart.titleGetter.get() + (delta > 0 ? "[green]+":"[red]") + Strings.autoFixed(delta, 2) + "/s(60s)";
                        }
                        return (chart != null ? chart.titleGetter.get():"") + "No Data";
                    }).growX();
                }
            };
        }
        return itemCharts[item.id];
    }
}
