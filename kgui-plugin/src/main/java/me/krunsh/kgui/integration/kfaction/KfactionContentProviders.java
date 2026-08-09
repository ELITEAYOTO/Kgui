package me.krunsh.kgui.integration.kfaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.krunsh.kfaction.api.v2.ApiResult;
import me.krunsh.kfaction.api.v2.ClaimView;
import me.krunsh.kfaction.api.v2.FactionInviteView;
import me.krunsh.kfaction.api.v2.FactionLogQuery;
import me.krunsh.kfaction.api.v2.FactionLogView;
import me.krunsh.kfaction.api.v2.FactionView;
import me.krunsh.kfaction.api.v2.KfactionApiV23;
import me.krunsh.kfaction.api.v2.KfactionPlayerActions;
import me.krunsh.kfaction.api.v2.MemberView;
import me.krunsh.kfaction.api.v2.PageRequest;
import me.krunsh.kfaction.api.v2.PageView;
import me.krunsh.kfaction.api.v2.QuestView;
import me.krunsh.kfaction.api.v2.RelationRequestView;
import me.krunsh.kfaction.api.v2.RelationView;
import me.krunsh.kfaction.api.v2.RewardLevelView;
import me.krunsh.kfaction.api.v2.WarpView;
import me.krunsh.kfaction.api.v2.ZoneView;
import me.krunsh.kfaction.core.operation.OperationContext;
import me.krunsh.kfaction.core.operation.OperationSource;
import me.krunsh.kgui.api.ContentItem;
import me.krunsh.kgui.api.ContentProvider;
import me.krunsh.kgui.api.ContentRequest;
import me.krunsh.kgui.api.ContentSnapshot;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.OwnedRegistration;
import me.krunsh.kgui.api.ProviderClickContext;
import me.krunsh.kgui.api.ProviderClickResult;

/** Providers snapshot-only du pack Kfaction. */
final class KfactionContentProviders {

    private final KfactionApiV23 api;
    private final KfactionPlayerActions actions;
    private final AtomicLong revisionEpoch = new AtomicLong(System.currentTimeMillis());
    private long zoneFingerprint = Long.MIN_VALUE;
    private long zoneRevision = System.currentTimeMillis();

    KfactionContentProviders(KfactionApiV23 api, KfactionPlayerActions actions) {
        this.api = api;
        this.actions = actions;
    }

    void register(KguiApi kgui, Plugin owner, List<OwnedRegistration> handles) {
        add(kgui, owner, handles, "members", this::members);
        add(kgui, owner, handles, "claims", this::claims);
        add(kgui, owner, handles, "logs", this::logs);
        add(kgui, owner, handles, "warps", new WarpProvider());
        add(kgui, owner, handles, "quests", this::quests);
        add(kgui, owner, handles, "rewards", this::rewards);
        add(kgui, owner, handles, "relations", this::relations);
        add(kgui, owner, handles, "invites", this::invites);
        add(kgui, owner, handles, "zones", this::zones);
    }

    private ContentSnapshot members(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (MemberView member : slice(faction.getMembers(), request)) {
            Map<String, String> attributes = attributes("member_uuid", text(member.getUuid()));
            if (member.getName() != null) attributes.put("skull_owner", member.getName());
            items.add(item("member/" + member.getUuid(), "SKULL_ITEM", (short) 3,
                    member.isOnline() ? "&a" : "&7", safe(member.getName(), text(member.getUuid())),
                    Arrays.asList("&7Rôle: &f" + safe(member.getRole(), "INCONNU"),
                            member.isOnline() ? "&aEn ligne" : "&8Hors ligne"), attributes));
        }
        return new ContentSnapshot(revision(), items, faction.getMembers().size());
    }

    private ContentSnapshot claims(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        PageView<ClaimView> page = api.getFactionClaims(faction.getId(), page(request));
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (ClaimView claim : page.getItems()) {
            String key = claim.getChunk().getKey();
            items.add(item("claim/" + stableId(key), "GRASS", (short) 0, "&2",
                    "Chunk " + claim.getChunk().getX() + ", " + claim.getChunk().getZ(),
                    Arrays.asList("&7Monde: &f" + claim.getChunk().getWorld(),
                            "&7Groupe: &f" + safe(claim.getGroupId(), "aucun")),
                    attributes("chunk", key)));
        }
        return new ContentSnapshot(revision(), items, page.getTotal());
    }

    private ContentSnapshot logs(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        String type = request.getArguments().get("type");
        PageView<FactionLogView> page = api.getFactionLogs(faction.getId(),
                new FactionLogQuery(page(request), type, null));
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (FactionLogView log : page.getItems()) {
            List<String> lore = new ArrayList<String>();
            lore.add("&7Acteur: &f" + safe(log.getActorName(), "SYSTEM"));
            if (log.getTargetName() != null) lore.add("&7Cible: &f" + log.getTargetName());
            if (log.getDetails() != null) lore.add("&8" + log.getDetails());
            lore.add("&8Timestamp: " + log.getTimestamp());
            items.add(item("log/" + stableId(safe(log.getId(), text(log.getTimestamp()))),
                    "PAPER", (short) 0, "&e", safe(log.getType(), "LOG"), lore,
                    attributes("log_id", safe(log.getId(), ""))));
        }
        return new ContentSnapshot(revision(), items, page.getTotal());
    }

    private ContentSnapshot quests(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        List<QuestView> quests = api.getProgressionQuests(faction.getId());
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (QuestView quest : slice(quests, request)) {
            List<String> lore = new ArrayList<String>(quest.getLore());
            lore.add("&7Progression: &e" + quest.getProgress() + "&7/&e" + quest.getRequired());
            lore.add(quest.isCompleted() ? "&aTerminée" : "&7Restant: &f" + quest.getRemaining());
            items.add(item("quest/" + stableId(quest.getId()), safe(quest.getIconMaterial(), "BOOK"),
                    (short) quest.getIconData(), quest.isCompleted() ? "&a" : "&e",
                    safe(quest.getDisplayName(), quest.getId()), lore,
                    attributes("quest_id", quest.getId())));
        }
        return new ContentSnapshot(revision(), items, quests.size());
    }

    private ContentSnapshot rewards(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        List<RewardLevelView> rewards = api.getRewardLevels(faction.getId());
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (RewardLevelView reward : slice(rewards, request)) {
            short data = reward.isUnlocked() ? (short) 5 : reward.isCurrent() ? (short) 4 : (short) 14;
            items.add(item("reward/" + reward.getLevel(), "STAINED_GLASS_PANE", data,
                    reward.isUnlocked() ? "&a" : reward.isCurrent() ? "&e" : "&c",
                    "Niveau " + reward.getLevel(), reward.getRewards(),
                    attributes("level", String.valueOf(reward.getLevel()))));
        }
        return new ContentSnapshot(revision(), items, rewards.size());
    }

    private ContentSnapshot relations(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        List<ContentItem> all = new ArrayList<ContentItem>();
        for (RelationRequestView pending : api.getRelationRequests(faction.getId())) {
            all.add(item("request/" + stableId(pending.getSourceFactionId() + pending.getRelation()),
                    "PAPER", (short) 0, "&e", "Demande " + pending.getRelation(),
                    Arrays.asList("&7Depuis: &f" + pending.getSourceFactionId(),
                            "&7État: &f" + pending.getStatus().name()),
                    attributes("source_faction_id", pending.getSourceFactionId())));
        }
        for (RelationView relation : api.getRelations(faction.getId())) {
            all.add(item("relation/" + stableId(relation.getOtherFactionId()),
                    "INK_SACK", relationData(relation.getRelation()), "&6",
                    relation.getOtherFactionId(), Collections.singletonList(
                            "&7Relation: &f" + relation.getRelation()),
                    attributes("target_faction_id", relation.getOtherFactionId())));
        }
        return new ContentSnapshot(revision(), slice(all, request), all.size());
    }

    private ContentSnapshot invites(ContentRequest request) {
        FactionView faction = faction(request.getPlayerId());
        if (faction == null) return ContentSnapshot.empty(revision());
        List<FactionInviteView> invites = api.getFactionInvites(faction.getId());
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (FactionInviteView invite : slice(invites, request)) {
            Player online = Bukkit.getPlayer(invite.getPlayerId());
            String name = online != null ? online.getName() : invite.getPlayerId().toString();
            items.add(item("invite/" + invite.getPlayerId(), "NAME_TAG", (short) 0, "&d", name,
                    Arrays.asList("&7État: &f" + invite.getStatus().name(),
                            "&8Expiration: " + invite.getExpiresAt()),
                    attributes("player_uuid", invite.getPlayerId().toString())));
        }
        return new ContentSnapshot(revision(), items, invites.size());
    }

    private ContentSnapshot zones(ContentRequest request) {
        List<ZoneView> zones = api.getGlobalZones();
        List<ContentItem> items = new ArrayList<ContentItem>();
        for (ZoneView zone : slice(zones, request)) {
            items.add(item("zone/" + stableId(zone.getId()), "MAP", (short) 0,
                    safe(zone.getColor(), "&6"), safe(zone.getDisplayName(), zone.getId()),
                    Arrays.asList("&7ID: &f" + zone.getId(),
                            "&7Chunks: &f" + zone.getChunkCount(),
                            "&7PVP: " + (zone.isPvpAllowed() ? "&aoui" : "&cnon"),
                            "&7Politique: &f" + safe(zone.getDefaultPolicy(), "défaut")),
                    attributes("zone_id", zone.getId())));
        }
        return new ContentSnapshot(zoneRevision(zones), items, zones.size());
    }

    private FactionView faction(UUID playerId) {
        return playerId == null ? null : api.getPlayerFaction(playerId);
    }

    void invalidateRevision() {
        revisionEpoch.incrementAndGet();
    }

    private long revision() {
        return revisionEpoch.get();
    }

    private static PageRequest page(ContentRequest request) {
        return new PageRequest(request.getOffset(), request.getLimit());
    }

    private static <T> List<T> slice(List<T> source, ContentRequest request) {
        if (source == null || source.isEmpty() || request.getOffset() >= source.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(source.size(), request.getOffset() + request.getLimit());
        return new ArrayList<T>(source.subList(request.getOffset(), end));
    }

    private static ContentItem item(String id, String material, short data, String color,
                                    String name, List<String> lore, Map<String, String> attributes) {
        return new ContentItem(id, material(material), data, 1,
                bounded(safe(color, "") + safe(name, id), 512),
                lore(lore), attributes(attributes));
    }

    private static Map<String, String> attributes(String key, String value) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (key != null && value != null) result.put(key, value);
        return result;
    }

    private static Map<String, String> attributes(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (source == null) return result;
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (result.size() >= 64) break;
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || key.length() > 64
                    || !key.matches("[A-Za-z0-9_.-]+")) continue;
            result.put(key, bounded(value, 4096));
        }
        return result;
    }

    private static List<String> lore(List<String> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<String>(Math.min(64, source.size()));
        for (String line : source) {
            if (result.size() >= 64) break;
            if (line != null) result.add(bounded(line, 1024));
        }
        return result;
    }

    private static String material(String requested) {
        String candidate = safe(requested, "STONE").toUpperCase(java.util.Locale.ROOT);
        return Material.getMaterial(candidate) == null ? "STONE" : candidate;
    }

    private static String bounded(String value, int maximum) {
        if (value == null || value.length() <= maximum) return value;
        return value.substring(0, maximum);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stableId(String value) {
        String safe = safe(value, "unknown").toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
        if (safe.length() > 72) safe = safe.substring(0, 72);
        return safe + "_" + Integer.toHexString(value == null ? 0 : value.hashCode());
    }

    private static short relationData(String relation) {
        if ("ALLY".equalsIgnoreCase(relation)) return 10;
        if ("TRUCE".equalsIgnoreCase(relation)) return 11;
        if ("ENEMY".equalsIgnoreCase(relation)) return 1;
        return 8;
    }

    private synchronized long zoneRevision(List<ZoneView> zones) {
        long hash = 1125899906842597L;
        for (ZoneView zone : zones) {
            hash = 31L * hash + safe(zone.getId(), "").hashCode();
            hash = 31L * hash + safe(zone.getDisplayName(), "").hashCode();
            hash = 31L * hash + safe(zone.getColor(), "").hashCode();
            hash = 31L * hash + safe(zone.getDefaultPolicy(), "").hashCode();
            hash = 31L * hash + zone.getChunkCount();
            hash = 31L * hash + (zone.isPvpAllowed() ? 1 : 0);
        }
        if (hash != zoneFingerprint) {
            zoneFingerprint = hash;
            zoneRevision++;
        }
        return zoneRevision;
    }

    private static void add(KguiApi api, Plugin owner, List<OwnedRegistration> handles,
                            String id, ContentProvider provider) {
        handles.add(api.registerProvider(owner, "kfaction:" + id, provider));
    }

    private final class WarpProvider implements ContentProvider {
        @Override
        public ContentSnapshot getContent(ContentRequest request) {
            FactionView faction = faction(request.getPlayerId());
            if (faction == null) return ContentSnapshot.empty(revision());
            List<WarpView> warps = api.getFactionWarps(faction.getId());
            List<ContentItem> items = new ArrayList<ContentItem>();
            for (WarpView warp : slice(warps, request)) {
                items.add(item("warp/" + stableId(warp.getName()), "ENDER_PEARL", (short) 0, "&b",
                        warp.getName(), Arrays.asList("&7Monde: &f" + warp.getPosition().getWorld(),
                                "&7Position: &f" + (int) warp.getPosition().getX() + ", "
                                        + (int) warp.getPosition().getY() + ", "
                                        + (int) warp.getPosition().getZ(),
                                warp.isPasswordProtected() ? "&cMot de passe requis" : "&eClic: téléporter"),
                        attributes("warp_name", warp.getName())));
            }
            return new ContentSnapshot(revision(), items, warps.size());
        }

        @Override
        public ProviderClickResult onClick(ProviderClickContext context) {
            Player player = Bukkit.getPlayer(context.getPlayerId());
            FactionView faction = faction(context.getPlayerId());
            if (player == null || faction == null) return ProviderClickResult.stale();
            for (WarpView warp : api.getFactionWarps(faction.getId())) {
                if (("warp/" + stableId(warp.getName())).equals(context.getItemId())) {
                    ApiResult<Void> result = actions.teleportWarp(player.getUniqueId(), warp.getName(),
                            context.getArguments().get("password"), OperationContext.actor(
                                    player.getUniqueId(), player.getName(), OperationSource.GUI));
                    return KfactionResultMapper.click(result);
                }
            }
            return ProviderClickResult.stale();
        }
    }
}
