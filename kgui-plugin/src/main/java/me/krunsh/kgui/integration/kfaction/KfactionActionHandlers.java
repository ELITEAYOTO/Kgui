package me.krunsh.kgui.integration.kfaction;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.krunsh.kfaction.api.v2.ApiResult;
import me.krunsh.kfaction.api.v2.ChunkView;
import me.krunsh.kfaction.api.v2.KfactionPlayerActions;
import me.krunsh.kfaction.api.v2.PositionView;
import me.krunsh.kfaction.core.operation.OperationContext;
import me.krunsh.kfaction.core.operation.OperationSource;
import me.krunsh.kfaction.data.FactionRole;
import me.krunsh.kgui.api.ActionContext;
import me.krunsh.kgui.api.ActionHandler;
import me.krunsh.kgui.api.ActionResult;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.api.OwnedRegistration;

/** Enregistre tous les parcours joueur 2.3 sans exécuter de commande texte. */
final class KfactionActionHandlers {

    private final KfactionPlayerActions actions;

    KfactionActionHandlers(KfactionPlayerActions actions) {
        if (actions == null) throw new IllegalArgumentException("actions must not be null");
        this.actions = actions;
    }

    void register(KguiApi kgui, Plugin owner, List<OwnedRegistration> handles) {
        add(kgui, owner, handles, "create_faction", context -> call(context,
                (player, operation) -> actions.createFaction(player.getUniqueId(),
                        required(context, "name"), operation)));
        add(kgui, owner, handles, "invite_player", context -> call(context,
                (player, operation) -> actions.invitePlayer(player.getUniqueId(),
                        target(context), operation)));
        add(kgui, owner, handles, "accept_invite", context -> call(context,
                (player, operation) -> actions.acceptInvite(player.getUniqueId(),
                        required(context, "faction_id"), operation)));
        add(kgui, owner, handles, "decline_invite", context -> call(context,
                (player, operation) -> actions.declineInvite(player.getUniqueId(),
                        required(context, "faction_id"), operation)));
        add(kgui, owner, handles, "leave_faction", context -> call(context,
                (player, operation) -> actions.leaveFaction(player.getUniqueId(), operation)));
        add(kgui, owner, handles, "kick_member", context -> call(context,
                (player, operation) -> actions.kickMember(player.getUniqueId(), target(context), operation)));
        add(kgui, owner, handles, "change_role", context -> call(context,
                (player, operation) -> actions.changeMemberRole(player.getUniqueId(), target(context),
                        role(context), operation)));
        add(kgui, owner, handles, "transfer_leadership", context -> call(context,
                (player, operation) -> actions.transferLeadership(player.getUniqueId(), target(context), operation)));
        add(kgui, owner, handles, "disband_faction", context -> call(context,
                (player, operation) -> actions.disbandFaction(player.getUniqueId(), operation)));

        add(kgui, owner, handles, "claim", context -> call(context,
                (player, operation) -> actions.claim(player.getUniqueId(), chunk(player), operation)));
        add(kgui, owner, handles, "claim_radius", context -> call(context,
                (player, operation) -> actions.claimRadius(player.getUniqueId(), chunk(player),
                        integer(context, "radius", 1, 1, 32), operation)));
        add(kgui, owner, handles, "claim_fill", context -> call(context,
                (player, operation) -> actions.claimFill(player.getUniqueId(), chunk(player), operation)));
        add(kgui, owner, handles, "unclaim", context -> call(context,
                (player, operation) -> actions.unclaim(player.getUniqueId(), chunk(player), operation)));
        add(kgui, owner, handles, "unclaim_radius", context -> call(context,
                (player, operation) -> actions.unclaimRadius(player.getUniqueId(), chunk(player),
                        integer(context, "radius", 1, 1, 32), operation)));
        add(kgui, owner, handles, "unclaim_all", context -> call(context,
                (player, operation) -> actions.unclaimAll(player.getUniqueId(), operation)));

        add(kgui, owner, handles, "request_relation", context -> call(context,
                (player, operation) -> actions.requestRelation(player.getUniqueId(),
                        required(context, "target_faction_id"), required(context, "relation"), operation)));
        add(kgui, owner, handles, "accept_relation", context -> call(context,
                (player, operation) -> actions.acceptRelation(player.getUniqueId(),
                        required(context, "source_faction_id"), required(context, "relation"), operation)));
        add(kgui, owner, handles, "decline_relation", context -> call(context,
                (player, operation) -> actions.declineRelation(player.getUniqueId(),
                        required(context, "source_faction_id"), required(context, "relation"), operation)));
        add(kgui, owner, handles, "set_neutral", context -> call(context,
                (player, operation) -> actions.setNeutral(player.getUniqueId(),
                        required(context, "target_faction_id"), operation)));

        add(kgui, owner, handles, "deposit_bank", context -> call(context,
                (player, operation) -> actions.depositBank(player.getUniqueId(),
                        positiveLong(context, "amount_minor"), operation)));
        add(kgui, owner, handles, "withdraw_bank", context -> call(context,
                (player, operation) -> actions.withdrawBank(player.getUniqueId(),
                        positiveLong(context, "amount_minor"), operation)));

        add(kgui, owner, handles, "set_home", context -> call(context,
                (player, operation) -> actions.setHome(player.getUniqueId(), position(player), operation)));
        add(kgui, owner, handles, "teleport_home", context -> call(context,
                (player, operation) -> actions.teleportHome(player.getUniqueId(), operation)));
        add(kgui, owner, handles, "create_warp", context -> call(context,
                (player, operation) -> actions.createWarp(player.getUniqueId(),
                        required(context, "name"), position(player), operation)));
        add(kgui, owner, handles, "delete_warp", context -> call(context,
                (player, operation) -> actions.deleteWarp(player.getUniqueId(),
                        required(context, "name"), operation)));
        add(kgui, owner, handles, "teleport_warp", context -> call(context,
                (player, operation) -> actions.teleportWarp(player.getUniqueId(),
                        required(context, "name"), optional(context, "password"), operation)));

        add(kgui, owner, handles, "set_role_permission", context -> call(context,
                (player, operation) -> actions.setRolePermission(player.getUniqueId(), role(context),
                        required(context, "permission"), bool(context, "allowed"), operation)));
        add(kgui, owner, handles, "set_relation_permission", context -> call(context,
                (player, operation) -> actions.setRelationPermission(player.getUniqueId(),
                        required(context, "relation"), required(context, "permission"),
                        bool(context, "allowed"), operation)));
        add(kgui, owner, handles, "select_quest_category", context -> call(context,
                (player, operation) -> actions.selectQuestCategory(player.getUniqueId(),
                        required(context, "category_id"), operation)));
        add(kgui, owner, handles, "claim_progression_reward", context -> call(context,
                (player, operation) -> actions.claimProgressionReward(player.getUniqueId(),
                        integer(context, "level", 1, 1, Integer.MAX_VALUE), operation)));
    }

    private ActionResult call(ActionContext context, PlayerCall call) {
        try {
            if (!Bukkit.isPrimaryThread()) return ActionResult.error(
                    "kfaction-unavailable", "main thread required");
            Player player = Bukkit.getPlayer(context.getPlayerId());
            if (player == null || !player.isOnline()) return ActionResult.denied("kfaction-unavailable");
            OperationContext operation = OperationContext.actor(player.getUniqueId(),
                    player.getName(), OperationSource.GUI);
            return KfactionResultMapper.action(call.execute(player, operation));
        } catch (IllegalArgumentException failure) {
            return ActionResult.denied("kfaction-invalid-input");
        } catch (RuntimeException failure) {
            return ActionResult.error("kfaction-action-failed", failure.getMessage());
        }
    }

    private static void add(KguiApi api, Plugin owner, List<OwnedRegistration> handles,
                            String id, ActionHandler handler) {
        handles.add(api.registerAction(owner, "kfaction:" + id, handler));
    }

    private static String required(ActionContext context, String key) {
        String value = optional(context, key);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static String optional(ActionContext context, String key) {
        MenuArguments parameters = context.getParameters();
        String value = parameters.get(key);
        if ((value == null || value.trim().isEmpty()) && "name".equals(key)) value = parameters.get("raw");
        if (value == null || value.trim().isEmpty()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > 128 || trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("unsafe " + key);
        }
        return trimmed;
    }

    private static UUID target(ActionContext context) {
        String raw = optional(context, "target_uuid");
        if (raw == null) raw = required(context, "target");
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact(raw);
            if (online == null) throw new IllegalArgumentException("target must be online or a UUID");
            return online.getUniqueId();
        }
    }

    private static FactionRole role(ActionContext context) {
        FactionRole role = FactionRole.parse(required(context, "role"));
        if (role == null) throw new IllegalArgumentException("invalid role");
        return role;
    }

    private static int integer(ActionContext context, String key, int fallback, int minimum, int maximum) {
        String raw = optional(context, key);
        int value = raw == null ? fallback : Integer.parseInt(raw);
        if (value < minimum || value > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    private static long positiveLong(ActionContext context, String key) {
        long value = Long.parseLong(required(context, key));
        if (value <= 0L) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    private static boolean bool(ActionContext context, String key) {
        String raw = required(context, key).toLowerCase(Locale.ROOT);
        if ("true".equals(raw)) return true;
        if ("false".equals(raw)) return false;
        throw new IllegalArgumentException("invalid " + key);
    }

    private static ChunkView chunk(Player player) {
        Location location = player.getLocation();
        return new ChunkView(location.getWorld().getName(), location.getBlockX() >> 4,
                location.getBlockZ() >> 4);
    }

    private static PositionView position(Player player) {
        Location location = player.getLocation();
        return new PositionView(location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    private interface PlayerCall {
        ApiResult<?> execute(Player player, OperationContext operation);
    }
}
