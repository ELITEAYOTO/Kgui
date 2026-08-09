package me.krunsh.kgui.refresh;

import me.krunsh.kgui.api.InvalidationRequest;
import me.krunsh.kgui.metrics.GuiMetrics;
import me.krunsh.kgui.provider.ProviderEngine;
import me.krunsh.kgui.session.SessionToken;

/** Route une invalidation vers les seuls tokens indexés qui correspondent. */
public final class GuiInvalidationBus implements AutoCloseable {
    private final InvalidationIndex index = new InvalidationIndex();
    private final RefreshScheduler scheduler;
    private final ProviderEngine providers;
    private final GuiMetrics metrics;

    public GuiInvalidationBus(RefreshScheduler scheduler, ProviderEngine providers, GuiMetrics metrics) {
        this.scheduler = scheduler;
        this.providers = providers;
        this.metrics = metrics;
    }

    public void register(SessionToken token, String menuId, String providerId, boolean acceptsEvents) {
        index.register(token, menuId, providerId, acceptsEvents);
    }

    public void unregister(SessionToken token) {
        index.unregister(token);
        scheduler.unregister(token);
    }

    public int publish(InvalidationRequest request) {
        if (request == null) return 0;
        metrics.invalidation();
        providers.invalidate(request);
        int matched = 0;
        for (SessionToken token : index.resolve(request)) {
            RefreshRequest refresh = new RefreshRequest(token, RefreshPriority.INVALIDATION,
                true, placeholdersAffected(request), request.getItemIds());
            if (scheduler.request(refresh) != BoundedRefreshQueue.OfferResult.REJECTED) matched++;
        }
        return matched;
    }

    public int indexedSessions() { return index.size(); }

    @Override public void close() { index.clear(); }

    static boolean placeholdersAffected(InvalidationRequest request) {
        // Une invalidation non ciblée peut modifier total/pages et doit donc
        // reconstruire les boutons et textes statiques. Des itemIds explicites
        // garantissent au contraire que seule la couche provider est sale.
        return request.getItemIds().isEmpty();
    }
}
