package mi2u.ui.stats;

import arc.scene.ui.layout.*;

import static mi2u.ui.MonitorCanvas.unitSize;

/**
 * 监视器基类。
 * 一个监视器可以具有数据源、数据缓存、显示、设置。
 * 根据数据源类型继承基类，数据源声明为子类的实例字段，在设置中进行抓取。
 * 数据缓存应作为实例字段，按需声明，在{@code update}中更新。
 */
public abstract class Monitor{
    public float x, y;
    public float w = 6 * unitSize, h = 2 * unitSize;
    public transient float maxWidth = Float.POSITIVE_INFINITY, maxHeight = Float.POSITIVE_INFINITY, minWidth = 2 * unitSize, minHeight = 2 * unitSize;

    public Monitor(){}

    /**构建显示*/
    public void build(Table table){}

    /**构建数据设置*/
    public void buildCfg(Table table){}

    /**构建数据源抓取，抓取监听建议写在table的updater中*/
    public void buildFetch(Table table){}

    /**后台持续抓取缓存数据*/
    public void update(){}

    /**重置数据缓存*/
    public void reflush(){}

    /**确保数据源和缓存可用*/
    public abstract void validate();

    public abstract String title();
}