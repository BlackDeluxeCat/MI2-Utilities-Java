package mi2u.ui.stats;

import arc.struct.*;

public abstract class TeamDataMonitor extends Monitor{
    public static Seq<TeamDataMonitor> monitors = new Seq<>();
    public int team;

    public TeamDataMonitor(){
        super();
    }
}
