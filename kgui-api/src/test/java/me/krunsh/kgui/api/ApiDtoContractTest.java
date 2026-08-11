package me.krunsh.kgui.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashSet;
import org.junit.Test;

public class ApiDtoContractTest {
    private static final Class<?>[] DTO_TYPES = {
            MenuArguments.class, MenuOpenRequest.class, ContentRequest.class,
            ContentItem.class, ContentSnapshot.class, ProviderClickContext.class,
            ProviderClickResult.class, ActionContext.class, ActionResult.class,
            RequirementContext.class, RequirementResult.class,
            InvalidationRequest.class, KguiApiCompatibility.class
    };

    @Test
    public void dtoTypesAreFinalAndHaveNoMutableInstanceFields() {
        for (Class<?> type : DTO_TYPES) {
            assertTrue(type.getName(), Modifier.isFinal(type.getModifiers()));
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(type.getName() + "." + field.getName(), Modifier.isPrivate(field.getModifiers()));
                assertTrue(type.getName() + "." + field.getName(), Modifier.isFinal(field.getModifiers()));
            }
            for (Method method : type.getMethods()) {
                assertFalse(type.getName() + " exposes " + method.getName(), method.getName().startsWith("set"));
            }
        }
    }

    @Test
    public void collectionsAreDefensivelyCopiedAndReadOnly() {
        Map<String, String> input = new HashMap<>();
        input.put("faction", "alpha");
        MenuArguments arguments = new MenuArguments(input);
        input.put("faction", "changed");
        assertTrue("alpha".equals(arguments.get("faction")));

        ContentItem item = new ContentItem("member:one", "SKULL_ITEM", (short) 3, 1,
                "One", Arrays.asList("line"), input);
        boolean failed = false;
        try {
            item.getLore().add("mutation");
        } catch (UnsupportedOperationException expected) {
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            arguments.asMap().put("x", "y");
        } catch (UnsupportedOperationException expected) {
            failed = true;
        }
        assertTrue(failed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void contentRequestsAreBounded() {
        new ContentRequest(UUID.randomUUID(), "menu", "owner:provider", 0,
                ContentRequest.MAX_LIMIT + 1, 0, MenuArguments.empty());
    }

    @Test
    public void invalidationItemTargetsAreBounded() {
        java.util.Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < 400; index++) ids.add("item-" + index);
        InvalidationRequest request = InvalidationRequest.playerMenu(
            UUID.randomUUID(), "menu", ids, "test");
        assertTrue(request.getItemIds().size() <= 256);

        ids.clear();
        ids.add(repeat('x', 129));
        assertTrue(InvalidationRequest.playerMenu(UUID.randomUUID(), "menu", ids, "test")
            .getItemIds().isEmpty());
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
