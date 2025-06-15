package mi2u.graphics;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;

/**
 * HitboxBarDrawer 类用于绘制游戏中具有碰撞框实体的属性条覆盖层。
 * <p>
 * - 只能通过 HitboxBarDrawer.hitboxBarDrawer 访问实例。
 * - 支持链式调用。
 * - 方法 next 调整指定方向的属性条。
 */
public class HitboxBarDrawer{
    public static HitboxBarDrawer barDrawer = new HitboxBarDrawer();

    public float padTop, padBottom, padLeft, padRight, padCenter;
    public Rect box = new Rect();

    public HitboxBarDrawer reset(){
        padTop = padBottom = padLeft = padRight = padCenter = 0f;
        return this;
    }

    public HitboxBarDrawer set(float x, float y, float w, float h){
        box.set(x, y, w, h);
        return this;
    }

    public HitboxBarDrawer fill(int alignTgt, float fraction, float lenMul, float stroke){
        return fill(alignTgt, 0, fraction, lenMul, stroke);
    }

    /**
     * 填充属性条。
     * <p>
     * 颜色在方法外自行设置
     * <p>
     *
     * @param alignTgt 属性条的对齐目标，可以是顶部、底部、左侧或右侧。
     * @param alignBar 属性条的起始对齐方式，用于确定条的起点。可选值有0-从左/下，和1-从右/上
     * @param fraction 属性条填充的百分比，范围从0到1。
     * @param lenMul   长度乘数，用于调整属性条的长度。
     * @param stroke   属性条的描边宽度。
     */
    public HitboxBarDrawer fill(int alignTgt, int alignBar, float fraction, float lenMul, float stroke){
        float fx = getBarCenterX(alignTgt), fy = getBarCenterY(alignTgt);

        //条长
        float barW = ((alignTgt & (Align.top | Align.bottom)) != 0 ? box.width : box.height) * lenMul * fraction;

        //条起点
        //补齐条长度（含alignBar对齐）和条粗细的坐标差
        if((alignTgt & (Align.top | Align.bottom)) != 0){
            fx += alignBar == 0 ? (-box.width / 2f * lenMul) : (box.width / 2f * lenMul - barW);
            fy -= (alignTgt == Align.top ? stroke : -stroke) / 2f;
            Fill.crect(fx, fy, barW, stroke);
        }else if((alignTgt & (Align.left | Align.right)) != 0){
            fy += alignBar == 0 ? (-box.height / 2f * lenMul) : (box.height / 2f * lenMul - barW);
            fx -= (alignTgt == Align.right ? stroke : -stroke) / 2f;
            Fill.crect(fx, fy, stroke, barW);
        }

        return this;
    }

    public float getBarCenterX(int alignTgt){
        return box.x + switch(alignTgt){
            case Align.left -> padLeft - box.width / 2f;
            case Align.right -> -padRight + box.width / 2f;
            default -> 0;
        };
    }

    public float getBarCenterY(int alignTgt){
        return box.y + switch(alignTgt){
            case Align.top -> -padTop + box.height / 2f;
            case Align.bottom -> padBottom - box.height / 2f;
            default -> 0;
        };
    }

    public HitboxBarDrawer addPad(int alignTgt, float step){
        if(alignTgt == Align.top) padTop += step;
        if(alignTgt == Align.bottom) padBottom += step;
        if(alignTgt == Align.left) padLeft += step;
        if(alignTgt == Align.right) padRight += step;
        if(alignTgt == Align.center) padCenter += step;
        return this;
    }
}
