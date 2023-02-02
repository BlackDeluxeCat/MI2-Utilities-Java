package mi2u.ui;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.io.MI2USettings;
import mi2u.struct.*;
import mi2u.struct.WorldData.*;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class WorldFinderTable extends PopupTable{
    protected WorldFinder finder = new WorldFinder();

    protected PopupTable selectTable = new PopupTable();
    private boolean withName = false;
    private int showSubTableID = -1;
    private static final Interval worldDataTimer = new Interval();
    @Nullable
    public Team team;
    public Block find = Blocks.air, replace = Blocks.air;
    TextureRegionDrawable tmp1 = new TextureRegionDrawable(), tmp2 = new TextureRegionDrawable();

    public WorldFinderTable(){
        float size = 36f;
        addCloseButton();
        addDragMove();
        addInGameVisible();
        update(() -> {
            this.keepInScreen();
            if(MI2USettings.getBool("worldDataUpdate") && worldDataTimer.get(Mathf.clamp(MI2USettings.getInt("worldDataUpdate.interval", 10), 3, 60))){
                WorldData.scanWorld();
            }
        });
        touchable = Touchable.enabled;
        setBackground(Styles.black8);

        add("@minimap.finder.title").growX().left().height(24f);

        row();
        add("@minimap.finder.find").color(Color.sky).left().growX();
        row();

        table(t -> {
            t.defaults().growY();
            t.button("", textb, () -> {
                showSubTableID = 0;
                setupSelect();
            }).minSize(size*2f, size).update(b -> b.setText(find.localizedName)).with(funcSetTextb).with(b -> {
                b.image().size(size).update(i -> i.setDrawable(tmp1.set(find.uiIcon))).pad(1f);
                b.getCells().reverse();
            });

            t.button("", textb, () -> {
                showSubTableID = 2;
                setupSelect();
            }).minSize(size*2f, size).update(b -> {
                b.setText(Core.bundle.get("coreInfo.selectButton.team") + (team == null ? Iconc.map : team.localized()));
                b.getLabel().setColor(team == null ? Color.white:team.color);
            }).with(funcSetTextb);
        });

        row();
        //finder button
        table(t -> {
            t.button(Iconc.zoom + "+1", textb, () -> {
                finder.team = team;
                finder.findTarget = find;
                int pos = finder.findNext();
                if(pos != -1 && control.input instanceof InputOverwrite ipo) ipo.pan(true, MI2UTmp.v1.set(Point2.x(pos), Point2.y(pos)).scl(tilesize));
            }).height(size).with(funcSetTextb);
            t.button("+20", textb, () -> {
                finder.team = team;
                finder.findTarget = find;
                int pos = finder.findNext(20);
                if(pos != -1 && control.input instanceof InputOverwrite ipo) ipo.pan(true, MI2UTmp.v1.set(Point2.x(pos), Point2.y(pos)).scl(tilesize));
            }).height(size).with(funcSetTextb);
            t.button("+2%", textb, () -> {
                finder.team = team;
                finder.findTarget = find;
                int pos = finder.findNext(Mathf.floor(WorldData.countBlock(find, team) * 0.01f));
                if(pos != -1 && control.input instanceof InputOverwrite ipo) ipo.pan(true, MI2UTmp.v1.set(Point2.x(pos), Point2.y(pos)).scl(tilesize));
            }).height(size).with(funcSetTextb);
            t.label(() -> finder.findIndex + "/" + WorldData.countBlock(find, team) + (finder.lastPos == -1 ? "" : Point2.unpack(finder.lastPos).toString()));
        }).grow().padBottom(8f);

        //replace buttons
        row();
        add("@minimap.finder.replace").color(Color.sky).left().growX();
        row();

        button("", textb, () -> {
            showSubTableID = 1;
            setupSelect();
        }).minSize(size*2f, size).update(b -> b.setText(replace.localizedName)).with(funcSetTextb).with(b -> {
            b.image().size(size).update(i -> i.setDrawable(tmp2.set(replace.uiIcon))).pad(1f);
            b.getCells().reverse();
        }).growX();

        row();

        table(t -> {
            t.defaults().growX();
            t.button("@minimap.finder.unitPos", textb, () -> {
                planReplace(find, replace, 8f);
            }).height(size).with(funcSetTextb);
            t.button("@minimap.finder.unitRange", textb, () -> {
                planReplace(find, replace, player.unit().range());
            }).height(size).with(funcSetTextb);
            t.row();
            t.button("@minimap.finder.buildRange", textb, () -> {
                planReplace(find, replace, buildingRange);
            }).height(size).with(funcSetTextb);
            t.button("@minimap.finder.mapRange", textb, () -> {
                planReplace(find, replace, -1f);
            }).height(size).with(funcSetTextb);
        }).grow();
    }

    public void planReplace(Block from, Block to, float range){
        if(!state.isGame() || player.unit() == null || !player.unit().canBuild()) return;
        var seq = WorldData.getSeq(from, player.team());
        if(seq == null) return;

        seq.each(pos -> {
            if(range != -1f && MI2UTmp.v1.set(Point2.x(pos), Point2.y(pos)).scl(tilesize).sub(player.unit().x, player.unit().y).len() > range) return;
            if(player.unit().plans().size > 1000) return;
            if(world.tile(pos).breakable() && to == Blocks.air){
                //break
                player.unit().addBuild(new BuildPlan(Point2.x(pos), Point2.y(pos)));
            }else{
                //build
                if(from == Blocks.air && world.tile(pos).block() != to){
                    player.unit().addBuild(new BuildPlan(Point2.x(pos), Point2.y(pos), 0, to, null));
                }else if(world.tile(pos).build != null && world.tile(pos).block() != to){
                    player.unit().addBuild(new BuildPlan(Point2.x(pos), Point2.y(pos), world.tile(pos).build.rotation, to, world.tile(pos).build.config()));
                }
            }
        });
    }

    public void setupSelect(){
        selectTable.clear();
        selectTable.addCloseButton();
        selectTable.background(Styles.black6);
        selectTable.update(() -> {
            selectTable.setPositionInScreen(this.x, this.y - selectTable.getPrefHeight());
            if(!this.shown && !this.visible) selectTable.hide();
        });

        switch(showSubTableID){
            //block selection
            case 0 -> {
                if(!MI2USettings.getBool("worldDataUpdate")) WorldData.scanWorld();
                selectTable.button("@minimap.finder.showBlockNames", textbtoggle, null).height(36f).growX().with(funcSetTextb).with(b -> {
                    b.clicked(() -> {
                        withName = !withName;
                        setupSelect();
                    });
                    b.setChecked(withName);
                }).minWidth(36f);

                selectTable.row();
                selectTable.pane(t -> {
                    t.defaults().fillX().left().uniform();
                    int i = 0;
                    for(var block : content.blocks()){
                        if(WorldData.countBlock(block, null) <= 0) continue;
                        if(withName){
                            t.button("" + block.localizedName, new TextureRegionDrawable(block.uiIcon), textb, 24f,() -> {
                                find = block;
                                finder.findTarget = find;
                                finder.findIndex = 0;
                            }).with(funcSetTextb);
                        }else{
                            t.button(b -> b.stack(new Image(block.uiIcon), new Label(""){{
                                int count = WorldData.countBlock(block, team);
                                this.setText(count == 0 ? "" : count + "");
                                this.setFillParent(true);
                                this.setFontScale(count >= 1000 ? 0.5f : 0.8f);
                                this.setAlignment(Align.bottomRight);
                            }}), textb,() -> {
                                find = block;
                                finder.findTarget = find;
                                finder.findIndex = 0;
                            }).size(32f).pad(2f);
                        }
                        if(i++ >= (withName ? 3 : 10)){
                            t.row();
                            i = 0;
                        }
                    }
                }).maxHeight(300f).update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)) {
                        p.requestScroll();
                    }else if(p.hasScroll()){
                        Core.scene.setScrollFocus(null);
                    }
                });
            }

            //replace block selection
            case 1 -> {
                if(!MI2USettings.getBool("worldDataUpdate")) WorldData.scanWorld();
                selectTable.button("@minimap.finder.showBlockNames", textbtoggle, null).height(36f).growX().with(funcSetTextb).with(b -> {
                    b.clicked(() -> {
                        withName = !withName;
                        setupSelect();
                    });
                    b.setChecked(withName);
                }).minWidth(36f);

                selectTable.row();
                selectTable.pane(t -> {
                    t.defaults().fillX().left().uniform();
                    int i = 0;
                    for(var block : content.blocks()){
                        if(!block.canReplace(find) && block != Blocks.air) continue;
                        //if(block.isHidden() && (!state.rules.infiniteResources || !state.isEditor())) continue;
                        if(withName){
                            t.button("" + block.localizedName, new TextureRegionDrawable(block.uiIcon), textb, 24f,() -> {
                                replace = block;
                            }).with(funcSetTextb);
                        }else{
                            t.button(b -> b.stack(new Image(block.uiIcon), new Label(""){{
                                int count = WorldData.countBlock(block, team);
                                this.setText(count == 0 ? "" : count + "");
                                this.setFillParent(true);
                                this.setFontScale(count >= 1000 ? 0.5f : 0.8f);
                                this.setAlignment(Align.bottomRight);
                            }}), textb,() -> {
                                replace = block;
                            }).size(32f).pad(2f);
                        }
                        if(i++ >= (withName ? 3 : 10)){
                            t.row();
                            i = 0;
                        }
                    }
                }).maxHeight(300f).update(p -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e != null && e.isDescendantOf(p)) {
                        p.requestScroll();
                    }else if(p.hasScroll()) {
                        Core.scene.setScrollFocus(null);
                    }
                });
            }

            //team selection
            case 2 -> {
                selectTable.button("" + Iconc.map, textb, () -> {
                    team = null;
                }).minSize(titleButtonSize * 2f).disabled(b -> team == null).with(b -> {
                    b.getLabel().setWrap(false);
                });
                int j = 1;
                for(int i = 0; i < 256; i++){
                    var team0 = Team.all[i];
                    if(team0.data().buildings.size > 0){
                        selectTable.button(team0.localized(), textb, () -> {
                            team = team0;
                        }).minSize(titleButtonSize * 2f).disabled(b -> team == team0).with(b -> {
                            b.getLabel().setWrap(false);
                            b.getLabel().setColor(team0.color);
                        });
                    }
                    if(++j < 4) continue;
                    selectTable.row();
                    j = 0;
                }
            }
        }

        selectTable.popup();
    }
}
