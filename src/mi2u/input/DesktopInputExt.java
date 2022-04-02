package mi2u.input;

import arc.Core;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mi2u.MI2UTmp;
import mindustry.gen.Mechc;
import mindustry.gen.Unit;
import mindustry.input.*;

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
    public boolean ctrlPan = false, pan = false; Vec2 panXY = new Vec2();
    public boolean ctrlShoot = false, shoot = false; Vec2 shootXY = new Vec2();
    public boolean ctrlMove = false; Vec2 move = new Vec2();

    @Override
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
        if(ctrlPan){
            panning = pan;
            if(panning) Core.camera.position.set(panXY);
        }
        if(ctrlMove && unit != null) unit.movePref(move);
    }

    @Override
    public void boost(Boolean value){
        ctrlBoost = true;
        boost = value;
    }

    /** set panXY to zero to cancel control, this may be useless on desktop input*/
    @Override
    public void pan(Boolean value, Vec2 panXY){
        ctrlPan = !panXY.isZero();
        pan = value;
        this.panXY = panXY;
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
    public void clear(){
        ctrlBoost = false;
        ctrlPan = false;
        ctrlShoot = false;
        ctrlMove = false;
        move.setZero();
        shootXY.setZero();
        panXY.setZero();
    }

    @Override
    public void replaceInput(){
        control.setInput(this);
    }
}
