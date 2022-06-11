package mi2u.input;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mi2u.*;
import mi2u.io.MI2USettings;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.breaking;

/**
 * An extented desktop input handler.
 * Keep vanilla control while provide access to control player actions.
 * To use this Ext, get a new instance and invoke replaceInput(),
 * Multi-Mod inputhandler will be written sooner.
 */
public class DesktopInputExt extends DesktopInput implements InputOverwrite{
    public static DesktopInputExt desktopExt = new DesktopInputExt();

    public boolean ctrlBoost = false, boost = false;
    public boolean ctrlPan = false; Vec2 panXY = new Vec2();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();

    public int schemX_Ext = -1, schemY_Ext = -1;
    protected Building forceTapped;

    @Override
    public void update(){
        super.update();
        Unit unit = player.unit();
        if(ctrlBoost) player.boosting = boost;
        if(ctrlShoot){
            boolean boosted = unit instanceof Mechc && unit.isFlying();
            player.shooting = shoot && !boosted;
            if(player.shooting){
                player.mouseX = shootXY.x;
                player.mouseY = shootXY.y;
                Vec2 aimxy = MI2UTmp.v1.set(player.mouseX, player.mouseY);
                unit.aim(unit.type.faceTarget ? aimxy : Tmp.v1.trns(unit.rotation, aimxy.dst(unit)).add(unit.x, unit.y));

                float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
                boolean aimCursor = unit.type.omniMovement && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted && unit.type.rotateShooting;
                if(aimCursor){
                    unit.lookAt(mouseAngle - 180);  //cancel out vanilla rotation to mouse
                    unit.lookAt(aimxy);
                }
                unit.controlWeapons(true, player.shooting);
            }
        }
        //panning state is stored on desktop. ctrlPan should be set to true to use overwritten states. Set ctrlPan to false after panning is ok.
        if(ctrlPan && !panXY.isZero()){
            panning = true;
            Core.camera.position.set(panXY);
            ctrlPan = false;
        }

        if(ctrlMove && unit != null) unit.movePref(move);

        checkCreateSchematic();
        if(MI2USettings.getBool("forceTapTile", false)) forceTap(prevSelected == null ? null: prevSelected.build);
    }

    @Override
    public void boost(Boolean boost){
        ctrlBoost = true;
        this.boost = boost;
    }

    /** set panXY to zero to cancel control, this may be useless on desktop input*/
    @Override
    public void pan(Boolean ctrl, Vec2 panXY){
        ctrlPan = ctrl;
        this.panXY.set(panXY);
    }

    @Override
    public void shoot(Vec2 vec, Boolean shoot, Boolean ctrl){
        ctrlShoot = ctrl;
        shootXY.set(vec);
        this.shoot = shoot;
    }

    @Override
    public void move(Vec2 movement){
        ctrlMove = true;
        move.set(movement);
    }

    @Override
    public void clear(){
        ctrlBoost = false;
        ctrlShoot = false;
        ctrlMove = false;
        move.setZero();
        shootXY.setZero();
    }

    @Override
    public void replaceInput(){
        control.setInput(this);
    }

    //will cover vanilla sche select
    public void checkCreateSchematic(){
        if(Core.input.keyTap(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            schemX_Ext = schemX;
            schemY_Ext = schemY;
        }
        if(Core.input.keyRelease(Binding.schematic_select) && !Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX_Ext != -1 && schemY_Ext != -1){
            lastSchematic = MI2UFuncs.createSchematic(schemX_Ext, schemY_Ext, World.toTile(Core.input.mouseWorld().x), World.toTile(Core.input.mouseWorld().y));
            useSchematic(lastSchematic);
            if(selectRequests.isEmpty()){
                lastSchematic = null;
            }
            schemX_Ext = -1;
            schemY_Ext = -1;
        }
    }

    @Override
    public void drawTop(){
        super.drawTop();
        if(Core.input.keyDown(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            Vec2 vec = MI2UTmp.v1.set(Core.input.mouseWorld(Core.input.mouseX(), Core.input.mouseY()));
            if(selectedBlock()){
                vec.sub(block.offset, block.offset);
            }
            drawSelection(schemX_Ext, schemY_Ext, World.toTile(vec.x), World.toTile(vec.y), 512);
        }
        Draw.reset();
    }

    public void forceTap(@Nullable Building build){
        if(build == null) return;
        if(build.interactable(player.team())) return;//handled by vanilla
        if(build == forceTapped) return;
        forceTapped = build;
        if(build.block.configurable){
            if((!frag.config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
                    //alternatively, the current selected block can 'agree' to switch config tiles
                    || (frag.config.isShown() && frag.config.getSelectedTile().onConfigureTileTapped(build))){
                frag.config.showConfig(build);
            }
            //otherwise...
        }else if(!frag.config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(frag.config.isShown() && frag.config.getSelectedTile().onConfigureTileTapped(build)){
                frag.config.hideConfig();
            }
        }

        //consume tap event if necessary
        if(build.block.synthetic() && (build.block.allowConfigInventory)){
            if(build.block.hasItems && build.items.total() > 0){
                frag.inv.showFor(build);
            }
        }
    }
}
