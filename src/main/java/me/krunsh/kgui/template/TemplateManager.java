package me.krunsh.kgui.template;

import me.krunsh.kgui.Kgui;
import me.krunsh.kgui.menu.MenuData;
import me.krunsh.kgui.menu.MenuItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Gestionnaire de templates de menus
 * Permet l'héritage et la réutilisation de configurations
 */
public class TemplateManager {

    private final Kgui plugin;
    
    // Cache des templates
    private final Map<String, MenuTemplate> templates = new HashMap<>();
    
    // Cache des templates d'items globaux
    private final Map<String, ItemTemplate> itemTemplates = new HashMap<>();

    public TemplateManager(Kgui plugin) {
        this.plugin = plugin;
    }

    /**
     * Template de menu
     */
    public static class MenuTemplate {
        private final String id;
        private String title;
        private int size = 54;
        private String parentTemplate;
        private Map<String, MenuItem> items = new HashMap<>();
        private List<String> openActions = new ArrayList<>();
        private List<String> closeActions = new ArrayList<>();
        private ConfigurationSection rawConfig;
        
        public MenuTemplate(String id) {
            this.id = id;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getParentTemplate() { return parentTemplate; }
        public void setParentTemplate(String parent) { this.parentTemplate = parent; }
        public Map<String, MenuItem> getItems() { return items; }
        public void addItem(MenuItem item) { this.items.put(item.getKey(), item); }
        public List<String> getOpenActions() { return openActions; }
        public void setOpenActions(List<String> actions) { this.openActions = actions; }
        public List<String> getCloseActions() { return closeActions; }
        public void setCloseActions(List<String> actions) { this.closeActions = actions; }
        public ConfigurationSection getRawConfig() { return rawConfig; }
        public void setRawConfig(ConfigurationSection config) { this.rawConfig = config; }
    }

    /**
     * Template d'item réutilisable
     */
    public static class ItemTemplate {
        private final String id;
        private String material;
        private short data = 0;
        private String name;
        private List<String> lore;
        private boolean glow = false;
        private String skull;
        private String hdb;
        private List<String> clickActions = new ArrayList<>();
        
        public ItemTemplate(String id) {
            this.id = id;
        }
        
        public String getId() { return id; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public short getData() { return data; }
        public void setData(short data) { this.data = data; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
        public boolean isGlow() { return glow; }
        public void setGlow(boolean glow) { this.glow = glow; }
        public String getSkull() { return skull; }
        public void setSkull(String skull) { this.skull = skull; }
        public String getHdb() { return hdb; }
        public void setHdb(String hdb) { this.hdb = hdb; }
        public List<String> getClickActions() { return clickActions; }
        public void setClickActions(List<String> actions) { this.clickActions = actions; }
    }

    /**
     * Charge les templates depuis le dossier templates/
     */
    public void loadTemplates() {
        templates.clear();
        
        File templateFolder = new File(plugin.getDataFolder(), "templates");
        if (!templateFolder.exists()) {
            templateFolder.mkdirs();
            createExampleTemplate(templateFolder);
        }
        
        File[] files = templateFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            loadTemplateFile(file);
        }
        
        // Résoudre les héritages
        resolveInheritance();
        
        plugin.getLogger().info("Loaded " + templates.size() + " menu template(s)");
    }

    /**
     * Charge un fichier de template
     */
    private void loadTemplateFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // Templates de menus
            ConfigurationSection menusSection = config.getConfigurationSection("menu_templates");
            if (menusSection != null) {
                for (String templateId : menusSection.getKeys(false)) {
                    ConfigurationSection templateConfig = menusSection.getConfigurationSection(templateId);
                    if (templateConfig != null) {
                        MenuTemplate template = parseMenuTemplate(templateId, templateConfig);
                        templates.put(templateId, template);
                    }
                }
            }
            
            // Templates d'items
            ConfigurationSection itemsSection = config.getConfigurationSection("item_templates");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemId);
                    if (itemConfig != null) {
                        ItemTemplate itemTemplate = parseItemTemplate(itemId, itemConfig);
                        itemTemplates.put(itemId, itemTemplate);
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load template file " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Parse un template de menu
     */
    private MenuTemplate parseMenuTemplate(String id, ConfigurationSection config) {
        MenuTemplate template = new MenuTemplate(id);
        
        template.setTitle(config.getString("title", "&8Menu Template"));
        template.setSize(config.getInt("size", 54));
        template.setParentTemplate(config.getString("inherit_from"));
        template.setRawConfig(config);
        
        if (config.contains("open_actions")) {
            template.setOpenActions(config.getStringList("open_actions"));
        }
        
        if (config.contains("close_actions")) {
            template.setCloseActions(config.getStringList("close_actions"));
        }
        
        // Parser les items du template
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemKey);
                if (itemConfig != null) {
                    MenuItem item = parseMenuItemTemplate(itemConfig, itemKey);
                    if (item != null) {
                        template.addItem(item);
                    }
                }
            }
        }
        
        return template;
    }

    /**
     * Parse un item de menu depuis une config de template
     */
    private MenuItem parseMenuItemTemplate(ConfigurationSection config, String key) {
        MenuItem item = new MenuItem(key);
        
        // Slots
        if (config.contains("slots")) {
            parseSlots(item, config.getString("slots"));
        }
        
        // Item référence
        if (config.contains("item")) {
            item.setItemId(config.getString("item"));
        }
        
        // Propriétés inline
        item.setInlineMaterial(config.getString("material"));
        item.setInlineData(config.getInt("data", 0));
        item.setInlineName(config.getString("name"));
        item.setInlineLore(config.getStringList("lore"));
        item.setInlineGlow(config.getBoolean("glow", false));
        
        // Actions
        if (config.contains("click_actions")) {
            item.setClickActions(config.getStringList("click_actions"));
        }
        
        return item;
    }

    /**
     * Parse les slots depuis une string
     */
    private void parseSlots(MenuItem item, String slotsStr) {
        if (slotsStr == null) return;
        
        for (String part : slotsStr.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int i = start; i <= end; i++) {
                        item.addSlot(i);
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    item.addSlot(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Parse un template d'item
     */
    private ItemTemplate parseItemTemplate(String id, ConfigurationSection config) {
        ItemTemplate template = new ItemTemplate(id);
        
        template.setMaterial(config.getString("material", "STONE"));
        template.setData((short) config.getInt("data", 0));
        template.setName(config.getString("name"));
        template.setLore(config.getStringList("lore"));
        template.setGlow(config.getBoolean("glow", false));
        template.setSkull(config.getString("skull"));
        template.setHdb(config.getString("hdb"));
        
        if (config.contains("click_actions")) {
            template.setClickActions(config.getStringList("click_actions"));
        }
        
        return template;
    }

    /**
     * Résout les héritages de templates
     */
    private void resolveInheritance() {
        for (MenuTemplate template : templates.values()) {
            if (template.getParentTemplate() != null) {
                MenuTemplate parent = templates.get(template.getParentTemplate());
                if (parent != null) {
                    inheritFrom(template, parent);
                } else {
                    plugin.getLogger().warning("Template " + template.getId() + 
                        " references unknown parent: " + template.getParentTemplate());
                }
            }
        }
    }

    /**
     * Hérite les propriétés d'un parent
     */
    private void inheritFrom(MenuTemplate child, MenuTemplate parent) {
        // Hériter les items du parent (le child peut override)
        for (Map.Entry<String, MenuItem> entry : parent.getItems().entrySet()) {
            if (!child.getItems().containsKey(entry.getKey())) {
                child.addItem(entry.getValue().clone());
            }
        }
        
        // Hériter le titre si non défini
        if (child.getTitle() == null || child.getTitle().equals("&8Menu Template")) {
            child.setTitle(parent.getTitle());
        }
        
        // Hériter la taille si par défaut
        if (child.getSize() == 54 && parent.getSize() != 54) {
            child.setSize(parent.getSize());
        }
        
        // Fusionner les actions (parent d'abord, puis child)
        List<String> openActions = new ArrayList<>(parent.getOpenActions());
        openActions.addAll(child.getOpenActions());
        child.setOpenActions(openActions);
        
        List<String> closeActions = new ArrayList<>(parent.getCloseActions());
        closeActions.addAll(child.getCloseActions());
        child.setCloseActions(closeActions);
    }

    /**
     * Applique un template à un MenuData
     */
    public void applyTemplate(MenuData menu, String templateId) {
        MenuTemplate template = templates.get(templateId);
        if (template == null) {
            plugin.getLogger().warning("Unknown template: " + templateId);
            return;
        }
        
        // Copier les items
        for (MenuItem item : template.getItems().values()) {
            menu.addItem(item.clone());
        }
        
        // Copier les actions
        if (!template.getOpenActions().isEmpty()) {
            List<String> menuOpenActions = new ArrayList<>(menu.getOpenActions());
            menuOpenActions.addAll(0, template.getOpenActions()); // Template actions first
            menu.setOpenActions(menuOpenActions);
        }
        
        if (!template.getCloseActions().isEmpty()) {
            List<String> menuCloseActions = new ArrayList<>(menu.getCloseActions());
            menuCloseActions.addAll(0, template.getCloseActions());
            menu.setCloseActions(menuCloseActions);
        }
    }

    /**
     * Obtient un template de menu
     */
    public MenuTemplate getTemplate(String id) {
        return templates.get(id);
    }

    /**
     * Obtient un template d'item
     */
    public ItemTemplate getItemTemplate(String id) {
        return itemTemplates.get(id);
    }

    /**
     * Vérifie si un template existe
     */
    public boolean hasTemplate(String id) {
        return templates.containsKey(id);
    }

    /**
     * Obtient tous les IDs de templates
     */
    public Set<String> getTemplateIds() {
        return templates.keySet();
    }

    /**
     * Crée un fichier template d'exemple
     */
    private void createExampleTemplate(File folder) {
        String content = 
            "# ==============================================\n" +
            "#     Kgui - Templates de Menus\n" +
            "# ==============================================\n" +
            "\n" +
            "# Templates de menus réutilisables\n" +
            "menu_templates:\n" +
            "  # Template de base avec bordure\n" +
            "  bordered_menu:\n" +
            "    size: 54\n" +
            "    items:\n" +
            "      border:\n" +
            "        slots: '0-8,45-53'\n" +
            "        item: 'border'\n" +
            "      close:\n" +
            "        slots: [49]\n" +
            "        item: 'close_button'\n" +
            "\n" +
            "  # Template paginé\n" +
            "  paginated_menu:\n" +
            "    inherit_from: bordered_menu\n" +
            "    items:\n" +
            "      prev_page:\n" +
            "        slots: [48]\n" +
            "        item: 'prev_page'\n" +
            "      next_page:\n" +
            "        slots: [50]\n" +
            "        item: 'next_page'\n" +
            "\n" +
            "  # Template confirmation\n" +
            "  confirm_menu:\n" +
            "    size: 27\n" +
            "    items:\n" +
            "      border:\n" +
            "        slots: '0-8,18-26'\n" +
            "        item: 'border'\n" +
            "      confirm:\n" +
            "        slots: [11]\n" +
            "        material: WOOL\n" +
            "        data: 5\n" +
            "        name: '&a&lConfirmer'\n" +
            "        lore:\n" +
            "          - '&7Cliquez pour confirmer'\n" +
            "      cancel:\n" +
            "        slots: [15]\n" +
            "        material: WOOL\n" +
            "        data: 14\n" +
            "        name: '&c&lAnnuler'\n" +
            "        lore:\n" +
            "          - '&7Cliquez pour annuler'\n" +
            "        click_actions:\n" +
            "          - '[close]'\n" +
            "\n" +
            "# Templates d'items réutilisables\n" +
            "item_templates:\n" +
            "  # Flèche retour\n" +
            "  back_arrow:\n" +
            "    material: ARROW\n" +
            "    name: '&e&lRetour'\n" +
            "    lore:\n" +
            "      - '&7Retourner au menu précédent'\n" +
            "    click_actions:\n" +
            "      - '[back]'\n" +
            "\n" +
            "  # Indicateur de chargement\n" +
            "  loading:\n" +
            "    material: BARRIER\n" +
            "    name: '&7Chargement...'\n" +
            "    lore:\n" +
            "      - '&8Veuillez patienter'\n";
        
        try {
            File exampleFile = new File(folder, "example.yml");
            java.nio.file.Files.write(exampleFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create example template: " + e.getMessage());
        }
    }

    /**
     * Recharge les templates
     */
    public void reload() {
        loadTemplates();
    }
}
