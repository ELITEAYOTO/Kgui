package me.krunsh.kgui.integration.kfaction;

import java.util.List;

import org.bukkit.plugin.Plugin;

import me.krunsh.kfaction.api.v2.FactionView;
import me.krunsh.kfaction.api.v2.KfactionApiV23;
import me.krunsh.kfaction.api.v2.PlayerView;
import me.krunsh.kfaction.data.FactionRole;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.OwnedRegistration;
import me.krunsh.kgui.api.RequirementContext;
import me.krunsh.kgui.api.RequirementHandler;
import me.krunsh.kgui.api.RequirementResult;

/** Conditions publiques fail-closed du pack Kfaction. */
final class KfactionRequirements {

    private final KfactionApiV23 api;

    KfactionRequirements(KfactionApiV23 api) {
        this.api = api;
    }

    void register(KguiApi kgui, Plugin owner, List<OwnedRegistration> handles) {
        add(kgui, owner, handles, "available", context -> RequirementResult.allowed());
        add(kgui, owner, handles, "has_faction", this::hasFaction);
        add(kgui, owner, handles, "role_at_least", this::roleAtLeast);
        add(kgui, owner, handles, "capability", this::capability);
        add(kgui, owner, handles, "relation", this::relation);
        add(kgui, owner, handles, "can_afford", this::canAfford);
    }

    private RequirementResult hasFaction(RequirementContext context) {
        PlayerView player = api.getPlayer(context.getPlayerId());
        return player != null && player.hasFaction() ? RequirementResult.allowed()
                : RequirementResult.denied("kfaction-faction-required");
    }

    private RequirementResult roleAtLeast(RequirementContext context) {
        PlayerView player = api.getPlayer(context.getPlayerId());
        FactionRole actual = player == null ? null : FactionRole.parse(player.getRole());
        FactionRole expected = FactionRole.parse(context.getParameters().get("role"));
        return actual != null && expected != null && actual.isAtLeast(expected)
                ? RequirementResult.allowed()
                : RequirementResult.denied("kfaction-role-required");
    }

    private RequirementResult capability(RequirementContext context) {
        PlayerView player = api.getPlayer(context.getPlayerId());
        String permission = context.getParameters().get("permission");
        if (player == null || !player.hasFaction() || player.getRole() == null || permission == null) {
            return RequirementResult.denied("kfaction-forbidden");
        }
        Boolean allowed = api.getRolePermission(player.getFactionId(), player.getRole(), permission);
        return Boolean.TRUE.equals(allowed) ? RequirementResult.allowed()
                : RequirementResult.denied("kfaction-forbidden");
    }

    private RequirementResult relation(RequirementContext context) {
        FactionView faction = api.getPlayerFaction(context.getPlayerId());
        String target = context.getParameters().get("target_faction_id");
        String expected = context.getParameters().get("relation");
        if (faction == null || target == null || expected == null) {
            return RequirementResult.denied("kfaction-relation-required");
        }
        String actual = api.getRelation(faction.getId(), target);
        return expected.equalsIgnoreCase(actual) ? RequirementResult.allowed()
                : RequirementResult.denied("kfaction-relation-required");
    }

    private RequirementResult canAfford(RequirementContext context) {
        FactionView faction = api.getPlayerFaction(context.getPlayerId());
        try {
            long amount = Long.parseLong(context.getParameters().get("amount_minor"));
            return faction != null && amount >= 0L && faction.getBankMinor() >= amount
                    ? RequirementResult.allowed()
                    : RequirementResult.denied("kfaction-cannot-afford");
        } catch (RuntimeException failure) {
            return RequirementResult.denied("kfaction-cannot-afford");
        }
    }

    private static void add(KguiApi api, Plugin owner, List<OwnedRegistration> handles,
                            String id, RequirementHandler handler) {
        handles.add(api.registerRequirement(owner, "kfaction:" + id, handler));
    }
}
