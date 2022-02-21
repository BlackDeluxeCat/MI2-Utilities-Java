package mi2u;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.TextButton.*;
import arc.util.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MI2UVars {
    public static float titleButtonSize = 28f;
    public static TextButtonStyle textb = Styles.cleart, textbtoggle = Styles.clearToggleMenut;

    public static MI2UI mi2ui = new MI2UI();
    public static EmojiMindow emojis = new EmojiMindow();
    public static CoreInfoMindow coreInfo = new CoreInfoMindow();
    public static LogicHelperMindow logicHelper = new LogicHelperMindow();
    public static CustomContainerMindow container = new CustomContainerMindow();

    public static void init(){
        Mindow2.init();
        mi2ui.loadUISettings();
        emojis.loadUISettings();
        coreInfo.loadUISettings();
        logicHelper.loadUISettings();
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
}
