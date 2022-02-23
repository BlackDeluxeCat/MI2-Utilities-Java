package mi2u;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.World;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.BufferedItemBridge.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.distribution.Junction.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.Unloader.*;
import mindustry.world.blocks.storage.Unloader.UnloaderBuild.*;

import static mindustry.Vars.*;

import java.lang.reflect.*;

/** @Author 工业2*/
public class MI2UFuncs{
    protected static Interval interval = new Interval();

    public static void drawUnit(Unit unit){
        if(Math.abs(unit.x - Core.camera.position.x) > (Core.camera.width / 2) || Math.abs(unit.y - Core.camera.position.y) > (Core.camera.height / 2)) return;
        //display healthbar by MI2
        Draw.z(Layer.shields + 6f);
        if(MI2USettings.getBool("enUnitHpBar")){
            Draw.reset();
            if(unit.hitTime > 0f){
                Lines.stroke(4f + Mathf.lerp(0f, 2f, Mathf.clamp(unit.hitTime)));
                Draw.color(Color.white, Mathf.lerp(0.1f, 1f, Mathf.clamp(unit.hitTime)));
                Lines.line(unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f), unit.x + unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f));
            }
            Lines.stroke(4f);
            Draw.color(unit.team.color, 0.5f);
            Lines.line(unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f), unit.x + unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f));
            Draw.color((unit.health > 0 ? Pal.health:Color.gray), 0.8f);
            Lines.stroke(2);
            Lines.line(
                unit.x - unit.hitSize() * 0.6f, unit.y + (unit.hitSize() / 2f), 
                unit.x + unit.hitSize() * ((unit.health > 0 ? unit.health : Mathf.maxZero(unit.maxHealth + unit.health)) / unit.maxHealth * 1.2f - 0.6f), unit.y + (unit.hitSize() / 2f));
            Lines.stroke(2f);
            if(unit.shield > 0){
                for(int didgt = 1; didgt <= Mathf.digits((int)(unit.shield / unit.maxHealth)) + 1; didgt++){
                    //if(didgt == Mathf.digits((int)(unit.shield / unit.maxHealth)) + 1) continue;
                    Draw.color(Pal.shield, 0.8f);
                    float barLength = Mathf.mod(unit.shield / unit.maxHealth, Mathf.pow(10f, (float)didgt - 1f)) / Mathf.pow(10f, (float)didgt - 1f);
                    if(didgt > 1){
                        //float x = unit.x - (1f - Mathf.floor(barLength * 10f) / 10f) / 2f * 1.2f * unit.hitSize();
                        float y = unit.y + unit.hitSize() / 2f + didgt * 2f;
                        float h = 2f;
                        //float w = 1.2f * Mathf.floor(barLength * 10f) / 10f * unit.hitSize();
                        for(float i = 1; i <= Mathf.floor(barLength * 10f); i++){
                            Fill.rect(unit.x - 0.55f * unit.hitSize() + (i - 1f) * 0.12f * unit.hitSize(), y, 0.1f * unit.hitSize(), h);
                        }
                    }else{
                        float x = unit.x - (1f - barLength) / 2f * 1.2f * unit.hitSize();
                        float y = unit.y + unit.hitSize() / 2f + didgt * 2f;
                        float h = 2f;
                        float w = 1.2f * barLength * unit.hitSize();
                        Fill.rect(x, y, w, h);
                    }
                }
            }
            Draw.reset();
            
            float index = 0f;
            for(StatusEffect eff : content.statusEffects()){
                if(eff == StatusEffects.boss) continue;
                if(unit.hasEffect(eff)){
                    float iconSize = Mathf.ceil(unit.hitSize() / 4f);
                    Draw.rect(eff.uiIcon, 
                    unit.x - unit.hitSize() * 0.6f + 0.5f * iconSize * Mathf.mod(index, 4f), 
                    unit.y + (unit.hitSize() / 2f) + 3f + iconSize * Mathf.floor(index / 4f), 
                    4f, 4f);
                    index++;
                }
            }
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

        //Pathfind Renderer
        //if(Core.settings.getBool("unitPathLine") && Core.settings.getInt("unitPathLineLength") > 0){
        if(MI2USettings.getBool("enUnitPath")){
            Draw.z(Layer.power - 4f);
            Tile tile = unit.tileOn();
            Draw.reset();
            for(int tileIndex = 1; tileIndex <= 40; tileIndex++){
                Tile nextTile = pathfinder.getTargetTile(tile, pathfinder.getField(unit.team, unit.pathType(), (unit.team.data().command == UnitCommand.attack)? Pathfinder.fieldCore : Pathfinder.fieldRally));
                if(nextTile == null) break;
                Lines.stroke(2);
                if(nextTile == tile){
                    Draw.color(unit.team.color, Color.black, Mathf.absin(Time.time, 4f, 1f));
                    Lines.poly(unit.x, unit.y, 6, unit.hitSize());
                    break;
                }
                Draw.color(unit.team.color, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                Lines.dashLine(tile.worldx(), tile.worldy(), nextTile.worldx(), nextTile.worldy(), (int)(Mathf.len(nextTile.worldx() - tile.worldx(), nextTile.worldy() - tile.worldy()) / 4f));
                //Fill.poly(nextTile.worldx(), nextTile.worldy(), 4, tilesize - 2, 90);
                tile = nextTile;
            }
            Draw.reset();
        }

        Draw.reset();
    }
    
    public static void unitRebuildBlocks(){
        if(!state.isGame() || !player.unit().canBuild()) return;
        int p = 0;
        for(BlockPlan block : state.teams.get(player.team()).blocks){
            if(Mathf.len(block.x - player.tileX(), block.y - player.tileY()) >= 200) continue;
            p++;
            if(p > 511) break;
            player.unit().addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
        }
    }

    public static void drawBlackboxBuilding(){
        Draw.reset();
        Lines.stroke(0.5f);
        Lines.rect(new Rect().setSize(240f).setCenter(Core.input.mouseWorld()));

        Groups.build.each(b -> {
            Rect rect = new Rect();
            Core.camera.bounds(rect);
            if(!rect.contains(b.tile().worldx(), b.tile().worldy())) return;
            rect.setSize(240f).setCenter(Core.input.mouseWorld());
            if(!rect.contains(b.tile().worldx(), b.tile().worldy())) return;

            if(b instanceof JunctionBuild jb) drawJunciton(jb);
            if(b instanceof ItemBridgeBuild ib) drawItemBridge(ib);
            if(b instanceof BufferedItemBridgeBuild bb) drawBufferedItemBridge(bb);
            if(b instanceof UnloaderBuild ub) drawUnloader(ub);
        });
    }
    public static void drawJunciton(JunctionBuild jb){
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
                            begx + ((endx - begx) / (float)cap * Math.min(((Time.time - times[i][idi]) * jb.timeScale / speed) * cap, cap - loti)), 
                            begy + ((endy - begy) / (float)cap * Math.min(((Time.time - times[i][idi]) * jb.timeScale / speed) * cap, cap - loti)),
                            4f, 4f);
                        }
                        loti++;
                    }
                }
            }
        }catch(Exception e){if(!interval.get(30)) return; Log.err(e.toString());}
    }
        
    public static void drawItemBridge(ItemBridgeBuild ib){
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

    public static void drawBufferedItemBridge(BufferedItemBridgeBuild bb){
        try{
            Field f = bb.getClass().getDeclaredField("buffer");
            f.setAccessible(true);
            ItemBuffer buffer = (ItemBuffer)f.get(bb);
            f = buffer.getClass().getDeclaredField("buffer");
            f.setAccessible(true);
            long[] bufferbuffer = (long[])f.get(buffer);
            f = buffer.getClass().getDeclaredField("index");
            f.setAccessible(true);
            int index = (int)f.get(buffer);

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
                    begx + ((endx - begx) / (float)bufferItems.length * Math.min(((Time.time - bufferTimes[idi]) * bb.timeScale / speed) * cap, cap - loti)), 
                    begy + ((endy - begy) / (float)bufferItems.length * Math.min(((Time.time - bufferTimes[idi]) * bb.timeScale / speed) * cap, cap - loti)), 4f, 4f);
                }
                loti++;
            }
        }catch(Exception e){if(!interval.get(30)) return; Log.err(e.toString());}
    }

    public static void drawUnloader(UnloaderBuild ub){
        //ContainerStat[] possibleBlocks sorted + rotations updated on each update
        try{
            Unloader block = (Unloader)ub.block;
            Item drawItem = content.item(ub.rotations);
            Building fromb = null, tob = null;

            Field f;
            Building tmp;
            boolean toFound = false, fromFound = false;
            boolean canLoad, canUnload;
            for(ContainerStat c : ub.possibleBlocks){
                f = ContainerStat.class.getDeclaredField("canLoad");
                f.setAccessible(true);
                canLoad = f.getBoolean(c);
                f = ContainerStat.class.getDeclaredField("canUnload");
                f.setAccessible(true);
                canUnload = f.getBoolean(c);
                f = ContainerStat.class.getDeclaredField("building");
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
                switch(Mathf.mod(((int)Angles.angle(tob.x - ub.x, tob.y - ub.y) + 45) / 90, 4)){
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
}
