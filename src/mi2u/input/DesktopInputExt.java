package mi2u.input;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.ObjectMap;
import arc.util.*;
import mi2u.*;
import mi2u.io.MI2USettings;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

/**
 * An extented desktop input handler.
 * Keep vanilla control while provide access to control player actions.
 * To use this Ext, get a new instance and invoke replaceInput(),
 * Multi-Mod inputhandler will be written sooner.
 */
public class DesktopInputExt extends DesktopInput implements InputOverwrite{
    public static DesktopInputExt desktopExt = new DesktopInputExt();

    public boolean ctrlBoost = false, boost = false;
    /** A timer for panning. Check returning true means moving camera.*/
    public Interval panTimer = new Interval();
    public Vec2 panXY = new Vec2();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();
    protected Building forceTapped;

    @Override
    public void update(){
        super.update();
        desktopFormation();

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
                boolean aimCursor = unit.type.omniMovement && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;
                if(aimCursor){
                    unit.lookAt(mouseAngle - 180);  //cancel out vanilla rotation to mouse
                    unit.lookAt(aimxy);
                }
                unit.controlWeapons(true, player.shooting);
            }
        }
        //panning state is stored on desktop. ctrlPan should be set to true to use overwritten states. Set ctrlPan to false after panning is ok.
        if(!panTimer.check(0, 30f)){
            panning = true;
            Core.camera.position.lerpDelta(panXY, 0.3f);

        }else if(state.isGame() && MI2USettings.getBool("edgePanning", true)){
            float camSpeed = (!Core.input.keyDown(Binding.boost) ? this.panSpeed : this.panBoostSpeed) * Time.delta;
            float margin = Mathf.clamp(Math.min(Core.graphics.getWidth() * 0.5f, Core.graphics.getHeight() * 0.5f), 5f, 30f);

            if(Core.input.mouseX() < margin){
                panning = true;
                Core.camera.position.add(-camSpeed, 0f);
            }
            if(Core.input.mouseX() > (float)Core.graphics.getWidth() - margin){
                panning = true;
                Core.camera.position.add(camSpeed, 0f);
            }
            if(Core.input.mouseY() < margin){
                panning = true;
                Core.camera.position.add(0f, -camSpeed);
            }
            if(Core.input.mouseY() > (float)Core.graphics.getHeight() - margin){
                panning = true;
                Core.camera.position.add(0f, camSpeed);
            }
        }

        if(ctrlMove && unit != null) unit.movePref(move);

        if(MI2USettings.getBool("forceTapTile", false) && Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            if(player.dead()){
                var build = world.buildWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
                forceTap(build, true);
            }else{
                forceTap(prevSelected == null ? null : prevSelected.build, false);
            }
        }
    }

    @Override
    public void boost(Boolean boost){
        ctrlBoost = true;
        this.boost = boost;
    }

    /** set ctrl to false to cancel control*/
    @Override
    public void pan(Boolean ctrl, Vec2 panXY){
        if(ctrl) panTimer.reset(0,0f);  //set a timer for extended smooth panning
        panning = ctrl;
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

    public void forceTap(@Nullable Building build, boolean includeSelfTeam){
        if(build == null) return;
        if(!includeSelfTeam && build.interactable(player.team())) return;//handled by vanilla
        if(build == forceTapped){
            inv.hide();
            config.hideConfig();
            return;
        }
        forceTapped = build;

        if(build.block.configurable){
            if((!config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
                    //alternatively, the current selected block can 'agree' to switch config tiles
                    || (config.isShown() && config.getSelected().onConfigureBuildTapped(build))){
                config.showConfig(build);
            }
            //otherwise...
        }else if(!config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(config.isShown() && config.getSelected().onConfigureBuildTapped(build)){
                config.hideConfig();
            }
        }

        //consume tap event if necessary
        if(build.block.synthetic() && (build.block.allowConfigInventory)){
            if(build.block.hasItems && build.items.total() > 0){
                inv.showFor(build);
            }
        }
    }

    public void desktopFormation(){
        if(commandMode){
            if(Core.input.keyDown(Binding.control)) RtsCommand.creatingFormation = true;
            if(Core.input.keyRelease(Binding.control)) RtsCommand.creatingFormation = false;
            //force block selection short-cut to switch category
            MI2Utils.setValue(ui.hudfrag.blockfrag, "blockSelectEnd", true);
            //cancel any stored block selections
            ObjectMap selectBlocks = MI2Utils.getValue(ui.hudfrag.blockfrag, "selectedBlocks");
            selectBlocks.each((cat, block) -> selectBlocks.put(cat, null));
            if(RtsCommand.creatingFormation){
                if(Core.input.keyTap(Binding.block_select_01)) RtsCommand.createFormation(selectedUnits, 0);
                if(Core.input.keyTap(Binding.block_select_02)) RtsCommand.createFormation(selectedUnits, 1);
                if(Core.input.keyTap(Binding.block_select_03)) RtsCommand.createFormation(selectedUnits, 2);
                if(Core.input.keyTap(Binding.block_select_04)) RtsCommand.createFormation(selectedUnits, 3);
                if(Core.input.keyTap(Binding.block_select_05)) RtsCommand.createFormation(selectedUnits, 4);
                if(Core.input.keyTap(Binding.block_select_06)) RtsCommand.createFormation(selectedUnits, 5);
                if(Core.input.keyTap(Binding.block_select_07)) RtsCommand.createFormation(selectedUnits, 6);
                if(Core.input.keyTap(Binding.block_select_08)) RtsCommand.createFormation(selectedUnits, 7);
                if(Core.input.keyTap(Binding.block_select_09)) RtsCommand.createFormation(selectedUnits, 8);
                if(Core.input.keyTap(Binding.block_select_10)) RtsCommand.createFormation(selectedUnits, 9);
            }else{
                if(Core.input.keyTap(Binding.block_select_01)) RtsCommand.callFormation(0);
                if(Core.input.keyTap(Binding.block_select_02)) RtsCommand.callFormation(1);
                if(Core.input.keyTap(Binding.block_select_03)) RtsCommand.callFormation(2);
                if(Core.input.keyTap(Binding.block_select_04)) RtsCommand.callFormation(3);
                if(Core.input.keyTap(Binding.block_select_05)) RtsCommand.callFormation(4);
                if(Core.input.keyTap(Binding.block_select_06)) RtsCommand.callFormation(5);
                if(Core.input.keyTap(Binding.block_select_07)) RtsCommand.callFormation(6);
                if(Core.input.keyTap(Binding.block_select_08)) RtsCommand.callFormation(7);
                if(Core.input.keyTap(Binding.block_select_09)) RtsCommand.callFormation(8);
                if(Core.input.keyTap(Binding.block_select_10)) RtsCommand.callFormation(9);
            }
        }
    }
}
