package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.struct.*;
import mi2u.struct.WorldData.*;
import mi2u.ui.elements.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class WorldFinderMindow extends Mindow2{
    protected WorldFinder finder = new WorldFinder();

    @Nullable
    public Team team;
    public Block find = Blocks.air, replace = Blocks.air;

    public Table searchSelectBlockTable = new Table(), replaceSelectBlockTable = new Table(), teamSelectTable = new Table();
    public boolean searchSelectBlock, replaceSelectBlock, teamSelect;

    public boolean searchEffects = true;
    MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis();

    public WorldFinderMindow(){
        super("WorldFinder");
        setVisibleInGame();
        hasCloseButton = true;

        titlePane.defaults().height(buttonSize);
        titlePane.button(Iconc.effect + "", textbtoggle, () -> searchEffects = !searchEffects).checked(b -> searchEffects).size(buttonSize).with(b -> MI2Utils.tooltip(b, Core.bundle.get("worldfinder.effects")));
        titlePane.add("@" + name + ".MI2U").labelAlign(Align.center).growX();

        Events.run(EventType.Trigger.update, () -> {
            if(control.input.block != null && Core.input.keyDown(MBinding.ctrlUI) && Core.input.keyDown(MBinding.uiPopWorldFinder)){
                find = control.input.block;
                minimized = false;
                minimize();
                addTo(Core.scene.root);
                forceSetPosition(Core.input.mouseX(), Core.input.mouseY());
            }

            if(searchEffects && hasParent() && timer.get(500)){
                int pos = finder.findNext(0);
                if(pos != -1) Fx.tapBlock.at(Point2.x(pos) * tilesize, Point2.y(pos) * tilesize);
            }
        });
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();

        cont.add(new MCollapser(t -> teamSelectTable = t, !teamSelect).setCollapsed(true, () -> !teamSelect));

        cont.row();

        //查找
        cont.table(t -> {
            t.defaults().height(buttonSize).minWidth(buttonSize).pad(2f);

            t.add(Core.bundle.get("worldfinder.find")).minWidth(2 * buttonSize);
            t.button(b -> {
                b.add(new CombinationIcon(c -> c.image(() -> find.uiIcon).scaling(Scaling.fit).pad(2f)).bottomRight(c -> c.add("").update(l -> {
                    int count = WorldData.countBlock(find, team);
                    l.setText(count + "");
                    l.setFontScale(count >= 1000 ? 0.5f : 0.8f);
                    l.setColor(l.hasMouse() ? Pal.accent : Color.white);
                }).labelAlign(Align.bottomRight).grow()));
            }, textbtoggle, () -> {
                searchSelectBlock = !searchSelectBlock;
                if(searchSelectBlockTable != null) buildSearchSelect(searchSelectBlockTable);
            }).with(b -> MI2Utils.tooltip(b, tt -> tt.label(() -> find.localizedName))).checked(b -> searchSelectBlock).size(buttonSize);

            t.button("", textbtoggle, () -> {
                teamSelect = !teamSelect;
                if(teamSelectTable != null) buildTeamSelect(teamSelectTable);
            }).with(b -> {
                var l = b.getLabel();
                l.update(() -> {
                    l.setText(Core.bundle.formatString("[[{0}]", team == null ? Iconc.map : team.localized()));
                    l.setColor(team == null ? Color.white : team.color);
                });
            }).with(funcSetTextb).disabled(tb -> !finder.findTarget.hasBuilding()).checked(b -> teamSelect);

            t.button(Iconc.play + "", textb, () -> {
                finder.team = team;
                finder.findTarget = find;
                int pos = finder.findNext();
                if(pos != -1)  InputUtils.panStable(pos);
            }).with(funcSetTextb);

            t.add("#").minWidth(0);

            t.field("", s -> finder.findIndex = Mathf.clamp(Strings.parseInt(s), 0, WorldData.countBlock(find, team) - 1)).disabled(tb -> WorldData.countBlock(find, team) == 0).update(tf -> {
                if(!tf.hasKeyboard()) tf.setText(finder.findIndex + "");
            });
        }).left();

        cont.row();

        cont.add(new MCollapser(t -> searchSelectBlockTable = t, !searchSelectBlock).setCollapsed(true, () -> !searchSelectBlock));

        cont.row();

        //替换
        cont.table(t -> {
            t.defaults().height(buttonSize).minWidth(buttonSize).pad(2f);
            t.add(Core.bundle.get("worldfinder.replace")).minWidth(2 * buttonSize);
            t.defaults().size(buttonSize);

            t.button(b -> {
                b.image(() -> replace.uiIcon).scaling(Scaling.fit).pad(2f);
                //b.label(() -> WorldData.countBlock(find, team) + "").labelAlign(Align.right).width(0.1f).fontScale(0.6f);
            }, textbtoggle, () -> {
                replaceSelectBlock = !replaceSelectBlock;
                if(replaceSelectBlockTable != null) buildReplaceSelect(replaceSelectBlockTable);
            }).with(b -> MI2Utils.tooltip(b, tt -> tt.label(() -> replace.localizedName))).checked(b -> replaceSelectBlock);

            t.button(b -> {
                b.add(new CombinationIcon(c -> c.add("#")).bottomRight(c -> c.add("" + Iconc.play).fontScale(0.8f).labelAlign(Align.bottomRight).get().setColor(Pal.accent))).grow();
            }, textb, () -> {
                planReplace(find, replace, finder.findNext(0));
                finder.findNext();
            }).with(b -> MI2Utils.tooltip(b, Core.bundle.get("worldfinder.replace.next")));

            t.button(b -> {
                b.add(new CombinationIcon(c -> c.add("" + Iconc.unitPoly)).bottomRight(c -> c.add(Iconc.fill + " " + Iconc.play).fontScale(0.8f).labelAlign(Align.bottomRight).get().setColor(Pal.accent))).grow();
            }, textb, () -> {
                planReplace(find, replace, buildingRange);
            }).with(b -> MI2Utils.tooltip(b, Core.bundle.get("worldfinder.replace.buildrange")));;

            t.button(b -> {
                b.add(new CombinationIcon(c -> c.add("" + Iconc.map)).bottomRight(c -> c.add(Iconc.fill + " " + Iconc.play).fontScale(0.8f).labelAlign(Align.bottomRight).get().setColor(Pal.accent))).grow();
            }, textb, () -> {
                planReplace(find, replace, -1f);
            }).with(b -> MI2Utils.tooltip(b, Core.bundle.get("worldfinder.replace.map")));;
        }).left();

        cont.row();

        cont.add(new MCollapser(t -> replaceSelectBlockTable = t, !replaceSelectBlock).setCollapsed(true, () -> !replaceSelectBlock));
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

    public void planReplace(Block from, Block to, int pos){
        if(pos == -1) return;
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
    }

    public void buildSearchSelect(Table table){
        WorldData.scanWorld();
        table.clear();

        table.pane(p -> {
            p.defaults().fillX().left().uniform().size(buttonSize).pad(4f);
            int i = 0;
            for(var block : content.blocks()){
                if(WorldData.countBlock(block, null) <= 0) continue;
                p.button(b -> b.stack(new Image(block.uiIcon).setScaling(Scaling.fit), new Label(""){{
                    int count = WorldData.countBlock(block, team);
                    this.setText(count == 0 ? "" : count + "");
                    this.setFillParent(true);
                    this.setFontScale(count >= 1000 ? 0.5f : 0.8f);
                    this.setAlignment(Align.bottomRight);
                }}), textb,() -> {
                    find = block;
                    finder.findTarget = find;
                    finder.findIndex = 0;
                    replace = Blocks.air;
                    buildReplaceSelect(replaceSelectBlockTable);
                }).with(b -> MI2Utils.tooltip(b, block.localizedName));

                if(i++ >= 6){
                    p.row();
                    i = 0;
                }
            }
        }).maxHeight(200f).update(p -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(p)) {
                p.requestScroll();
            }else if(p.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    public void buildReplaceSelect(Table table){
        table.clear();

        table.pane(t -> {
            t.defaults().fillX().left().uniform().size(buttonSize).pad(4f);
            int i = 0;
            for(var block : content.blocks()){
                if(!block.canReplace(find) && block != Blocks.air) continue;
                //if(block.isHidden() && (!state.rules.infiniteResources || !state.isEditor())) continue;
                t.button(b -> b.stack(new Image(block.uiIcon).setScaling(Scaling.fit), new Label(""){{
                    int count = WorldData.countBlock(block, team);
                    this.setText(count == 0 ? "" : count + "");
                    this.setFillParent(true);
                    this.setFontScale(count >= 1000 ? 0.5f : 0.8f);
                    this.setAlignment(Align.bottomRight);
                }}), textb,() -> {
                    replace = block;
                }).with(b -> MI2Utils.tooltip(b, block.localizedName));;
                if(i++ >= 6){
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

    public void buildTeamSelect(Table table){
        table.clear();

        table.pane(t -> {
            t.defaults().fillX().left().uniform().height(buttonSize).minWidth(buttonSize).pad(2f);
            t.button("" + Iconc.map, textb, () -> {
                team = null;
            }).disabled(b -> team == null).with(b -> {
                b.getLabel().setWrap(false);
            });
            int j = 1;
            for(int i = 0; i < 256; i++){
                var team0 = Team.all[i];
                if(team0.data().buildings.size > 0){
                    t.button(team0.localized(), textb, () -> {
                        team = team0;
                    }).disabled(b -> team == team0).with(b -> {
                        b.getLabel().setWrap(false);
                        b.getLabel().setColor(team0.color);
                    });
                }
                if(++j < 3) continue;
                t.row();
                j = 0;
            }
        });
    }

    @Override
    public void addTo(Group newParent){
        WorldData.scanWorld();
        super.addTo(newParent);
    }
}
