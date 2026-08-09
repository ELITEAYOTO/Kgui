package me.krunsh.kgui.render;

import me.krunsh.kgui.menu.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Comportement serveur associe au slot rendu ; il n'est jamais serialise dans l'item. */
public final class ClickBinding {
    private final String id;
    private final List<Map<String, Object>> requirements;
    private final List<String> denyActions;
    private final List<String> actions;
    private final List<String> leftActions;
    private final List<String> rightActions;
    private final List<String> shiftActions;
    private final List<String> middleActions;
    private final int cooldownTicks;

    private ClickBinding(String id, List<Map<String, Object>> requirements, List<String> denyActions,
                         List<String> actions, List<String> leftActions, List<String> rightActions,
                         List<String> shiftActions, List<String> middleActions, int cooldownTicks) {
        this.id = Objects.requireNonNull(id, "id");
        this.requirements = immutableRequirements(requirements);
        this.denyActions = immutable(denyActions);
        this.actions = immutable(actions);
        this.leftActions = immutable(leftActions);
        this.rightActions = immutable(rightActions);
        this.shiftActions = immutable(shiftActions);
        this.middleActions = immutable(middleActions);
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public static ClickBinding forMenuItem(MenuItem item) {
        return new ClickBinding("menu:" + item.getKey(), item.getClickRequirements(), item.getDenyActions(),
            item.getClickActions(), item.getLeftClickActions(), item.getRightClickActions(),
            item.getShiftClickActions(), item.getMiddleClickActions(), item.getCooldown());
    }

    public static ClickBinding forDynamicItem(String id, List<String> actions, List<String> leftActions,
                                               List<String> rightActions, List<String> shiftActions) {
        return new ClickBinding("provider:" + id, Collections.<Map<String, Object>>emptyList(),
            Collections.<String>emptyList(), actions, leftActions, rightActions, shiftActions,
            Collections.<String>emptyList(), 0);
    }

    public String getId() {
        return id;
    }

    public List<Map<String, Object>> getRequirements() {
        return requirements;
    }

    public List<String> getDenyActions() {
        return denyActions;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public List<String> actionsFor(GuiClick click) {
        if (click == null || click == GuiClick.UNSUPPORTED) return Collections.emptyList();
        List<String> selected;
        switch (click) {
            case LEFT: selected = leftActions; break;
            case RIGHT: selected = rightActions; break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT: selected = shiftActions; break;
            case MIDDLE: selected = middleActions; break;
            default: return Collections.emptyList();
        }
        return selected.isEmpty() ? actions : selected;
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<Map<String, Object>> immutableRequirements(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> value : values) {
            copy.add(value == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(value)));
        }
        return Collections.unmodifiableList(copy);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ClickBinding)) return false;
        ClickBinding that = (ClickBinding) other;
        return cooldownTicks == that.cooldownTicks && id.equals(that.id)
            && requirements.equals(that.requirements) && denyActions.equals(that.denyActions)
            && actions.equals(that.actions) && leftActions.equals(that.leftActions)
            && rightActions.equals(that.rightActions) && shiftActions.equals(that.shiftActions)
            && middleActions.equals(that.middleActions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requirements, denyActions, actions, leftActions, rightActions,
            shiftActions, middleActions, cooldownTicks);
    }
}
