package mi2u.input;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import mi2u.MI2UTmp;
import mindustry.gen.Unit;

import static mindustry.Vars.*;

public interface InputOverwrite{
    public default void build(Boolean value){};

    public default void boost(Boolean value){};

    public default void pan(Boolean value, Vec2 panXY){};

    public default void shoot(Vec2 vec, Boolean value, Boolean ctrl){};

    public default void move(Vec2 value){};

    public default void approach(Vec2 point, float radius){
        Vec2 vec = MI2UTmp.v1;
        Unit unit = player.unit();
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

    public default void approach(Position target, float radius){
        if(target == null) return;
        approach(MI2UTmp.v2.set(target.getX(),target.getY()), radius);
    }

    public default void clear(){};

    public void replaceInput();
}
