package me.krunsh.kgui.integration.kfaction;

import me.krunsh.kfaction.api.v2.ApiResult;
import me.krunsh.kgui.api.ActionResult;
import me.krunsh.kgui.api.ProviderClickResult;

/** Conversion exhaustive et centralisée du contrat de mutation Kfaction. */
final class KfactionResultMapper {

    private KfactionResultMapper() {
    }

    static ActionResult action(ApiResult<?> result) {
        if (result == null) return ActionResult.error("kfaction-action-failed", "null ApiResult");
        switch (result.getStatus()) {
            case SUCCESS:
                return ActionResult.handledAndInvalidate();
            case NO_CHANGE:
                return new ActionResult(ActionResult.Status.HANDLED, false,
                        "kfaction-no-change", result.getDetail());
            case CANCELLED:
                return ActionResult.denied("kfaction-cancelled");
            case INVALID_INPUT:
                return ActionResult.denied("kfaction-invalid-input");
            case NOT_FOUND:
                return ActionResult.denied("kfaction-not-found");
            case FORBIDDEN:
                return ActionResult.denied("kfaction-forbidden");
            case CONFLICT:
                return ActionResult.denied("kfaction-conflict");
            case LIMIT_REACHED:
                return ActionResult.denied("kfaction-limit-reached");
            case UNAVAILABLE:
                return ActionResult.denied("kfaction-unavailable");
            case FAILED:
                return ActionResult.error("kfaction-action-failed", detail(result));
            default:
                return ActionResult.error("kfaction-action-failed", "unknown ApiResult status");
        }
    }

    static ProviderClickResult click(ApiResult<?> result) {
        ActionResult mapped = action(result);
        switch (mapped.getStatus()) {
            case HANDLED:
                return mapped.shouldInvalidate() ? ProviderClickResult.handledAndInvalidate()
                        : ProviderClickResult.handled();
            case DENIED:
                return ProviderClickResult.denied(mapped.getMessageKey());
            case ERROR:
                return ProviderClickResult.error(mapped.getMessageKey());
            case IGNORED:
            default:
                return ProviderClickResult.ignored();
        }
    }

    private static String detail(ApiResult<?> result) {
        if (result.getDetail() != null) return result.getDetail();
        if (result.getMessageKey() != null) return result.getMessageKey();
        return result.getStatus().name();
    }
}
