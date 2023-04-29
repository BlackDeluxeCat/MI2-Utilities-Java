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
import mi2u.ui.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

/**
 * Modify and add additional render
 */
public class RendererExt{
    protected static Interval interval = new Interval();
    protected static ObjectMap<Unit, Vec2> players = new ObjectMap<>();
    protected static Seq<Unit> hiddenUnit = new Seq<>();
    public static Field itemBridgeBuffer = MI2Utils.getField(BufferedItemBridge.BufferedItemBridgeBuild.class, "buffer"),
            itemBridgeBufferBuffer = MI2Utils.getField(ItemBuffer.class, "buffer"), itemBridgeBufferIndex = MI2Utils.getField(ItemBuffer.class, "index"),
            unloaderBuilding = MI2Utils.getField(Unloader.ContainerStat.class, "building"),
            lexecTimer = MI2Utils.getField(LExecutor.class, "unitTimeouts");

    public static boolean animatedshields;
    public static boolean enPlayerCursor, enUnitHpBar, enUnitRangeZone, enOverdriveZone, enMenderZone, enTurretZone, enBlockHpBar, enDistributionReveal, drevealBridge, drevealJunction, drevealUnloader, drevealInventory, enSpawnZone, disableWreck, disableUnit, disableBuilding, disableBullet, shadow;
    public static float flashZoneAlpha;

    public static void initBase(){
        BuildingInventory.init();
        Events.on(EventType.WorldLoadEvent.class, e -> {
            players.clear();
            hiddenUnit.clear();
            TurretZoneDrawer.clear();
        });

        Events.run(EventType.Trigger.draw, () -> {
            updateSettings();
            players.each((u, v) -> {if(u == null) return; if(!u.isPlayer()||!u.isValid()) players.remove(u);});
            drawBase();
        });

        Events.run(EventType.Trigger.drawOver, () -> {
            if(MI2USettings.getBool("forceTapTile", false)) forceDrawSelect();
        });
    }

    public static void updateSettings(){
        animatedshields = Core.settings.getBool("animatedshields");

        flashZoneAlpha = MI2USettings.getInt("flashZoneAlpha", 50) / 100f;
        enPlayerCursor = MI2USettings.getBool("enPlayerCursor", false);
        enUnitHpBar = MI2USettings.getBool("enUnitHpBar");
        enUnitRangeZone = MI2USettings.getBool("enUnitRangeZone", false);
        enOverdriveZone = MI2USettings.getBool("enOverdriveZone", false);
        enMenderZone = MI2USettings.getBool("enMenderZone", false);
        enTurretZone = MI2USettings.getBool("enTurretZone", false);
        enBlockHpBar = MI2USettings.getBool("enBlockHpBar", true);
        enDistributionReveal = MI2USettings.getBool("enDistributionReveal", false);
        drevealBridge = MI2USettings.getBool("drevealBridge", true);
        drevealJunction = MI2USettings.getBool("drevealJunction", true);
        drevealUnloader = MI2USettings.getBool("drevealUnloader", true);
        drevealInventory = MI2USettings.getBool("drevealInventory", true);
        enSpawnZone = MI2USettings.getBool("enSpawnZone", true);
        disableWreck = MI2USettings.getBool("disableWreck", false);
        disableUnit = MI2USettings.getBool("disableUnit", false);
        disableBuilding = MI2USettings.getBool("disableBuilding", false);
        disableBullet = MI2USettings.getBool("disableBullet", false);
        shadow = MI2USettings.getBool("shadow", false);
    }

    public static Field drawIndexUnit = MI2Utils.getField(Unit.class, "index__draw"), drawIndexDecal = MI2Utils.getField(Decal.class, "index__draw"), drawIndexBullet = MI2Utils.getField(Bullet.class, "index__draw");
    public static void drawBase(){
        if(!state.isGame()) return;
        if(!disableUnit){
            //Caution!! EntityGroup.add without index update leads to bug!!!
            hiddenUnit.select(Healthc::isValid).each(u -> u.setIndex__draw(Groups.draw.addIndex(u)));
            if(!hiddenUnit.isEmpty()) Log.info(hiddenUnit.mapInt(u -> u.isAdded() ? 1:0));
            hiddenUnit.clear();
        }

        drawZoneShader();

        Groups.draw.each(d -> {
            //No-bug way. TODO find out the reason of Bullet wrong removeIndex.
            if(disableWreck && d instanceof Decal dd){
                Groups.draw.removeIndex(dd, MI2Utils.getValue(drawIndexDecal, dd));
                dd.setIndex__draw(-1);
            }
            if(d instanceof Unit u){
                if(disableUnit){
                    Groups.draw.removeIndex(u, MI2Utils.getValue(drawIndexUnit, u));
                    u.setIndex__draw(-1);
                    hiddenUnit.add(u);
                }else{
                    drawUnit(u);
                }
            }
            if(disableBullet && d instanceof Bullet b){
                Groups.draw.removeIndex(b, MI2Utils.getValue(drawIndexBullet, b));
                ((Bullet)d).setIndex__draw(-1);
            }
        });

        Seq<Tile> tiles = MI2Utils.getValue(renderer.blocks, "tileview");
        BuildingInventory.ids.clear();
        if(tiles != null){
            if(disableBuilding) tiles.clear();

            for(var tile : tiles){
                if(tile.build == null) continue;
                if(enBlockHpBar) drawBlockHpBar(tile.build);
                if(enDistributionReveal){
                    BuildingInventory.ids.add(tile.build.id);
                    boolean transport = drawBlackboxBuilding(tile.build);
                    if(drevealInventory && !transport) drawItemStack(tile.build);
                }
                if(enTurretZone && tile.build instanceof BaseTurret.BaseTurretBuild btb) drawTurretZone(btb);
                if(enOverdriveZone && tile.build instanceof OverdriveProjector.OverdriveBuild odb) drawOverDriver(odb);
                if(enMenderZone && tile.build instanceof MendProjector.MendBuild mb) drawMender(mb);
                if(enMenderZone && tile.build instanceof RegenProjector.RegenProjectorBuild rb) drawRegen(rb);
                Draw.reset();
            }
        }
        if(enSpawnZone) drawSpawnPoint();

        Draw.reset();
    }

    public static void drawUnit(Unit unit){
        //Draw aim point
        if(unit.isPlayer() && enPlayerCursor && Mathf.len(unit.aimX - unit.x, unit.aimY - unit.y) < 4800f){
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
            if(enUnitHpBar){
                drawUnitHpBar(unit);
            }

            //display logicAI info by MI2
            if(unit.controller() instanceof LogicAI logicai){
                Draw.reset();
                if(MI2USettings.getBool("enUnitLogic")){
                    if(logicai.controller instanceof LogicBlock.LogicBuild lb && lb.executor != null){
                        Draw.color(0.2f, 1f, 0.6f, 0.3f);
                        Fill.arc(unit.x, unit.y, 6f, 1f - Mathf.clamp(logicai.controlTimer / LogicAI.logicControlTimeout), 90f, 20);
                        IntFloatMap utimer = MI2Utils.getValue(lexecTimer, lb.executor);
                        Draw.color(0.2f, 1f, 0.2f, 0.6f);
                        Fill.arc(unit.x, unit.y, 4f, 1f - Mathf.clamp((Time.time - utimer.get(unit.id)) / LogicAI.transferDelay), 90f, 16);
                    }
                    if(Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) <= 3200f){
                        Lines.stroke(1f);
                        Draw.color(0.2f, 0.2f, 1f, 0.8f);
                        Lines.dashLine(unit.x, unit.y, logicai.moveX, logicai.moveY, (int) (Mathf.len(logicai.moveX - unit.x, logicai.moveY - unit.y) / 8));
                        Lines.dashCircle(logicai.moveX, logicai.moveY, logicai.moveRad);
                        Draw.reset();
                    }
                }
            }

            if(enUnitRangeZone){
                float z = Draw.z();
                float range = unit.range();

                Draw.color(unit.team.color);
                Draw.z(TurretZoneDrawer.getLayer(unit.team.id));
                if(animatedshields){
                    Draw.alpha(0.05f);
                    Fill.poly(unit.x, unit.y, (int)(range) / 4, range);

                    Lines.stroke(2f);
                    Draw.alpha(1f);
                    Lines.circle(unit.x, unit.y, range);
                }else{
                    Draw.alpha(0.05f);
                    Fill.poly(unit.x, unit.y, (int)(range) / 4, range);

                    Lines.stroke(2f);
                    Draw.alpha(0.5f);
                    Lines.circle(unit.x, unit.y, range);
                }

                Draw.z(z);
                Draw.color();
            }

            //v7 rts pathfind render, making your device a barbecue.
            //Pathfind Renderer
            //TODO line length limitation to prevent lagging
            if(MI2USettings.getBool("enUnitPath")){
                if(unit.isCommandable() && unit.controller() instanceof CommandAI ai && ai.targetPos != null){
                    Draw.reset();
                    Draw.z(Layer.power - 4f);
                    Lines.stroke(1.5f);

                    if(unit.isGrounded()){
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
                    }

                    if(ai.targetPos != null){
                        Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                        Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f);
                        Draw.color(Color.white, 0.4f);
                        MI2UTmp.v1.set(lineDest).sub(unit).limit(1000f);
                        Lines.line(unit.x, unit.y, unit.x + MI2UTmp.v1.x, unit.y + MI2UTmp.v1.y);
                    }

                }else if(unit.controller() instanceof GroundAI || unit.controller() instanceof HugAI || unit.controller() instanceof SuicideAI){
                    Draw.reset();
                    Draw.z(Layer.power - 4f);
                    Tile tile = unit.tileOn();
                    int max = MI2USettings.getInt("enUnitPath.length", 40);
                    for(int tileIndex = 1; tileIndex <= max; tileIndex++){
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

    //TODO set a Runnable list.
    public static void drawBlockHpBar(Building build){
        Draw.z(Layer.shields + 3f);
        float width = 0.8f, halfwidth = width / 2f;
        float offy = -build.hitSize() * 0.4f;
        float barLength, x, y, h, w;

        if(build.health < build.maxHealth){
            if(build instanceof Wall.WallBuild wb){
                float hitTime = ((Wall)wb.block).flashHit? wb.hit:0f;
                if(hitTime > 0f){
                    Lines.stroke(2f + Mathf.lerp(0f, 2f, Mathf.clamp(hitTime)));
                    Draw.color(Color.white, Mathf.lerp(0.1f, 1f, Mathf.clamp(hitTime)));
                    Lines.line(build.x - build.hitSize() * halfwidth, build.y + offy, build.x + build.hitSize() * halfwidth, build.y + offy);
                }
            }

            Draw.color((build.health > 0 ? Pal.health:Color.gray), 0.8f);

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

        Draw.color();
    }

    public static void drawItemStack(Building b){
        BuildingInventory.get(b);
    }

    public static boolean drawBlackboxBuilding(Building b){
        if(drevealJunction && b instanceof Junction.JunctionBuild jb) drawJunciton(jb);
        else if(drevealBridge && b instanceof BufferedItemBridge.BufferedItemBridgeBuild bb) drawBufferedItemBridge(bb);
        else if(drevealBridge && b instanceof ItemBridge.ItemBridgeBuild ib) drawItemBridge(ib);
        else if(drevealUnloader && b instanceof Unloader.UnloaderBuild ub) drawUnloader(ub);
        else if(drevealJunction && b instanceof Router.RouterBuild rb) drawRouter(rb);
        else if(drevealBridge && b instanceof DuctBridge.DuctBridgeBuild db) drawDuctBridge(db);
        else if(drevealUnloader && b instanceof DirectionalUnloader.DirectionalUnloaderBuild rb) drawDirectionalUnloader(rb);
        else return false;
        return true;
    }

    public static void drawZoneShader(){
        if(animatedshields){
            if(enOverdriveZone && MI2UShaders.odzone != null){
                Draw.drawRange(91.1f, 0.02f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                    renderer.effectBuffer.end();
                    renderer.effectBuffer.blit(MI2UShaders.odzone);
                });
            }

            if(enMenderZone){
                if(MI2UShaders.mdzone != null){
                    Draw.drawRange(91.2f, 0.02f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                        renderer.effectBuffer.end();
                        renderer.effectBuffer.blit(MI2UShaders.mdzone);
                    });
                }
                if(MI2UShaders.rgzone != null){
                    Draw.drawRange(91.3f, 0.02f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                        renderer.effectBuffer.end();
                        renderer.effectBuffer.blit(MI2UShaders.rgzone);
                    });
                }
            }

            if(enTurretZone && MI2UShaders.turretzone != null){
                TurretZoneDrawer.applyShader();
            }
        }
    }

    public static void drawOverDriver(OverdriveProjector.OverdriveBuild odb){
        OverdriveProjector block = (OverdriveProjector)odb.block();
        Draw.color(block.baseColor, block.phaseColor, odb.phaseHeat);
        Draw.mixcol(Color.black, 1f - odb.efficiency());
        Draw.z(91.1f);
        Draw.alpha(animatedshields?0.6f:0.1f);
        Fill.poly(odb.x, odb.y, (int)(block.range + odb.phaseHeat * block.phaseRangeBoost) / 4, block.range + odb.phaseHeat * block.phaseRangeBoost);

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
        Draw.alpha((animatedshields?0.6f:0.2f) * alpha * flashZoneAlpha + 0.12f);
        Fill.poly(mb.x, mb.y, (int)(block.range + mb.phaseHeat * block.phaseRangeBoost) / 4, block.range + mb.phaseHeat * block.phaseRangeBoost);
    }

    public static void drawRegen(RegenProjector.RegenProjectorBuild rb){
        RegenProjector block = (RegenProjector)rb.block;
        Draw.z(91.3f);
        Draw.color(block.baseColor);
        Draw.alpha((animatedshields ? 0.4f : 0.1f) * (rb.efficiency() <= 0f ? 0.6f : 1f));
        Fill.rect(rb.x, rb.y, block.range * tilesize, block.range * tilesize);

        Lines.stroke(2f);
        Draw.alpha(rb.efficiency() <= 0f ? 0.5f : 1f);
        Lines.rect(rb.x - block.range * tilesize / 2f, rb.y - block.range * tilesize / 2f, block.range * tilesize, block.range * tilesize);
    }

    public static void drawTurretZone(BaseTurret.BaseTurretBuild btb){
        float z = Draw.z();
        float range = btb.range();

        Draw.color(btb.team.color);
        Draw.z(TurretZoneDrawer.getLayer(btb.team.id));
        if(animatedshields){
            Draw.alpha(0.05f);
            Fill.poly(btb.x, btb.y, (int)(range) / 4, range);

            Lines.stroke(2f);
            Draw.alpha(1f);
            Lines.circle(btb.x, btb.y, range);
        }else{
            Draw.alpha(0.05f);
            Fill.poly(btb.x, btb.y, (int)(range) / 4, range);

            Lines.stroke(2f);
            Draw.alpha(0.5f);
            Lines.circle(btb.x, btb.y, range);
        }

        Draw.z(z);
        Draw.color();
    }

    public static void drawJunciton(Junction.JunctionBuild jb){
        try{
            int cap = ((Junction)jb.block).capacity;
            float speed = ((Junction)jb.block).speed;

            Item[][] items = new Item[4][jb.buffer.buffers[0].length];
            for(int i = 0; i < 4; i++){
                for(int ii = 0; ii < jb.buffer.buffers[i].length; ii++){
                    items[i][ii] = (ii < jb.buffer.indexes[i])? content.item(BufferItem.item(jb.buffer.buffers[i][ii])) : null;
                }
            }
            float[][] times = new float[4][jb.buffer.buffers[0].length];
            for(int i = 0; i < 4; i++){
                for(int ii = 0; ii < jb.buffer.buffers[i].length; ii++){
                    times[i][ii] = (ii < jb.buffer.indexes[i])? BufferItem.time(jb.buffer.buffers[i][ii]) : 9999999999999f;
                }
            }

            float begx, begy, endx, endy;
            for(int i = 0; i < 4; i++){
                endx = jb.x + Geometry.d4(i).x * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
                endy = jb.y + Geometry.d4(i).y * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;
                begx = jb.x - Geometry.d4(i).x * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
                begy = jb.y - Geometry.d4(i).y * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;
                if(jb.buffer.indexes[i] > 0){
                    float loti = 0f;
                    for(int idi = 0; idi < jb.buffer.indexes[i]; idi++){
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

    public static void drawDuctBridge(DuctBridge.DuctBridgeBuild ib){
        Draw.reset();
        Draw.z(Layer.power);
        //draw each item this bridge have
        if(ib.items != null){
            Draw.color(Color.white, 0.8f);
            int loti = 0;
            for(int iid = 0; iid < ib.items.length(); iid++){
                if(ib.items.get(iid) > 0){
                    for(int itemid = 1; itemid <= ib.items.get(iid); itemid++){
                        Draw.rect(content.item(iid).fullIcon, ib.x, ib.y + tilesize * (-0.5f + 0.8f * loti / (float)ib.block.itemCapacity) + 1f, 4f, 4f);
                        loti++;
                    }
                }
            }
        }
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
                        Draw.rect(content.item(iid).fullIcon, ib.x, ib.y + tilesize * (-0.5f + 0.8f * loti / (float)ib.block.itemCapacity) + 1f, 4f, 4f);
                        loti++;
                    }
                }
            }
        }
    }

    public static void drawBufferedItemBridge(BufferedItemBridge.BufferedItemBridgeBuild bb){
        Draw.reset();
        ItemBuffer buffer = MI2Utils.getValue(itemBridgeBuffer, bb);
        if(buffer == null) return;
        long[] bufferbuffer = MI2Utils.getValue(itemBridgeBufferBuffer, buffer);
        if(bufferbuffer == null) return;
        int index = MI2Utils.getValue(itemBridgeBufferIndex, buffer);

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
        Draw.alpha(0.9f);
        for(int idi = 0; idi < bufferbuffer.length && idi < index; idi++){
            float time = Float.intBitsToFloat(Pack.leftInt(bufferbuffer[idi]));
            var item = content.item(Pack.leftShort(Pack.rightInt(bufferbuffer[idi])));
            if(item != null){
                Draw.rect(item.fullIcon,
                begx + ((endx - begx) / (float)bufferbuffer.length * Math.min(((Time.time - time) * bb.timeScale() / speed) * cap, cap - loti)),
                begy + ((endy - begy) / (float)bufferbuffer.length * Math.min(((Time.time - time) * bb.timeScale() / speed) * cap, cap - loti)), 4f, 4f);
            }
            loti++;
        }

        drawItemBridge(bb);
    }

    public static void drawUnloader(Unloader.UnloaderBuild ub){
        //ContainerStat[] possibleBlocks sorted + rotations updated on each update
        try{
            Unloader block = (Unloader)ub.block;
            Item drawItem = content.item(ub.rotations);
            Unloader.ContainerStat fromCont = ub.dumpingFrom, toCont = ub.dumpingTo;
            if(!ub.possibleBlocks.contains(ub.dumpingFrom) || !ub.possibleBlocks.contains(ub.dumpingTo)) return;

            Building fromb = MI2Utils.getValue(unloaderBuilding, fromCont), tob = MI2Utils.getValue(unloaderBuilding, toCont);

            Draw.color();

            if(!(drawItem == null || fromb == null || tob == null)){
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

                Draw.color(Pal.placing, ub.unloadTimer < block.speed ? 1f : 0.25f);
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

                Draw.color(Pal.remove, ub.unloadTimer < block.speed ? 1f : 0.25f);
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

    public static void drawDirectionalUnloader(DirectionalUnloader.DirectionalUnloaderBuild db){
        Draw.color();
        Building front = db.front(), back = db.back();
        if(front == null || back == null || back.items == null || front.team != db.team || back.team != db.team || !back.canUnload() || !(((DirectionalUnloader)db.block).allowCoreUnload || !(back instanceof CoreBlock.CoreBuild))) return;
        if(db.unloadItem != null){
            Draw.alpha(db.unloadTimer / ((DirectionalUnloader)db.block).speed < 1f && back.items.has(db.unloadItem) && front.acceptItem(db, db.unloadItem) ? 0.8f : 0f);
            Draw.rect(db.unloadItem.uiIcon, db.x, db.y, 4f, 4f);
        }else{
            var itemseq = content.items();
            int itemc = itemseq.size;
            for(int i = 0; i < itemc; i++){
                Item item = itemseq.get((i + db.offset) % itemc);
                if(back.items.has(item) && front.acceptItem(db, item)){
                    Draw.alpha(0.8f);
                    Draw.rect(item.uiIcon, db.x, db.y, 4f, 4f);
                    break;
                }
            }
        }
        Draw.color();
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

    public static void forceDrawSelect(){
        //draw selected block
        if(control.input.block == null && !Core.scene.hasMouse()){
            Vec2 vec = Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY());
            Building build = world.buildWorld(vec.x, vec.y);

            //draw different teams
            if(build != null && build.team != player.team()){
                build.drawSelect();
                if(!build.enabled && build.block.drawDisabled){
                    build.drawDisabled();
                }
            }
        }
    }
}
