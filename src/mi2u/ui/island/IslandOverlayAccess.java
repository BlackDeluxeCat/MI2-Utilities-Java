package mi2u.ui.island;

public interface IslandOverlayAccess {
    Island getRoot();

    void loadFromJson(String json);

    float getAutoSaveCooldown();
}
