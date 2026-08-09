package me.krunsh.kgui.menu.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MenuCompilerTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void bundledMenusRemainAcceptedDuringMigration() {
        File resources = new File("src/main/resources");
        MenuCompilationResult result = new MenuCompiler(
            new File(resources, "templates"), new File(resources, "menus")).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        assertTrue(result.getMenus().size() >= 20);
        assertEquals(0, result.getWarningCount());
    }

    @Test
    public void documentedExamplesAreCompiledByTheTestSuite() throws Exception {
        File emptyTemplates = temporary.newFolder("documented-templates");
        File examples = new File("../docs/examples");

        MenuCompilationResult result = new MenuCompiler(emptyTemplates, examples).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        assertEquals(1, result.getMenus().size());
        assertTrue(result.getMenus().containsKey("menu-v2"));
    }

    @Test
    public void missingSchemaVersionIsAcceptedWithMigrationWarning() throws Exception {
        Layout layout = layout();
        write(layout.menus, "legacy.yml", "title: Legacy\nsize: 9\nitems: {}\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        assertEquals(1, result.getWarningCount());
        assertNotNull(find(result, "LEGACY_SCHEMA"));
    }

    @Test
    public void completeV2SchemaCompilesIntoImmutableResolvedModel() throws Exception {
        Layout layout = layout();
        write(layout.templates, "base.yml",
            "schema_version: 2\n" +
            "title: '&8Base'\n" +
            "size: 54\n" +
            "open_actions: ['[sound] CLICK']\n" +
            "items:\n" +
            "  inherited:\n" +
            "    slot: 0\n" +
            "    material: STONE\n");
        write(layout.menus, "complete.yml", completeMenu());

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        assertEquals(0, result.getWarningCount());
        CompiledMenu menu = result.getMenus().get("complete");
        assertNotNull(menu);
        assertEquals(54, menu.getSize());
        assertEquals("base", menu.getTemplateId());
        assertTrue(menu.getItems().containsKey("inherited"));
        assertTrue(menu.getItems().containsKey("everything"));
        assertEquals(2, menu.toYamlConfiguration().getStringList("open_actions").size());
        try {
            menu.getConfig().put("title", "mutable");
            throw new AssertionError("compiled config should be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void unknownKeyIsLocalizedToFileLineAndPath() throws Exception {
        Layout layout = layout();
        write(layout.menus, "broken.yml",
            "schema_version: 2\n" +
            "title: Test\n" +
            "size: 9\n" +
            "typo_key: true\n" +
            "items: {}\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertFalse(result.isSuccess());
        MenuDiagnostic diagnostic = find(result, "UNKNOWN_KEY");
        assertNotNull(diagnostic);
        assertEquals("broken.yml", diagnostic.getSource());
        assertEquals("typo_key", diagnostic.getPath());
        assertEquals(4, diagnostic.getLine());
    }

    @Test
    public void invalidNestedItemKeyKeepsItsFullYamlPath() throws Exception {
        Layout layout = layout();
        write(layout.menus, "nested.yml",
            "schema_version: 2\nsize: 9\nitems:\n  icon:\n    slot: 0\n    material: STONE\n    display_nam: Typo\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        MenuDiagnostic diagnostic = find(result, "UNKNOWN_KEY");
        assertNotNull(diagnostic);
        assertEquals("items.icon.display_nam", diagnostic.getPath());
        assertEquals(7, diagnostic.getLine());
    }

    @Test
    public void invalidValueReportsItsExactKeyAndLine() throws Exception {
        Layout layout = layout();
        write(layout.menus, "invalid-value.yml",
            "schema_version: 2\nsize: 10\nitems:\n  icon:\n    slot: 10\n    material: STONE\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        MenuDiagnostic size = find(result, "INVALID_SIZE");
        assertNotNull(size);
        assertEquals("size", size.getPath());
        assertEquals(2, size.getLine());
        MenuDiagnostic slot = find(result, "SLOT_OUT_OF_BOUNDS");
        assertNotNull(slot);
        assertEquals("items.icon.slot", slot.getPath());
        assertEquals(5, slot.getLine());
    }

    @Test
    public void inheritanceCycleIsRejected() throws Exception {
        Layout layout = layout();
        write(layout.templates, "a.yml", "schema_version: 2\ntemplate: b\nitems: {}\n");
        write(layout.templates, "b.yml", "schema_version: 2\nextends: a\nitems: {}\n");
        write(layout.menus, "menu.yml", "schema_version: 2\ntemplate: a\nitems: {}\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertFalse(result.isSuccess());
        assertNotNull(find(result, "TEMPLATE_CYCLE"));
    }

    @Test
    public void duplicateYamlKeyIsRejectedBeforePublication() throws Exception {
        Layout layout = layout();
        write(layout.menus, "duplicate.yml",
            "schema_version: 2\ntitle: First\ntitle: Second\nsize: 9\nitems: {}\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertFalse(result.isSuccess());
        assertNotNull(find(result, "DUPLICATE_KEY"));
    }

    @Test
    public void legacyInheritanceAliasesRemainSupportedSeparately() throws Exception {
        Layout layout = layout();
        write(layout.templates, "base.yml", "schema_version: 2\nsize: 9\nitems: {}\n");
        write(layout.menus, "extends_alias.yml", "schema_version: 2\nextends: base\nitems: {}\n");
        write(layout.menus, "inherit_alias.yml", "schema_version: 2\ninherit_from: base\nitems: {}\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        assertEquals("base", result.getMenus().get("extends_alias").getTemplateId());
        assertEquals("base", result.getMenus().get("inherit_alias").getTemplateId());
    }

    @Test
    public void inheritedItemCanOverrideOnlySelectedProperties() throws Exception {
        Layout layout = layout();
        write(layout.templates, "base.yml",
            "schema_version: 2\nsize: 9\nitems:\n  icon:\n    slot: 2\n    material: STONE\n    lore: [Base]\n");
        write(layout.menus, "child.yml",
            "schema_version: 2\ntemplate: base\nitems:\n  icon:\n    display_name: Child\n");

        MenuCompilationResult result = new MenuCompiler(layout.templates, layout.menus).compileAll();

        assertTrue(diagnostics(result), result.isSuccess());
        CompiledItem item = result.getMenus().get("child").getItems().get("icon");
        assertEquals(Integer.valueOf(2), item.getSlots().get(0));
        assertEquals("STONE", item.getConfig().get("material"));
        assertEquals("Child", item.getConfig().get("display_name"));
    }

    private Layout layout() throws Exception {
        File root = temporary.newFolder();
        File templates = new File(root, "templates");
        File menus = new File(root, "menus");
        assertTrue(templates.mkdirs());
        assertTrue(menus.mkdirs());
        return new Layout(templates, menus);
    }

    private static void write(File folder, String name, String content) throws Exception {
        Files.write(new File(folder, name).toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static MenuDiagnostic find(MenuCompilationResult result, String code) {
        for (MenuDiagnostic diagnostic : result.getDiagnostics()) {
            if (code.equals(diagnostic.getCode())) return diagnostic;
        }
        return null;
    }

    private static String diagnostics(MenuCompilationResult result) {
        StringBuilder output = new StringBuilder();
        for (MenuDiagnostic diagnostic : result.getDiagnostics()) output.append(diagnostic.format()).append('\n');
        return output.toString();
    }

    private static String completeMenu() {
        return "schema_version: 2\n" +
            "id: complete\n" +
            "template: base\n" +
            "title: '&6Complete'\n" +
            "size: 54\n" +
            "type: scroll\n" +
            "open_command: complete\n" +
            "open_commands: [complete, cgui]\n" +
            "open_actions: ['[message] open']\n" +
            "close_actions: ['[message] close']\n" +
            "open_requirements:\n" +
            "  permission_check:\n" +
            "    type: permission\n" +
            "    permission: kgui.complete\n" +
            "permission: kgui.open.complete\n" +
            "block_in_combat: false\n" +
            "allowed_worlds: [world]\n" +
            "blocked_worlds: [disabled]\n" +
            "open_on_region_enter: [spawn]\n" +
            "allowed_regions: [spawn]\n" +
            "blocked_regions: [pvp]\n" +
            "cooldown: 2\n" +
            "update_interval: 20\n" +
            "content_slots: '9-17'\n" +
            "prev_button_slot: 45\n" +
            "next_button_slot: 53\n" +
            "max_pages: 10\n" +
            "provider: root_provider\n" +
            "provider_args: {mode: root}\n" +
            "empty_message: Empty root\n" +
            "empty_item: {material: BARRIER, display_name: Empty}\n" +
            "pagination:\n" +
            "  enabled: true\n" +
            "  content_slots: [9, 10, 11]\n" +
            "  prev_button_slot: 45\n" +
            "  next_button_slot: 53\n" +
            "  max_pages: 5\n" +
            "  provider: test_provider\n" +
            "  provider_args: {filter: all}\n" +
            "  empty_message: Nothing\n" +
            "  empty_item: {material: BARRIER, name: Empty}\n" +
            "animations: {pulse: {interval: 10}}\n" +
            "items:\n" +
            "  everything:\n" +
            "    slot: 1\n" +
            "    slots: '2-3'\n" +
            "    item_id: shared\n" +
            "    item: shared_alias\n" +
            "    material: STONE\n" +
            "    data: 1\n" +
            "    display_name: Display\n" +
            "    name: Name\n" +
            "    lore: [Line]\n" +
            "    glow: true\n" +
            "    skull: player\n" +
            "    skull_owner: owner\n" +
            "    head_database: '1'\n" +
            "    hdb: '2'\n" +
            "    cit: icon\n" +
            "    cit_key: icon_alias\n" +
            "    amount: '%amount%'\n" +
            "    priority: 10\n" +
            "    view_requirements: ['permission: kgui.view']\n" +
            "    view_requirement: {type: permission, permission: kgui.view}\n" +
            "    click_requirements: ['money: 1']\n" +
            "    click_requirement: {type: permission, permission: kgui.click}\n" +
            "    left_click_actions: ['[message] left']\n" +
            "    right_click_actions: ['[message] right']\n" +
            "    shift_click_actions: ['[message] shift']\n" +
            "    middle_click_actions: ['[message] middle']\n" +
            "    click_actions: ['[message] click']\n" +
            "    deny_actions: ['[message] deny']\n" +
            "    cooldown: 1\n" +
            "    update: true\n" +
            "    animation: pulse\n" +
            "    hide_attributes: true\n" +
            "    kgui_protected: true\n";
    }

    private static final class Layout {
        private final File templates;
        private final File menus;

        private Layout(File templates, File menus) {
            this.templates = templates;
            this.menus = menus;
        }
    }
}
