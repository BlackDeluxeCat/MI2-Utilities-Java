package mi2u.input;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.Time;
import arc.util.Tmp;
import mi2u.MI2UTmp;
import mindustry.gen.*;
import mindustry.input.MobileInput;

import static mindustry.Vars.control;
import static mindustry.Vars.player;

public class MobileInputExt extends MobileInput implements InputOverwrite{
    public static MobileInputExt mobileExt = new MobileInputExt();

    public boolean ctrlBoost = false, boost = false;
    public boolean ctrlPan = false, pan = false; Vec2 panXY = new Vec2();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();

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
                boolean aimCursor = unit.type.omniMovement && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;
                if(aimCursor){
                    unit.lookAt(mouseAngle - 180);  //cancel out vanilla rotation to mouse
                    unit.lookAt(aimxy);
                }
                unit.controlWeapons(true, player.shooting);
            }
        }
        //camera panning will stop unit
        if(ctrlPan && pan) {
            Core.camera.position.set(panXY);
            movement.setZero();
            unit.vel.approachDelta(Vec2.ZERO, unit.speed() * unit.type.accel / 2f);
            unit.movePref(movement);
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
    public void pan(Boolean value, Vec2 panXY){
        ctrlPan = true;
        pan = value;
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
        ctrlPan = false;
        ctrlShoot = false;
        ctrlMove = false;
        move.setZero();
        shootXY.setZero();
        panXY.setZero();
    }

    @Override
    public void replaceInput() {
        control.setInput(this);
    }
}
