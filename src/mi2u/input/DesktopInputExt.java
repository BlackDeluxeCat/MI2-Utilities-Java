package mi2u.input;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mi2u.graphics.*;
import mi2u.io.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.world.blocks.*;

import static arc.Core.*;
import static mindustry.Vars.*;

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
    public MI2Utils.IntervalMillis panTimer = new MI2Utils.IntervalMillis();
    public Vec2 panXY = new Vec2();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();

    @Override
    public void update(){
        super.update();
        postPMFrag();
        tryCtrlBuildUnderUnit();

        Unit unit = player.unit();
        if(ctrlBoost) player.boosting = boost;
        if(ctrlShoot){
            boolean boosted = unit instanceof Mechc && unit.isFlying();
            player.shooting = shoot && !boosted;
            if(player.shooting){
                player.mouseX = shootXY.x;
                player.mouseY = shootXY.y;
                Vec2 aimxy = shootXY;
                unit.aim(aimxy);

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
        if(!panTimer.check(0, 400)){
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
                InputUtils.forceTap(build, true);
            }else{
                InputUtils.forceTap(prevSelected == null ? null : prevSelected.build, false);
            }
        }
    }

    @Override
    public void boost(boolean boost){
        ctrlBoost = true;
        this.boost = boost;
    }

    /** set ctrl to false to cancel control*/
    @Override
    public void pan(boolean ctrl, Vec2 panXY){
        if(ctrl) panTimer.reset(0,0);  //set a timer for extended smooth panning
        panning = ctrl;
        this.panXY.set(panXY);
    }

    @Override
    public void shoot(Vec2 vec, boolean shoot, boolean ctrl){
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

    public static Binding[] numKey = {
            Binding.block_select_01,
            Binding.block_select_02,
            Binding.block_select_03,
            Binding.block_select_04,
            Binding.block_select_05,
            Binding.block_select_06,
            Binding.block_select_07,
            Binding.block_select_08,
            Binding.block_select_09,
            Binding.block_select_10};

    public void tryCtrlBuildUnderUnit(){
        if(!scene.hasMouse() && !locked() && state.rules.possessionAllowed){
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                var build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
                if(RendererExt.disableUnit && selectedUnit() != null && build instanceof ControlBlock cont && cont.canControl() && build.team == player.team() && cont.unit() != player.unit() && cont.unit().isAI()){
                    Call.unitControl(player, cont.unit());
                }
            }
        }
    }

    /**
     * Post Placement fragment to your mouse.
     */
    public void postPMFrag(){
        Table fullT = MI2Utils.getValue(ui.hudfrag.blockfrag, "toggler");
        if(fullT == null) return;
        if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.pick)){
            fullT.margin(0f, 0f,
                    (Core.input.mouseY() + 15f) / Scl.scl(),
                    Mathf.maxZero(Core.graphics.getWidth() - Core.input.mouseX() - fullT.getChildren().first().getWidth() - 15f) / Scl.scl());
            fullT.layout();
        }else if(Core.input.keyTap(Binding.control)){
            fullT.margin(0f);
            fullT.layout();
        }
    }
}
