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
        //TODO use unit.aim()
        if(ctrlShoot && unit != null){
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
    }

    @Override
    protected void updateMovement(Unit unit){
        //介入原方法的targetPos状态来调整移动位置
        //对payloadPos的反应似乎是优先完成payload搬运，然后才由该方法覆盖操作
        if(ctrlMove && unit != null){
            float x = Core.camera.position.x, y = Core.camera.position.y;
            Core.camera.position.set(unit).add(MI2UTmp.v3.set(move).scl(unit.type.aimDst + 8f));
            super.updateMovement(unit);
            Core.camera.position.set(x, y);
        }else{
            super.updateMovement(unit);
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
    public void pan(boolean ctrl, float x, float y){
        if(ctrl) panTimer.reset(0,0);  //set a timer for extended smooth panning
        this.panXY.set(x, y);
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
