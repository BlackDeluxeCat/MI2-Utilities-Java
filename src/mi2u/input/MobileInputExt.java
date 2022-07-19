package mi2u.input;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.Interval;
import arc.util.Time;
import mindustry.gen.*;
import mindustry.input.MobileInput;

import static mindustry.Vars.control;
import static mindustry.Vars.player;

public class MobileInputExt extends MobileInput implements InputOverwrite{
    public static MobileInputExt mobileExt = new MobileInputExt();

    public boolean ctrlBoost = false, boost = false;
    public Vec2 panXY = new Vec2();
    /** A timer for panning. Check returning true means moving camera.*/
    public Interval panTimer = new Interval();
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
        if(!panTimer.check(0, 30f)){
            Core.camera.position.lerpDelta(panXY, 0.3f);
        }

        //unit move will let camera position disassociates with unit position
        if(ctrlMove && unit != null){
            //camera-centered movement offset
            movement.set(player).sub(targetPos).limit(unit.speed());
            if(player.within(targetPos, 15f)) movement.setZero();
            unit.movePref(movement);
            //control move
            unit.movePref(move);
        }
    }

    @Override
    public void boost(Boolean value){
        ctrlBoost = true;
        boost = value;
    }

    @Override
    public void pan(Boolean ctrl, Vec2 panXY){
        if(ctrl) panTimer.reset(0,0f);  //set a timer for extended smooth panning
        this.panXY.set(panXY);
    }

    @Override
    public void shoot(Vec2 vec, Boolean value, Boolean ctrl){
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

    @Override
    public void replaceInput() {
        control.setInput(this);
    }
}
