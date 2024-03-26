package mi2u.input;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;

import static arc.Core.camera;
import static arc.Core.graphics;
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

    @Override
    protected void drawSelection(int x1, int y1, int x2, int y2, int maxLength){
        super.drawSelection(x1, y1, x2, y2, maxLength);
        if(!mi2ui.settings.getBool("drawSelectionSize")) return;
        Placement.NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);
        Font font = Fonts.outline;
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        font.getData().setScale(4f / Scl.scl(1f) * camera.width / graphics.getWidth());
        font.getCache().setColor(MI2UTmp.c1.set(Pal.accent).a(0.7f));
        font.draw((int)(result.x2 - result.x) / 8 + "x" + (int)(result.y2 - result.y) / 8, result.x2, result.y, Align.right);
        font.getData().setScale(oldScaleX, oldScaleY);
    }
}
