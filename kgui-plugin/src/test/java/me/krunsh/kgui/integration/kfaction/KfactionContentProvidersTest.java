package me.krunsh.kgui.integration.kfaction;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import me.krunsh.kfaction.api.v2.KfactionApiV23;
import me.krunsh.kfaction.api.v2.KfactionPlayerActions;
import me.krunsh.kgui.api.ContentProvider;
import me.krunsh.kgui.api.ContentRequest;
import me.krunsh.kgui.api.ContentSnapshot;
import me.krunsh.kgui.api.KguiApi;
import me.krunsh.kgui.api.MenuArguments;
import me.krunsh.kgui.api.OwnedRegistration;
import me.krunsh.kgui.api.ProviderRegistration;

public class KfactionContentProvidersTest {

    @Test
    public void factionlessSnapshotKeepsAMonotonicRevisionAfterInvalidation() {
        KfactionApiV23 api = proxy(KfactionApiV23.class);
        KfactionPlayerActions actions = proxy(KfactionPlayerActions.class);
        KfactionContentProviders providers = new KfactionContentProviders(api, actions);
        final Map<String, ContentProvider> registered = new LinkedHashMap<String, ContentProvider>();
        KguiApi kgui = (KguiApi) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { KguiApi.class }, (instance, method, arguments) -> {
                    if ("registerProvider".equals(method.getName())) {
                        registered.put((String) arguments[1], (ContentProvider) arguments[2]);
                        return proxy(ProviderRegistration.class);
                    }
                    return defaultValue(method.getReturnType());
                });

        providers.register(kgui, null, new ArrayList<OwnedRegistration>());
        ContentProvider members = registered.get("kfaction:members");
        assertNotNull(members);
        ContentRequest request = new ContentRequest(UUID.randomUUID(), "faction_members",
                "kfaction:members", 0, 21, -1L, MenuArguments.empty());

        ContentSnapshot before = members.getContent(request);
        providers.invalidateRevision();
        ContentSnapshot after = members.getContent(request);

        assertTrue(before.getItems().isEmpty());
        assertTrue(after.getItems().isEmpty());
        assertTrue(after.getRevision() > before.getRevision());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (instance, method, arguments) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == Boolean.TYPE) return Boolean.FALSE;
        if (type == Character.TYPE) return Character.valueOf('\0');
        if (type == Byte.TYPE) return Byte.valueOf((byte) 0);
        if (type == Short.TYPE) return Short.valueOf((short) 0);
        if (type == Integer.TYPE) return Integer.valueOf(0);
        if (type == Long.TYPE) return Long.valueOf(0L);
        if (type == Float.TYPE) return Float.valueOf(0F);
        if (type == Double.TYPE) return Double.valueOf(0D);
        return null;
    }
}
