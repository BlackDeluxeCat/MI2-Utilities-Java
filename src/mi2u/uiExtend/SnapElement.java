package mi2u.uiExtend;

import arc.input.KeyCode;
import arc.math.geom.QuadTree;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Tmp;
import mi2u.MI2UVars;

import java.util.Arrays;

import static arc.Core.input;
import static arc.util.Align.*;
import static arc.util.Align.bottom;

public class SnapElement extends Table implements QuadTree.QuadTreeObject {
    public static final float snapRange = 32f;

    public boolean dragging = false;
    public float snapX = -1, snapY = -1;
    public float hoverTime = 0f;
    /**
     * Returns the X position of the specified {@link Align alignment}.
     */
    @Override
    public float getX(int alignment){
        float x = this.x;
        if((alignment & right) != 0)
            x += width * scaleX;
        else if((alignment & left) == 0) //
            x += width * scaleX / 2;
        return x;
    }

    /**
     * Returns the Y position of the specified {@link Align alignment}.
     */
    @Override
    public float getY(int alignment){
        float y = this.y;
        if((alignment & top) != 0)
            y += height * scaleY;
        else if((alignment & bottom) == 0) //
            y += height * scaleY / 2;
        return y;
    }

    @Override
    public void setPosition(float x, float y, int alignment){
        if((alignment & right) != 0)
            x -= width * scaleX;
        else if((alignment & left) == 0) //
            x -= width * scaleX / 2;

        if((alignment & top) != 0)
            y -= height * scaleY;
        else if((alignment & bottom) == 0) //
            y -= height * scaleY / 2;

        if(this.x != x || this.y != y){
            this.x = x;
            this.y = y;
        }
    }

    public void innerElementSnap(Element element){
        if (element == null) return;
        if (Math.abs(leftBound(this) - leftBound(element)) < snapRange) snapX = leftBound(element);
        if (Math.abs(bottomBound(this) - bottomBound(element)) < snapRange) snapY = bottomBound(element);
        if (Math.abs(topBound(this) - topBound(element)) < snapRange) snapY = topBound(element) - height(this);
        if (Math.abs(rightBound(this) - rightBound(element)) < snapRange) snapX = rightBound(element) - width(this);
    }

    public void outerElementSnap(Element element){
        if (element == null) return;
        if (!element.visible) return;
        Tmp.r1.set(x, y, width(this), height(this)).grow(snapRange/2f);
        Tmp.r2.set(element.x, element.y, width(element), height(element)).grow(snapRange/2f);
        if (!Tmp.r1.overlaps(Tmp.r2)) return;

        //todo check on both dst
        if (Math.abs(leftBound(this) - rightBound(element)) < snapRange) {
            snapX = rightBound(element);
            if (Math.abs(topBound(this) - topBound(element)) < snapRange) snapY = topBound(element) - height(this);
            if (Math.abs(bottomBound(this) - bottomBound(element)) < snapRange) snapY = bottomBound(element);
        }
        if (Math.abs(rightBound(this) - leftBound(element)) < snapRange) {
            snapX = leftBound(element) - width(this);
            if (Math.abs(topBound(this) - topBound(element)) < snapRange) snapY = topBound(element) - height(this);
            if (Math.abs(bottomBound(this) - bottomBound(element)) < snapRange) snapY = bottomBound(element);
        }
        if (Math.abs(bottomBound(this) - topBound(element)) < snapRange) {
            snapY = topBound(element);
            if (Math.abs(rightBound(this) - rightBound(element)) < snapRange) snapX = rightBound(element) - width(this);
            if (Math.abs(leftBound(this) - leftBound(element)) < snapRange) snapX = leftBound(element);
        }
        if (Math.abs(topBound(this) - bottomBound(element)) < snapRange) {
            snapY = bottomBound(element) - height(this);
            if (Math.abs(rightBound(this) - rightBound(element)) < snapRange) snapX = rightBound(element) - width(this);
            if (Math.abs(leftBound(this) - leftBound(element)) < snapRange) snapX = leftBound(element);
        }
    }

    public boolean isElementSnapped(Element self, Element element){
        if (element == null) return false;
        if (!element.visible) return false;
        Tmp.r1.set(self.x, self.y, width(self), height(self)).grow(snapRange/2f);
        Tmp.r2.set(element.x, element.y, width(element), height(element)).grow(snapRange/2f);
        if (!Tmp.r1.overlaps(Tmp.r2)) return false;

        return Math.abs(topBound(self) - bottomBound(element)) < snapRange/4f ||
               Math.abs(leftBound(self) - rightBound(element)) < snapRange/4f ||
               Math.abs(rightBound(self) - leftBound(element)) < snapRange/4f ||
               Math.abs(bottomBound(self) - topBound(element)) < snapRange/4f;
    }

    public boolean isElementSnapped(Element element){
        return isElementSnapped(this, element);
    }

    public Seq<Element> getAllElements() {
        Seq<Element> elements = new Seq<>();
        elements.add(MI2UVars.mindow2s);
        elements.add(MI2UVars.windows);
        return elements;
    }

    public Seq<Element> getProxySnappedElements(Element self, Seq<Element> check){
        //todo replace check with quadtree
        Seq<Element> tmp = new Seq<>();
        check.each(e -> {
            if (isElementSnapped(self, e)){
                tmp.add(e);
            }
        });
        return tmp;
    }

    public void getSnappedElements(Seq<Element> out){
        Queue<Element> queue = new Queue<>();

        out.add(this);
        queue.addLast(this);

        while (!queue.isEmpty()){
            Element element = queue.removeFirst();
            for (Element proxy: getProxySnappedElements(element, getAllElements())){
                if (isElementSnapped(element, proxy) && !out.contains(proxy)){
                    out.add(proxy);
                    queue.addLast(proxy);
                }
            }
        }
    }

    public float leftBound(Element element){
        return element.x;
    }

    public float bottomBound(Element element){
        return element.y;
    }

    public float rightBound(Element element){
        return element.x + element.getWidth() * element.scaleX;
    }

    public float topBound(Element element){
        return element.y + element.getHeight() * element.scaleY;
    }

    public float width(Element element){
        return element.getWidth() * element.scaleX;
    }

    public float height(Element element){
        return element.getHeight() * element.scaleY;
    }

    public void updateDragging(){
        dragging = hasMouse() && input.keyDown(KeyCode.controlLeft);
    }

    @Override
    public void hitbox(Rect out) {
        out.set(x, y, width, hoverTime);
    }
}
