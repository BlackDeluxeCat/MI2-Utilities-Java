package mi2u.ui.stats;

import arc.scene.ui.layout.*;

public abstract class Monitor{
    public float x, y;
    public int w = 4, h = 2;

    public Monitor(){}

    public abstract void build(Table table);

    public void buildCfg(Table table){}

    /**
     * 用于监视器在后台抓取数据。
     * */
    public void update(){}

    /**
     * 用于重置监视器数据。
     * */
    public void reset(){}
}