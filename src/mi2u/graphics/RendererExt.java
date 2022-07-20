package mi2u.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mi2u.*;
import mi2u.io.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.Fonts;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;

import java.lang.reflect.Field;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

/**
 * Modify and add additional render
 */
public class RendererExt{

    protected static Interval interval = new Interval();
    protected static ObjectMap<Unit, Vec2> players = new ObjectMap<Unit, Vec2>();
    protected static Seq<Unit> hiddenUnit = new Seq<>();

    public static void initBase(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            players.clear();
            hiddenUnit.clear();
        });

        Events.run(EventType.Trigger.draw, () -> {
            players.each((u, v) -> {if(u == null||!u.isPlayer()||!u.isValid()) players.remove(u);});
            drawBase();
        });

        Events.run(EventType.Trigger.update, () -> {
            fullAI.unit(player.unit());
            fullAI.updateUnit();
        });
    }

    public static void drawBase(){
        if(!state.isGame()) return;
        if(!MI2USettings.getBool("disableUnit", false)){
            hiddenUnit.select(Healthc::isValid).each(u -> Groups.draw.add(u));
            hiddenUnit.clear();
        }

        Groups.draw.each(d -> {
            if(d instanceof Decal && MI2USettings.getBool("disableWreck", false)) d.remove();
            if(d instanceof Unit u){
                if(MI2USettings.getBool("disableUnit", false)){
                    Groups.draw.remove(u);
                    hiddenUnit.add(u);
                }else{
                    drawUnit(u);
                }
            }
            if(d instanceof Bullet b && MI2USettings.getBool("disableBullet", false)) Groups.draw.remove(b);
        });

        drawZoneShader();
        Seq<Tile> tiles = MI2Utils.getValue(renderer.blocks, "tileview");
        if(tiles != null){
            if(MI2USettings.getBool("disableBuilding", false)) tiles.clear();
            boolean enOverdriveZone = MI2USettings.getBool("enOverdriveZone", false), enMenderZone = MI2USettings.getBool("enMenderZone", false),
            enBlockHpBar = MI2USettings.getBool("enBlockHpBar", true);

            for(var tile : tiles){
                if(tile.build == null) continue;
                if(enBlockHpBar) drawBlockHpBar(tile.build);
                if(enDistributionReveal) drawBlackboxBuilding(tile.build);
                if(enOverdriveZone && tile.build instanceof OverdriveProjector.OverdriveBuild odb) drawOverDriver(odb);
                if(enMenderZone && tile.build instanceof MendProjector.MendBuild mb) drawMender(mb);
            }

        }
        if(MI2USettings.getBool("enSpawnZone", true)) drawSpawnPoint();

        Draw.reset();
    }

    public static void drawUnit(Unit unit){
        //Draw aim point
        if(unit.isPlayer() && MI2USettings.getBool("enPlayerCursor", false) && Mathf.len(unit.aimX - unit.x, unit.aimY - unit.y) < 4800f){
            if(players.get(unit) != null){
                players.get(unit).lerp(unit.aimX, unit.aimY, 0.4f);
            }else{
                players.put(unit, new Vec2(unit.aimX, unit.aimY));
            }
            Vec2 v = MI2UTmp.v2.set(players.get(unit));
            Rect tmpRect = MI2UTmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);
            if(tmpRect.contains(v.x, v.y) || tmpRect.contains(unit.x, unit.y)){
                Draw.reset();
                Draw.z(Layer.flyingUnit + 2f);
                if(unit.isShooting()){
                    Draw.color(1f, 0.2f, 0.2f, 0.8f);
                }else{
                    Draw.color(1f, 1f, 1f, 0.4f);
                }
                if(unit.mounts().length == 0){
                    Lines.dashLine(v.x, v.y, unit.x, unit.y, 40);
                }else{
                    for(WeaponMount m : unit.mounts()){
                        if(Mathf.len(m.aimX - unit.x - m.weapon.x, m.aimY - unit.y - m.weapon.y) < 1800f){
                            if(m.weapon.controllable){
                                Lines.dashLine(v.x, v.y, unit.x + Mathf.cos((Mathf.angle(m.weapon.x, m.weapon.y) + unit.rotation() - 90f) / 180f * Mathf.pi) * Mathf.len(m.weapon.x, m.weapon.y), unit.y + Mathf.sin((Mathf.angle(m.weapon.x, m.weapon.y) + unit.rotation() - 90f) / 180f * Mathf.pi) * Mathf.len(m.weapon.x, m.weapon.y), 40);
                            }else{
                                Lines.dashLine(m.aimX, m.aimY, unit.x + Mathf.cos((Mathf.angle(m.weapon.x, m.weapon.y) + unit.rotation() - 90f) / 180f * Mathf.pi) * Mathf.len(m.weapon.x, m.weapon.y), unit.y + Mathf.sin((Mathf.angle(m.weapon.x, m.weapon.y) + unit.rotation() - 90f) / 180f * Mathf.pi) * Mathf.len(m.weapon.x, m.weapon.y), 40);
                            }
                        }
                    }
                }
                Draw.z(Layer.playerName);
                Drawf.target(v.x, v.y, 4f, 0.6f, Pal.remove);
            }

            if(tmpRect.contains(v.x, v.y) && !tmpRect.contains(unit.x, unit.y)){
                Draw.z(Layer.playerName);

                if(!unit.getPlayer().isLocal()){
                    Player p = unit.getPlayer();
                    Font font = Fonts.def;
                    GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                    final float nameHeight = 6f;

                    boolean ints = font.usesIntegerPositions();
                    font.setUseIntegerPositions(false);
                    font.getData().setScale(0.25f / Scl.scl(1f));
                    layout.setText(font, p.name);

                    Draw.color(unit.team.color, 0.3f);
                    Fill.rect(v.x, v.y + nameHeight - layout.height / 2, layout.width + 2, layout.height + 3);
                    Draw.color();
                    font.setColor(p.color);
                    font.draw(p.name, v.x, v.y + nameHeight, 0, Align.center, false);

                    if(p.admin){
                        float s = 3f;
                        Draw.color(p.color.r * 0.5f, p.color.g * 0.5f, p.color.b * 0.5f, 1f);
                        Draw.rect(Icon.adminSmall.getRegion(), v.x + layout.width / 2f + 2 + 1, v.y + nameHeight - 1.5f, s, s);
                        Draw.color(p.color);
                        Draw.rect(Icon.adminSmall.getRegion(), v.x + layout.width / 2f + 2 + 1, v.y + nameHeight - 1f, s, s);
                    }

                    Pools.free(layout);
                    font.getData().setScale(1f);
                    font.setColor(Color.white);
                    font.setUseIntegerPositions(ints);
                }
            }
            Draw.reset();
        }

        if(Math.abs(unit.x - Core.camera.position.x) <= (Core.camera.width / 2) && Math.abs(unit.y - Core.camera.position.y) <= (Core.camera.height / 2)){
            //display healthbar by MI2
            Draw.z(Layer.shields + 6f);
            if(MI2USettings.getBool("enUnitHpBar")){
                drawUnitHpBar(unit);
            }

            //display logicAI info by MI2
            if(unit.controller() instanceof LogicAI logicai){
                Draw.reset();
                //if(Core.settings.getBool("unitLogicMoveLine") && Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) <= 1200f){
                if(MI2USettings.getBool("enUnitLogic") && Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) <= 1200f){
                    Lines.stroke(1f);
                    Draw.color(0.2f, 0.2f, 1f, 0.9f);
                    Lines.dashLine(unit.x, unit.y, logicai.moveX, logicai.moveY, (int)(Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) / 8));
                    Lines.dashCircle(logicai.moveX, logicai.moveY, logicai.moveRad);
                    Draw.reset();
                }

                //logicai timers
                //if(Core.settings.getBool("unitLogicTimerBars")){
                if(MI2USettings.getBool("enUnitLogicTimer")){
                    Lines.stroke(2f);
                    Draw.color(Pal.heal);
                    Lines.line(unit.x - (unit.hitSize() / 2f), unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f), unit.y + unit.hitSize() * (logicai.controlTimer / LogicAI.logicControlTimeout - 0.5f));

                    Lines.stroke(2f);
                    Draw.color(Pal.items);
                    Lines.line(unit.x - (unit.hitSize() / 2f) - 1f, unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f) - 1f, unit.y + unit.hitSize() * (logicai.itemTimer / LogicAI.transferDelay - 0.5f));

                    Lines.stroke(2f);
                    Draw.color(Pal.items);
                    Lines.line(unit.x - (unit.hitSize() / 2f) - 1.5f, unit.y - (unit.hitSize() / 2f), unit.x - (unit.hitSize() / 2f) - 1.5f, unit.y + unit.hitSize() * (logicai.payTimer / LogicAI.transferDelay - 0.5f));

                    Draw.reset();
                }
            }

            //v7 rts pathfind render, making your device a barbecue.
            //Pathfind Renderer
            if(MI2USettings.getBool("enUnitPath")){
                if(unit.isCommandable() && unit.controller() instanceof CommandAI ai && ai.targetPos != null){
                    Draw.reset();
                    Draw.z(Layer.power - 4f);
                    Lines.stroke(1.5f);

                    try{
                        Tile tile = unit.tileOn();
                        ObjectMap requests = MI2Utils.getValue(controlPath, "requests");
                        Object req = requests.get(unit);
                        IntSeq result = MI2Utils.getValue(req, "result");
                        int start = MI2Utils.getValue(req, "rayPathIndex");
                        for(int tileIndex = start; tileIndex < result.size; tileIndex++){
                            Tile nextTile = world.tiles.geti(result.get(tileIndex));
                            if(nextTile == null) break;
                            if(!Core.camera.bounds(MI2UTmp.r1).contains(tile.worldx(), tile.worldy()) && !Core.camera.bounds(MI2UTmp.r1).contains(nextTile.worldx(), nextTile.worldy())) continue;  //Skip paths outside screen
                            if(nextTile == tile) break;
                            Draw.color(unit.team.color, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                            if(Mathf.len(nextTile.worldx() - tile.worldx(), nextTile.worldy() - tile.worldy()) > 4000f) break;
                            Lines.dashLine(tile.worldx(), tile.worldy(), nextTile.worldx(), nextTile.worldy(), (int)(Mathf.len(nextTile.worldx() - tile.worldx(), nextTile.worldy() - tile.worldy()) / 4f));
                            tile = nextTile;
                        }
                    }catch(Exception ignore){
                        boolean move = controlPath.getPathPosition(unit, MI2Utils.getValue(ai, "pathId"), ai.targetPos, MI2UTmp.v1);
                        if(move){
                            Draw.color(unit.team.color, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                            Lines.dashLine(unit.x, unit.y, MI2UTmp.v1.x, MI2UTmp.v1.y, (int)(Mathf.len(MI2UTmp.v1.x - unit.x, MI2UTmp.v1.y - unit.y) / 4f));
                        }
                    }

                    if(ai.targetPos != null){
                        Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                        Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f);
                        Draw.color(Color.white, 0.4f);
                        MI2UTmp.v1.set(lineDest).sub(unit).limit(1000f);
                        Lines.line(unit.x, unit.y, unit.x + MI2UTmp.v1.x, unit.y + MI2UTmp.v1.y);
                    }

                }else{
                    Draw.reset();
                    Draw.z(Layer.power - 4f);
                    Tile tile = unit.tileOn();
                    for(int tileIndex = 1; tileIndex <= 40; tileIndex++){
                        Tile nextTile = pathfinder.getTargetTile(tile, pathfinder.getField(unit.team, unit.pathType(), Pathfinder.fieldCore));
                        if(nextTile == null) break;
                        if(nextTile == tile) break;
                        Lines.stroke(1.5f);
                        Draw.color(unit.team.color, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                        Lines.dashLine(tile.worldx(), tile.worldy(), nextTile.worldx(), nextTile.worldy(), (int)(Mathf.len(nextTile.worldx() - tile.worldx(), nextTile.worldy() - tile.worldy()) / 4f));
                        tile = nextTile;
                    }
                }
            }

            Draw.reset();
        }
    }

    //TODO set a Runnable list.
    public static void drawBlockHpBar(Building build){
        Draw.z(Layer.shields + 3f);
        float width = 0.8f, halfwidth = width / 2f;
        float offy = -build.hitSize() * 0.4f;
        float barLength, x, y, h, w;

        if(build.health < build.maxHealth){
            if(build.hitTime > 0f){
                Lines.stroke(4f + Mathf.lerp(0f, 2f, Mathf.clamp(build.hitTime)));
                Draw.color(Color.white, Mathf.lerp(0.1f, 1f, Mathf.clamp(build.hitTime)));
                Lines.line(build.x - build.hitSize() * halfwidth, build.y + offy, build.x + build.hitSize() * halfwidth, build.y + offy);
            }

            //Lines.stroke(4f);
            //Draw.color(build.team.color, 0.5f);
            //Lines.line(build.x - build.hitSize() * halfwidth, build.y + offy, build.x + build.hitSize() * halfwidth, build.y + offy);

            Draw.color((build.health > 0 ? Pal.health:Color.gray), 0.8f);
            //Lines.stroke(2);
            /*Lines.line(
                    build.x - build.hitSize() * halfwidth, build.y + offy,
                    build.x + build.hitSize() * ((build.health > 0 ? build.health : Mathf.maxZero(build.maxHealth + build.health)) / build.maxHealth * width - halfwidth), build.y + offy);
            */
            barLength = Mathf.clamp(build.health / build.maxHealth);

            x = build.x - (1f - barLength) * halfwidth * build.hitSize();
            y = build.y + offy;
            h = 2f;
            w = width * barLength * build.hitSize();
            Fill.rect(x, y, w, h);
            offy += 2f;
        }

        if(build instanceof ShieldWall.ShieldWallBuild sw && sw.shield > 0f){
            Draw.color(Pal.shield, 0.8f);
            barLength = Mathf.clamp(sw.shield / ((ShieldWall)sw.block).shieldHealth);

            x = sw.x - (1f - barLength) * halfwidth * sw.hitSize();
            y = sw.y + offy;
            h = 2f;
            w = width * barLength * sw.hitSize();
            Fill.rect(x, y, w, h);
            offy += 2f;
        }

        if(build instanceof UnitFactory.UnitFactoryBuild uf){
            Draw.color(Pal.accent, 0.8f);
            barLength = uf.fraction();

            x = uf.x - (1f - barLength) * halfwidth * uf.hitSize();
            y = uf.y + offy;
            h = 2f;
            w = width * barLength * uf.hitSize();
            Fill.rect(x, y, w, h);
            offy += 2f;

            drawText((uf.currentPlan == -1 ? "":((UnitFactory)uf.block).plans.get(uf.currentPlan).unit.emoji()) + (Strings.autoFixed(uf.fraction() * 100f, 1) + "% | " + (uf.currentPlan == -1 ? Core.bundle.get("none") : Strings.autoFixed((((UnitFactory)uf.block).plans.get(uf.currentPlan).time - uf.progress) / (60f * state.rules.unitBuildSpeed(uf.team) * uf.timeScale()), 1))), uf.x, uf.y + offy, Pal.accent, uf.block.size > 3 ? 1.0f : 0.8f, Align.center);
            offy += 2f;
        }

        if(build instanceof Reconstructor.ReconstructorBuild uf){
            Draw.color(Pal.accent, 0.8f);
            barLength = uf.fraction();

            x = uf.x - (1f - barLength) * halfwidth * uf.hitSize();
            y = uf.y + offy;
            h = 2f;
            w = width * barLength * uf.hitSize();
            Fill.rect(x, y, w, h);
            offy += 2f;

            drawText((uf.unit() == null ? "":uf.unit().emoji()) + (Strings.autoFixed(uf.fraction() * 100f, 1) + "% | " + Strings.autoFixed((((Reconstructor)uf.block).constructTime - uf.progress) / (60f * state.rules.unitBuildSpeed(uf.team) * uf.timeScale()), 1)), uf.x, uf.y + offy, Pal.accent, uf.block.size > 3 ? 1.0f : 0.8f, Align.center);
            offy += 2f;
        }

        if(build instanceof UnitAssembler.UnitAssemblerBuild uf){
            Draw.color(Pal.accent, 0.8f);
            barLength = uf.progress;

            x = uf.x - (1f - barLength) * halfwidth * uf.hitSize();
            y = uf.y + offy;
            h = 2f;
            w = width * barLength * uf.hitSize();
            Fill.rect(x, y, w, h);
            offy += 2f;

            drawText((uf.unit() == null ? "":uf.unit().emoji()) + (Strings.autoFixed(barLength * 100f, 1) + "% | " + Strings.autoFixed(uf.plan().time * (1 - uf.progress) / (60f * state.rules.unitBuildSpeed(uf.team) * uf.timeScale()), 1)), uf.x, uf.y + offy, Pal.accent, uf.block.size > 3 ? 1.0f : 0.8f, Align.center);
            offy += 2f;
        }
    }

    public static void drawUnitHpBar(Unit unit){
        float width = 1.2f, halfwidth = width / 2f;

        if(unit.hitTime > 0f){
            Lines.stroke(4f + Mathf.lerp(0f, 2f, Mathf.clamp(unit.hitTime)));
            Draw.color(Color.white, Mathf.lerp(0.1f, 1f, Mathf.clamp(unit.hitTime)));
            Lines.line(unit.x - unit.hitSize * halfwidth, unit.y + (unit.hitSize / 2f), unit.x + unit.hitSize * halfwidth, unit.y + (unit.hitSize / 2f));
        }

        Lines.stroke(4f);
        Draw.color(unit.team.color, 0.5f);
        Lines.line(unit.x - unit.hitSize * halfwidth, unit.y + (unit.hitSize / 2f), unit.x + unit.hitSize * halfwidth, unit.y + (unit.hitSize / 2f));

        Draw.color((unit.health > 0 ? Pal.health:Color.gray), 0.8f);
        Lines.stroke(2);
        Lines.line(
                unit.x - unit.hitSize * halfwidth, unit.y + (unit.hitSize / 2f),
                unit.x + unit.hitSize * ((unit.health > 0 ? unit.health : Mathf.maxZero(unit.maxHealth + unit.health)) / unit.maxHealth * width - halfwidth), unit.y + (unit.hitSize / 2f));

        if(unit.shield > 0){
            for(int didgt = 1; didgt <= Mathf.digits((int)(unit.shield / unit.maxHealth)) + 1; didgt++){
                Draw.color(Pal.shield, 0.8f);
                float barLength = Mathf.mod(unit.shield / unit.maxHealth, Mathf.pow(10f, (float)didgt - 1f)) / Mathf.pow(10f, (float)didgt - 1f);
                if(didgt > 1){
                    float y = unit.y + unit.hitSize / 2f + didgt * 2f;
                    float h = 2f;
                    int counts = Mathf.floor(barLength * 10f);
                    for(float i = 1; i <= counts; i++){
                        Fill.rect(unit.x - 0.55f * unit.hitSize + (i - 1f) * 0.12f * unit.hitSize, y, 0.1f * unit.hitSize, h);
                    }
                }else{
                    float x = unit.x - (1f - barLength) * halfwidth * unit.hitSize;
                    float y = unit.y + unit.hitSize / 2f + didgt * 2f;
                    float h = 2f;
                    float w = width * barLength * unit.hitSize;
                    Fill.rect(x, y, w, h);
                }
            }
        }

        Draw.reset();

        float index = 0f;
        int columns = Mathf.floor(unit.hitSize * width / 4f);
        for(StatusEffect eff : content.statusEffects()){
            if(eff == StatusEffects.none) continue;
            if(unit.hasEffect(eff)){
                Draw.alpha(unit.getDuration(eff) < 180f ? 0.3f + 0.7f * Math.abs(Mathf.sin(Time.time / 20f)) : 1f);
                Draw.rect(eff.uiIcon,
                        unit.x - unit.hitSize * halfwidth + 2f + 4f * Mathf.mod(index, columns),
                        unit.y + (unit.hitSize / 2f) + 3f + 5f * Mathf.floor(index / columns),
                        eff.uiIcon.width / (float)eff.uiIcon.height * 5f, 5f);
                index++;
            }
        }

        if(unit instanceof PayloadUnit pu && pu.payloads != null){
            Draw.alpha(0.9f);
            //the smaller pui is, the further payload is in drop list. And those further ones can be slightly covered.
            float fullIconCells = width * unit.hitSize / pu.payloads.size < 6f ? Mathf.floor(unit.hitSize * width * 0.5f / 6f) : 100f;
            for(int pui = 0; pui < pu.payloads.size; pui++){
                var p = pu.payloads.get(pu.payloads.size - 1 - pui);
                if(p == null) continue;
                Draw.rect(p.icon(),
                        unit.x + (1f + (pui > fullIconCells ? unit.hitSize * -halfwidth + fullIconCells * 6f + (pui - fullIconCells) * (unit.hitSize * width - fullIconCells * 6f) / (pu.payloads.size - fullIconCells) : unit.hitSize * -halfwidth + pui * 6f)),
                        unit.y + (unit.hitSize / 2f) - 4f,
                        6f, 6f);
            }
        }
    }

    public static void drawBlackboxBuilding(Building b){
        if(b instanceof Junction.JunctionBuild jb) drawJunciton(jb);
        if(b instanceof ItemBridge.ItemBridgeBuild ib) drawItemBridge(ib);
        if(b instanceof BufferedItemBridge.BufferedItemBridgeBuild bb) drawBufferedItemBridge(bb);
        if(b instanceof Unloader.UnloaderBuild ub) drawUnloader(ub);
        if(b instanceof Router.RouterBuild rb) drawRouter(rb);
    }

    public static void drawZoneShader(){
        if(Core.settings.getBool("animatedshields") && MI2USettings.getBool("enOverdriveZone", false) && MI2UShaders.odzone != null){
            Draw.drawRange(91.1f, 0.02f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                renderer.effectBuffer.end();
                renderer.effectBuffer.blit(MI2UShaders.odzone);
            });
        }
        if(Core.settings.getBool("animatedshields") && MI2USettings.getBool("enMenderZone", false) && MI2UShaders.mdzone != null){
            Draw.drawRange(91.2f, 0.02f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                renderer.effectBuffer.end();
                renderer.effectBuffer.blit(MI2UShaders.mdzone);
            });
        }
    }

    public static void drawOverDriver(OverdriveProjector.OverdriveBuild odb){
        OverdriveProjector block = (OverdriveProjector)odb.block();
        Draw.color(block.baseColor, block.phaseColor, odb.phaseHeat);
        Draw.z(91.1f);
        Draw.alpha(Core.settings.getBool("animatedshields")?0.6f:0.2f);
        Fill.circle(odb.x, odb.y, block.range + odb.phaseHeat * block.phaseRangeBoost);

        Lines.stroke(2f);
        Draw.alpha(1f);
        Lines.circle(odb.x, odb.y, block.range + odb.phaseHeat * block.phaseRangeBoost);
    }

    public static void drawMender(MendProjector.MendBuild mb){
        if(mb.efficiency() <= 0f) return;
        MendProjector block = (MendProjector)mb.block;
        float alpha = Mathf.pow(1f - (mb.charge / block.reload), 5);
        Draw.z(91.2f);
        Draw.color(block.baseColor);
        Draw.alpha((Core.settings.getBool("animatedshields")?0.6f:0.2f) * (alpha > 0.05 ? alpha : 0f));
        Fill.circle(mb.x, mb.y, block.range + mb.phaseHeat * block.phaseRangeBoost);
    }

    public static void drawJunciton(Junction.JunctionBuild jb){
        try{
            int cap = ((Junction)jb.block).capacity;
            float speed = ((Junction)jb.block).speed;

            Field f = jb.getClass().getDeclaredField("buffer");
            f.setAccessible(true);
            DirectionalItemBuffer buffer = (DirectionalItemBuffer)f.get(jb);
            f = buffer.getClass().getDeclaredField("buffers");
            f.setAccessible(true);
            long[][] bufferbuffers = (long[][])f.get(buffer);
            f = buffer.getClass().getDeclaredField("indexes");
            f.setAccessible(true);
            int[] indexes = (int[])f.get(buffer);

            Item[][] items = new Item[4][bufferbuffers[0].length];
            for(int i = 0; i < 4; i++){
                for(int ii = 0; ii < bufferbuffers[i].length; ii++){
                    items[i][ii] = (ii < indexes[i])? content.item(BufferItem.item(bufferbuffers[i][ii])) : null;
                }
            }
            float[][] times = new float[4][bufferbuffers[0].length];
            for(int i = 0; i < 4; i++){
                for(int ii = 0; ii < bufferbuffers[i].length; ii++){
                    times[i][ii] = (ii < indexes[i])? BufferItem.time(bufferbuffers[i][ii]) : 9999999999999f;
                }
            }

            float begx, begy, endx, endy;
            for(int i = 0; i < 4; i++){
                endx = jb.x + Geometry.d4(i).x * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
                endy = jb.y + Geometry.d4(i).y * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;
                begx = jb.x - Geometry.d4(i).x * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
                begy = jb.y - Geometry.d4(i).y * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;
                if(buffer.indexes[i] > 0){
                    float loti = 0f;
                    for(int idi = 0; idi < buffer.indexes[i]; idi++){
                        if(items[i][idi] != null){
                            Draw.alpha(0.9f);
                            Draw.rect(items[i][idi].fullIcon,
                            begx + ((endx - begx) / (float)cap * Math.min(((Time.time - times[i][idi]) * jb.timeScale() / speed) * cap, cap - loti)),
                            begy + ((endy - begy) / (float)cap * Math.min(((Time.time - times[i][idi]) * jb.timeScale() / speed) * cap, cap - loti)),
                            4f, 4f);
                        }
                        loti++;
                    }
                }
            }
        }catch(Exception e){if(!interval.get(30)) return; Log.err(e.toString());}
    }

    public static void drawItemBridge(ItemBridge.ItemBridgeBuild ib){
        Draw.reset();
        Draw.z(Layer.power);
        //draw each item this bridge have
        if(ib.items != null){
            Draw.color(Color.white, 0.8f);
            int loti = 0;
            for(int iid = 0; iid < ib.items.length(); iid++){
                if(ib.items.get(iid) > 0){
                    for(int itemid = 1; itemid <= ib.items.get(iid); itemid++){
                        Draw.rect(content.item(iid).fullIcon, ib.x, ib.y - tilesize/2f + 1f + 0.6f * (float)loti, 4f, 4f);
                        loti++;
                    }
                }
            }
        }
    }

    public static void drawBufferedItemBridge(BufferedItemBridge.BufferedItemBridgeBuild bb){
        try{
            ItemBuffer buffer = MI2Utils.getValue(bb, "buffer");
            long[] bufferbuffer = MI2Utils.getValue(buffer, "buffer");
            int index = MI2Utils.getValue(buffer, "index");

            Item[] bufferItems = new Item[bufferbuffer.length];
            for(int ii = 0; ii < bufferbuffer.length; ii++){
                bufferItems[ii] = (ii < index)? content.item(Pack.leftShort(Pack.rightInt(bufferbuffer[ii]))) : null;
            }
            float[] bufferTimes = new float[bufferbuffer.length];
            for(int ii = 0; ii < bufferbuffer.length; ii++){
                bufferTimes[ii] = (ii < index)? Float.intBitsToFloat(Pack.leftInt(bufferbuffer[ii])) : 999999999f;
            }

            Tile other = world.tile(bb.link);
            float begx, begy, endx, endy;
            if(!((ItemBridge)bb.block()).linkValid(bb.tile, other)){
                begx = bb.x - tilesize / 2f;
                begy = bb.y - tilesize / 2f;
                endx = bb.x + tilesize / 2f;
                endy = bb.y - tilesize / 2f;
            }else{
                int i = bb.tile.absoluteRelativeTo(other.x, other.y);
                float ex = other.worldx() - bb.x - Geometry.d4(i).x * tilesize / 2f,
                ey = other.worldy() - bb.y - Geometry.d4(i).y * tilesize / 2f;

                begx = bb.x + Geometry.d4(i).x * tilesize / 2f;
                begy = bb.y + Geometry.d4(i).y * tilesize / 2f;
                endx = bb.x + ex;
                endy = bb.y + ey;
            }

            float loti = 0f;
            int cap = ((BufferedItemBridge)bb.block).bufferCapacity;
            float speed = ((BufferedItemBridge)bb.block).speed;
            for(int idi = 0; idi < bufferItems.length; idi++){
                if(bufferItems[idi] != null){
                    Draw.alpha(0.9f);

                    Draw.rect(bufferItems[idi].fullIcon,
                    begx + ((endx - begx) / (float)bufferItems.length * Math.min(((Time.time - bufferTimes[idi]) * bb.timeScale() / speed) * cap, cap - loti)),
                    begy + ((endy - begy) / (float)bufferItems.length * Math.min(((Time.time - bufferTimes[idi]) * bb.timeScale() / speed) * cap, cap - loti)), 4f, 4f);
                }
                loti++;
            }
        }catch(Exception e){if(!interval.get(30)) return; Log.err(e.toString());}
    }

    public static void drawUnloader(Unloader.UnloaderBuild ub){
        //ContainerStat[] possibleBlocks sorted + rotations updated on each update
        try{
            Unloader block = (Unloader)ub.block;
            Item drawItem = content.item(ub.rotations);
            Building fromb = null, tob = null;

            Field f;
            Building tmp;
            boolean toFound = false, fromFound = false;
            boolean canLoad, canUnload;
            for(Unloader.ContainerStat c : ub.possibleBlocks){
                f = Unloader.ContainerStat.class.getDeclaredField("canLoad");
                f.setAccessible(true);
                canLoad = f.getBoolean(c);
                f = Unloader.ContainerStat.class.getDeclaredField("canUnload");
                f.setAccessible(true);
                canUnload = f.getBoolean(c);
                f = Unloader.ContainerStat.class.getDeclaredField("building");
                f.setAccessible(true);
                tmp = (Building)f.get(c);
                if(!toFound && canLoad){
                    tob = tmp;
                    toFound = true;
                }
                if(!fromFound && canUnload){
                    fromb = tmp;
                    fromFound = true;
                }
            }

            Draw.color();

            if(!(drawItem == null || fromb == null ||  tob == null)){
                float x1 = 0f, x2 = 0f, y1 = 0f, y2 = 0f;
                //0> 1^ 2< 3\/
                switch(Mathf.mod(((int) Angles.angle(tob.x - ub.x, tob.y - ub.y) + 45) / 90, 4)){
                    case 0 -> {
                        x1 = ub.x + World.unconv(block.size) / 2f;
                        x2 = ub.x + World.unconv(block.size) / 2f;
                        y1 = Math.min(ub.y + World.unconv(block.size) / 2f, tob.y + World.unconv(tob.block.size) / 2f);
                        y2 = Math.max(ub.y - World.unconv(block.size) / 2f, tob.y - World.unconv(tob.block.size) / 2f);
                    }
                    case 1 -> {
                        y1 = ub.y + World.unconv(block.size) / 2f;
                        y2 = ub.y + World.unconv(block.size) / 2f;
                        x1 = Math.min(ub.x + World.unconv(block.size) / 2f, tob.x + World.unconv(tob.block.size) / 2f);
                        x2 = Math.max(ub.x - World.unconv(block.size) / 2f, tob.x - World.unconv(tob.block.size) / 2f);
                    }
                    case 2 -> {
                        x1 = ub.x - World.unconv(block.size) / 2f;
                        x2 = ub.x - World.unconv(block.size) / 2f;
                        y1 = Math.min(ub.y + World.unconv(block.size) / 2f, tob.y + World.unconv(tob.block.size) / 2f);
                        y2 = Math.max(ub.y - World.unconv(block.size) / 2f, tob.y - World.unconv(tob.block.size) / 2f);
                    }
                    case 3 -> {
                        y1 = ub.y - World.unconv(block.size) / 2f;
                        y2 = ub.y - World.unconv(block.size) / 2f;
                        x1 = Math.min(ub.x + World.unconv(block.size) / 2f, tob.x + World.unconv(tob.block.size) / 2f);
                        x2 = Math.max(ub.x - World.unconv(block.size) / 2f, tob.x - World.unconv(tob.block.size) / 2f);
                    }
                }

                Draw.z(Layer.block + 1f);

                Draw.color(Pal.placing);
                Lines.stroke(1.5f);
                Lines.line(x1, y1, x2, y2);

                switch(Mathf.mod(((int)Angles.angle(fromb.x - ub.x, fromb.y - ub.y) + 45) / 90, 4)){
                    case 0 -> {
                        x1 = ub.x + World.unconv(block.size) / 2f;
                        x2 = ub.x + World.unconv(block.size) / 2f;
                        y1 = Math.min(ub.y + World.unconv(block.size) / 2f, fromb.y + World.unconv(fromb.block.size) / 2f);
                        y2 = Math.max(ub.y - World.unconv(block.size) / 2f, fromb.y - World.unconv(fromb.block.size) / 2f);
                    }
                    case 1 -> {
                        y1 = ub.y + World.unconv(block.size) / 2f;
                        y2 = ub.y + World.unconv(block.size) / 2f;
                        x1 = Math.min(ub.x + World.unconv(block.size) / 2f, fromb.x + World.unconv(fromb.block.size) / 2f);
                        x2 = Math.max(ub.x - World.unconv(block.size) / 2f, fromb.x - World.unconv(fromb.block.size) / 2f);
                    }
                    case 2 -> {
                        x1 = ub.x - World.unconv(block.size) / 2f;
                        x2 = ub.x - World.unconv(block.size) / 2f;
                        y1 = Math.min(ub.y + World.unconv(block.size) / 2f, fromb.y + World.unconv(fromb.block.size) / 2f);
                        y2 = Math.max(ub.y - World.unconv(block.size) / 2f, fromb.y - World.unconv(fromb.block.size) / 2f);
                    }
                    case 3 -> {
                        y1 = ub.y - World.unconv(block.size) / 2f;
                        y2 = ub.y - World.unconv(block.size) / 2f;
                        x1 = Math.min(ub.x + World.unconv(block.size) / 2f, fromb.x + World.unconv(fromb.block.size) / 2f);
                        x2 = Math.max(ub.x - World.unconv(block.size) / 2f, fromb.x - World.unconv(fromb.block.size) / 2f);
                    }
                }

                Draw.color(Pal.remove);
                Lines.stroke(1.5f);
                Lines.line(x1, y1, x2, y2);

                if(ub.sortItem == null){
                    Draw.color();
                    Draw.rect(drawItem.uiIcon, ub.x, ub.y, 4f, 4f);
                }
                Draw.reset();
            }
        }catch(Exception e){if(!interval.get(30)) return; Log.err(e.toString());}
    }

    public static void drawRouter(Router.RouterBuild rb){
        Router block = (Router)rb.block;
        Building fromb = rb.lastInput == null ? null : rb.lastInput.build;
        Building tob = rb.proximity.size == 0 ? null : rb.proximity.get(((rb.rotation) % rb.proximity.size - 1 + rb.proximity.size) % rb.proximity.size);

        Draw.color();
        Draw.z(Layer.block + 1f);
        float x1 = 0f, x2 = 0f, y1 = 0f, y2 = 0f;
        if(tob != null){
            //0> 1^ 2< 3\/
            switch(Mathf.mod(((int)Angles.angle(tob.x - rb.x, tob.y - rb.y) + 45) / 90, 4)){
                case 0 -> {
                    x1 = rb.x + World.unconv(block.size) / 2f;
                    x2 = rb.x + World.unconv(block.size) / 2f;
                    y1 = Math.min(rb.y + World.unconv(block.size) / 2f, tob.y + World.unconv(tob.block.size) / 2f);
                    y2 = Math.max(rb.y - World.unconv(block.size) / 2f, tob.y - World.unconv(tob.block.size) / 2f);
                }
                case 1 -> {
                    y1 = rb.y + World.unconv(block.size) / 2f;
                    y2 = rb.y + World.unconv(block.size) / 2f;
                    x1 = Math.min(rb.x + World.unconv(block.size) / 2f, tob.x + World.unconv(tob.block.size) / 2f);
                    x2 = Math.max(rb.x - World.unconv(block.size) / 2f, tob.x - World.unconv(tob.block.size) / 2f);
                }
                case 2 -> {
                    x1 = rb.x - World.unconv(block.size) / 2f;
                    x2 = rb.x - World.unconv(block.size) / 2f;
                    y1 = Math.min(rb.y + World.unconv(block.size) / 2f, tob.y + World.unconv(tob.block.size) / 2f);
                    y2 = Math.max(rb.y - World.unconv(block.size) / 2f, tob.y - World.unconv(tob.block.size) / 2f);
                }
                case 3 -> {
                    y1 = rb.y - World.unconv(block.size) / 2f;
                    y2 = rb.y - World.unconv(block.size) / 2f;
                    x1 = Math.min(rb.x + World.unconv(block.size) / 2f, tob.x + World.unconv(tob.block.size) / 2f);
                    x2 = Math.max(rb.x - World.unconv(block.size) / 2f, tob.x - World.unconv(tob.block.size) / 2f);
                }
            }
            Draw.color(Pal.placing, 1 - rb.time);
            Lines.stroke(1.5f);
            Lines.line(x1, y1, x2, y2);
        }

        if(fromb != null){
            float rmargin = World.unconv(block.size) / 2f - 2f;
            float fmargin = World.unconv(fromb.block.size) / 2f + 2f;
            switch(Mathf.mod(((int)Angles.angle(fromb.x - rb.x, fromb.y - rb.y) + 45) / 90, 4)){
                case 0 -> {
                    x1 = rb.x + rmargin;
                    x2 = rb.x + rmargin;
                    y1 = Math.min(rb.y + rmargin, fromb.y + fmargin);
                    y2 = Math.max(rb.y - rmargin, fromb.y - fmargin);
                }
                case 1 -> {
                    y1 = rb.y + rmargin;
                    y2 = rb.y + rmargin;
                    x1 = Math.min(rb.x + rmargin, fromb.x + fmargin);
                    x2 = Math.max(rb.x - rmargin, fromb.x - fmargin);
                }
                case 2 -> {
                    x1 = rb.x - rmargin;
                    x2 = rb.x - rmargin;
                    y1 = Math.min(rb.y + rmargin, fromb.y + fmargin);
                    y2 = Math.max(rb.y - rmargin, fromb.y - fmargin);
                }
                case 3 -> {
                    y1 = rb.y - rmargin;
                    y2 = rb.y - rmargin;
                    x1 = Math.min(rb.x + rmargin, fromb.x + fmargin);
                    x2 = Math.max(rb.x - rmargin, fromb.x - fmargin);
                }
            }
            Draw.color(Pal.remove, 1 - rb.time);
            Lines.stroke(1.5f);
            Lines.line(x1, y1, x2, y2);
        }

        if(rb.lastItem != null){
            Draw.color();
            Draw.rect(rb.lastItem.uiIcon, rb.x, rb.y, 4f, 4f);
        }
        Draw.reset();
    }

    public static void drawSpawnPoint(){
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
        Draw.alpha(1f);
        Draw.z(Layer.overlayUI);
        if(state.hasSpawns()){
            float len = MI2UTmp.v1.set(Core.camera.width, Core.camera.height).len() + state.rules.dropZoneRadius;
            for(Tile tile : spawner.getSpawns()){
                if(MI2UTmp.v1.set(tile).sub(Core.camera.position).len() < len){
                    Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                }
            }
        }
        Draw.reset();
    }

    public static void drawText(String text, float x, float y, Color color, float scl,  int align){
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f / 4f / Scl.scl(1f) * scl);
        layout.setText(font, text);

        font.setColor(color);
        font.draw(text, x, y + layout.height + 1, align);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
    }
}
