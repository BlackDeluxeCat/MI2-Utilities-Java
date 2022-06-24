package mi2u.input;

import arc.func.*;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.struct.*;
import mi2u.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** The inputOverwrite accept commands and change control states, implements of control should be overwritten and run according to states on each update. */
public interface InputOverwrite{
    default void build(Boolean build){};

    default void boost(Boolean boost){};

    default void pan(Boolean ctrl, Vec2 panXY){};

    default void shoot(Vec2 vec, Boolean shoot, Boolean ctrl){};

    default void move(Vec2 movement){};

    default void approach(Vec2 point, float radius, boolean checkWithin){
        Vec2 vec = MI2UTmp.v1;
        Unit unit = player.unit();
        if(checkWithin && unit.within(point.x, point.y, radius)){
            move(vec.setZero());
            return;
        }
        vec.set(point).sub(unit);
        float length = radius <= 0.001f ? 1f : Mathf.clamp((unit.dst(point.x, point.y) - radius) / 50f, -1f, 1f);
        vec.setLength(unit.speed() * length);
        if(length < -0.5f){
            vec.rotate(180f);
        }else if(length < 0){
            vec.setZero();
        }
        move(vec);
    }

    default void approach(Position target, float radius, boolean checkWithin){
        if(target == null) return;
        approach(MI2UTmp.v2.set(target.getX(),target.getY()), radius, checkWithin);
    }

    default void clear(){};

    void replaceInput();
}
