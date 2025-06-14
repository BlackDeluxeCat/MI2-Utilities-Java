package mi2u.input;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mi2u.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class MobileInputExt extends MobileInput implements InputOverwrite{
    public static MobileInputExt mobileExt = new MobileInputExt();

    public boolean ctrlBoost = false, boost = false;
    public Vec2 panXY = new Vec2();
    /** A timer for panning. Check returning true means moving camera.*/
    public MI2Utils.IntervalMillis panTimer = new MI2Utils.IntervalMillis();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();

    public void update(){
        super.update();
        Unit unit = player.unit();
        if(ctrlBoost) player.boosting = boost;
        if(ctrlShoot){
            player.shooting = shoot && !(unit instanceof Mechc && unit.isFlying());
            if(player.shooting){
                unit.rotation(Angles.moveToward(unit.rotation(), Angles.angle(shootXY.x - unit.x, shootXY.y - unit.y), unit.type.rotateSpeed * unit.speedMultiplier() * Time.delta * 1.5f));
                player.mouseX = shootXY.x;
                player.mouseY = shootXY.y;
                unit.aim(player.mouseX, player.mouseY);
                unit.controlWeapons(true, player.shooting);
            }
        }
        //camera moving, shouldn't consider unit follow movement
        if(!panTimer.check(0, 400)){
            Core.camera.position.lerpDelta(panXY, 0.3f);
        }

        //unit move will let camera position disassociates with unit position
        if(ctrlMove && unit != null){
            //camera-centered movement offset
            movement.set(player).sub(targetPos).limit(unit.speed());
            if(player.within(targetPos, 15f)) movement.setZero();
            unit.movePref(movement);
            if(unit.type.omniMovement && MI2UTmp.v1.set(unit.x, unit.y).sub(move).len() < 4f) unit.vel.approachDelta(Vec2.ZERO, unit.speed() * unit.type.accel / 4f);
            //control move
            unit.movePref(move);
        }
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button){
        if(mi2ui.settings.getBool("forceTapTile") && Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            var build = world.buildWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
            InputUtils.forceTap(build, player.dead());
        }
        return super.tap(x, y, count, button);
    }

    @Override
    public void boost(boolean value){
        ctrlBoost = true;
        boost = value;
    }

    @Override
    public void pan(boolean ctrl, Vec2 panXY){
        if(ctrl) panTimer.reset(0,0);  //set a timer for extended smooth panning
        this.panXY.set(panXY);
    }

    @Override
    public void shoot(Vec2 vec, boolean value, boolean ctrl){
        ctrlShoot = ctrl;
        shootXY.set(vec);
        shoot = value;
    }

    @Override
    public void move(Vec2 value){
        ctrlMove = true;
        move.set(value);
    }

    @Override
    public void clear() {
        ctrlBoost = false;
        ctrlShoot = false;
        ctrlMove = false;
        move.setZero();
        shootXY.setZero();
    }
}
