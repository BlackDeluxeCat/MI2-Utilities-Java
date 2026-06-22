package mi2u.ui.capability;

/**
 * 最小化动作的能力事件。
 * <p>
 * 使用方自行持有单例实例，不复用池。
 */
public class MinimizeCapabilityEvent extends CapabilityEvent{
    public boolean minimized;

    public MinimizeCapabilityEvent setMinimized(boolean minimized){
        this.minimized = minimized;
        return this;
    }
}
