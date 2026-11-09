package com.rivalsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public final class RivalSMP extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey weaponKey;
    private final Map<UUID, Integer> hammerHits = new HashMap<>(), knifeHits = new HashMap<>(), scytheHits = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> arrowAmmo = new HashMap<>();
    private final Map<UUID, Integer> hammerLaunches = new HashMap<>();
    private final Map<UUID, Long> hammerReadyAt = new HashMap<>();
    private final Map<String, Long> ritualEnds = new HashMap<>();
    private final Set<String> ritualUnique = new HashSet<>();
    private Inventory autoBowGui;
    private final Map<UUID, Set<UUID>> allies = new HashMap<>();
    private final Map<UUID, Long> temporaryHealthTasks = new HashMap<>();
    private final Random random = new Random();

    private static final List<String> WEAPONS = List.of("blood-sword","dooms-hammer","scythe","killer-knife","auto-bow","crossbow","ban-hammer");

    @Override public void onEnable() {
        saveDefaultConfig();
        weaponKey = new NamespacedKey(this, "weapon_id");
        Objects.requireNonNull(getCommand("rivalsmp")).setExecutor(this);
        Objects.requireNonNull(getCommand("rivalsmp")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipes();
        loadUniqueWeapons();
        getLogger().info("RivalSMP enabled. Paper 1.21.11 target.");
    }

    @Override public void onDisable() {
        for (String id : WEAPONS) Bukkit.removeRecipe(new NamespacedKey(this, id));
        hammerHits.clear(); knifeHits.clear(); scytheHits.clear(); cooldowns.clear();
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private boolean admin(CommandSender s) { if (s.hasPermission("rivalsmp.admin")) return true; s.sendMessage(color("&cYou don't have permission.")); return false; }
    private String norm(String s) { return s.toLowerCase(Locale.ROOT).replace('_','-'); }

    private String nice(String id) {
        return switch(id) {
            case "blood-sword" -> "Blood Sword"; case "dooms-hammer" -> "DoomsHammer";
            case "scythe" -> "Scythe"; case "killer-knife" -> "KillerKnife";
            case "auto-bow" -> "AutoBow"; case "crossbow" -> "CrossBow"; case "ban-hammer" -> "BanHammer";
            default -> id;
        };
    }

    private Material baseMaterial(String id) {
        return switch(id) {
            case "blood-sword" -> Material.NETHERITE_SWORD;
            case "dooms-hammer" -> Material.MACE;
            case "scythe" -> Material.NETHERITE_SWORD;
            case "killer-knife" -> Material.NETHERITE_SWORD;
            case "auto-bow" -> Material.BOW;
            case "crossbow" -> Material.CROSSBOW;
            case "ban-hammer" -> Material.MACE;
            default -> null;
        };
    }

    private ItemStack weapon(String id) {
        Material type = baseMaterial(id);
        if (type == null) return null;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(switch(id) {
            case "blood-sword" -> "&c&lBlood Sword"; case "dooms-hammer" -> "&5&lDoomsHammer";
            case "scythe" -> "&d&lScythe"; case "killer-knife" -> "&4&lKillerKnife";
            case "auto-bow" -> "&6&lAutoBow"; case "crossbow" -> "&b&lCrossBow"; default -> "&e&lBanHammer";
        }));
        meta.setLore(List.of(color("&7RivalSMP Legendary Weapon"), color("&8ID: "+id),
                color("&eF: Activate ultimate / special ability")));
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, id);
        meta.setUnbreakable(true);
        int modelId = switch (id) {
            case "blood-sword" -> 10001;
            case "dooms-hammer" -> 10002;
            case "scythe" -> 10003;
            case "killer-knife" -> 10004;
            case "auto-bow" -> 10005;
            case "crossbow" -> 10006;
            case "ban-hammer" -> 10007;
            default -> 0;
        };
        meta.setCustomModelData(modelId);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        switch(id) {
            case "blood-sword" -> { meta.addEnchant(Enchantment.SHARPNESS, 6, true); meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true); meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true); }
            case "dooms-hammer" -> { meta.addEnchant(Enchantment.WIND_BURST, 3, true); meta.addEnchant(Enchantment.DENSITY, 5, true); }
            case "scythe" -> { meta.addEnchant(Enchantment.SHARPNESS, 5, true); meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true); meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true); }
            case "killer-knife" -> { meta.addEnchant(Enchantment.SHARPNESS, 5, true); meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true); }
            case "auto-bow" -> { meta.addEnchant(Enchantment.POWER, 5, true); meta.addEnchant(Enchantment.PUNCH, 2, true); meta.addEnchant(Enchantment.FLAME, 1, true); }
            case "crossbow" -> { meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true); meta.addEnchant(Enchantment.MULTISHOT, 1, true); }
            case "ban-hammer" -> meta.addEnchant(Enchantment.DENSITY, 5, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String weaponId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
    }

    private void registerRecipes() {
        for (String id : WEAPONS) {
            NamespacedKey key = new NamespacedKey(this, id);
            Bukkit.removeRecipe(key);
            ShapedRecipe r = new ShapedRecipe(key, weapon(id));
            r.shape("ABA","CDC"," E ");
            switch(id) {
                case "blood-sword" -> { r.setIngredient('A', Material.NETHER_STAR); r.setIngredient('B', Material.REDSTONE_BLOCK); r.setIngredient('C', Material.REDSTONE_BLOCK); r.setIngredient('D', Material.NETHERITE_SWORD); r.setIngredient('E', Material.NETHERITE_INGOT); }
                case "dooms-hammer" -> { r.setIngredient('A', Material.NETHER_STAR); r.setIngredient('B', Material.OBSIDIAN); r.setIngredient('C', Material.OBSIDIAN); r.setIngredient('D', Material.MACE); r.setIngredient('E', Material.NETHERITE_BLOCK); }
                case "scythe" -> { r.setIngredient('A', Material.ECHO_SHARD); r.setIngredient('B', Material.AMETHYST_BLOCK); r.setIngredient('C', Material.AMETHYST_BLOCK); r.setIngredient('D', Material.NETHERITE_SWORD); r.setIngredient('E', Material.NETHER_STAR); }
                case "killer-knife" -> { r.setIngredient('A', Material.WITHER_SKELETON_SKULL); r.setIngredient('B', Material.IRON_BLOCK); r.setIngredient('C', Material.IRON_BLOCK); r.setIngredient('D', Material.NETHERITE_SWORD); r.setIngredient('E', Material.NETHER_STAR); }
                case "auto-bow" -> { r.setIngredient('A', Material.DISPENSER); r.setIngredient('B', Material.REDSTONE_BLOCK); r.setIngredient('C', Material.REDSTONE_BLOCK); r.setIngredient('D', Material.BOW); r.setIngredient('E', Material.NETHER_STAR); }
                case "crossbow" -> { r.setIngredient('A', Material.SCULK_CATALYST); r.setIngredient('B', Material.AMETHYST_BLOCK); r.setIngredient('C', Material.AMETHYST_BLOCK); r.setIngredient('D', Material.CROSSBOW); r.setIngredient('E', Material.NETHER_STAR); }
                case "ban-hammer" -> { r.setIngredient('A', Material.NETHER_STAR); r.setIngredient('B', Material.NETHERITE_BLOCK); r.setIngredient('C', Material.NETHERITE_BLOCK); r.setIngredient('D', Material.MACE); r.setIngredient('E', Material.WITHER_SKELETON_SKULL); }
            }
            Bukkit.addRecipe(r);
        }
    }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("help")) {
            s.sendMessage(color("&8&m---------&c RivalSMP &8&m---------"));
            for (String line : List.of("/rivalsmp list","/rivalsmp recipes","/rivalsmp recipe <weapon>","/rivalsmp give <player> <weapon>","/rivalsmp announce <message>","/rivalsmp allies <add|remove|list> [player]","/rivalsmp ammo <amount>","/rivalsmp reload")) s.sendMessage(color("&e"+line));
            s.sendMessage(color("&7Hold a legendary weapon and press F to use its special ability."));
            return true;
        }
        switch(a[0].toLowerCase(Locale.ROOT)) {
            case "list" -> { s.sendMessage(color("&cWeapons: &f"+String.join(", ", WEAPONS))); return true; }
            case "recipes" -> {
                s.sendMessage(color("&6&lRivalSMP Crafting Recipes"));
                for (String id : WEAPONS) s.sendMessage(color("&e"+nice(id)+" &7- /rivalsmp recipe "+id));
                return true;
            }
            case "recipe" -> {
                if (a.length < 2) { s.sendMessage(color("&cUsage: /rivalsmp recipe <weapon>")); return true; }
                String id=norm(a[1]); if (!WEAPONS.contains(id)) { s.sendMessage(color("&cUnknown weapon.")); return true; }
                s.sendMessage(color("&6Recipe: &e"+nice(id)));
                List<String> lines = switch(id) {
                    case "blood-sword" -> List.of("Nether Star | Redstone Block | Nether Star","Redstone Block | Netherite Sword | Redstone Block","Empty | Netherite Ingot | Empty");
                    case "dooms-hammer" -> List.of("Nether Star | Obsidian | Nether Star","Obsidian | Mace | Obsidian","Empty | Netherite Block | Empty");
                    case "scythe" -> List.of("Echo Shard | Amethyst Block | Echo Shard","Amethyst Block | Netherite Sword | Amethyst Block","Empty | Nether Star | Empty");
                    case "killer-knife" -> List.of("Wither Skeleton Skull | Iron Block | Wither Skeleton Skull","Iron Block | Netherite Sword | Iron Block","Empty | Nether Star | Empty");
                    case "auto-bow" -> List.of("Dispenser | Redstone Block | Dispenser","Redstone Block | Bow | Redstone Block","Empty | Nether Star | Empty");
                    case "crossbow" -> List.of("Sculk Catalyst | Amethyst Block | Sculk Catalyst","Amethyst Block | Crossbow | Amethyst Block","Empty | Nether Star | Empty");
                    default -> List.of("Nether Star | Netherite Block | Nether Star","Netherite Block | Mace | Netherite Block","Empty | Wither Skeleton Skull | Empty");
                };
                lines.forEach(line -> s.sendMessage(color("&7"+line)));
                s.sendMessage(color("&8Crafting uses the 3x3 pattern shown above."));
                return true;
            }
            case "give" -> {
                if (!admin(s)) return true;
                if (a.length<3) { s.sendMessage(color("&c/rivalsmp give <player> <weapon>")); return true; }
                Player p=Bukkit.getPlayerExact(a[1]); String id=norm(a[2]); ItemStack item=weapon(id);
                if (p==null || item==null) { s.sendMessage(color("&cPlayer not online or weapon unknown.")); return true; }
                if(ritualUnique.contains(id)||ritualEnds.containsKey(id)){s.sendMessage(color("&cThat unique weapon already exists or is being crafted."));return true;}
                saveUnique(id); p.getInventory().addItem(item).values().forEach(it->p.getWorld().dropItemNaturally(p.getLocation(),it)); s.sendMessage(color("&aCreated and given the unique "+nice(id)+" to "+p.getName())); return true;
            }
            case "announce" -> {
                if (!admin(s)) return true;
                if (a.length<2) { s.sendMessage(color("&c/rivalsmp announce <message>")); return true; }
                announce(String.join(" ",Arrays.copyOfRange(a,1,a.length))); s.sendMessage(color("&aAnnouncement sent.")); return true;
            }
            case "reload" -> {
                if (!admin(s)) return true;
                reloadConfig(); registerRecipes(); s.sendMessage(color("&aRivalSMP reloaded.")); return true;
            }
            case "ammo" -> {
                if (!(s instanceof Player p) || !admin(s)) return true;
                int amount= a.length>1 ? Math.max(1,Math.min(64,Integer.parseInt(a[1]))) : 16;
                arrowAmmo.put(p.getUniqueId(),amount); p.sendMessage(color("&aAutoBow ammo set to "+amount)); return true;
            }
            case "allies" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                Set<UUID> set=allies.computeIfAbsent(p.getUniqueId(),k->new HashSet<>());
                if (a.length==1 || a[1].equalsIgnoreCase("list")) {
                    s.sendMessage(color("&dAllies: &f"+set.stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(Objects::nonNull).toList())); return true;
                }
                if (a.length<3) { s.sendMessage(color("&c/rivalsmp allies <add|remove> <player>")); return true; }
                Player target=Bukkit.getPlayerExact(a[2]); if(target==null){s.sendMessage(color("&cThat player must be online."));return true;}
                if(a[1].equalsIgnoreCase("add")){set.add(target.getUniqueId());p.sendMessage(color("&aAdded ally "+target.getName()));}
                else if(a[1].equalsIgnoreCase("remove")){set.remove(target.getUniqueId());p.sendMessage(color("&cRemoved ally "+target.getName()));}
                return true;
            }
            default -> { s.sendMessage(color("&cUnknown command. Use /rivalsmp help.")); return true; }
        }
    }

    private boolean allied(Player a, Player b) { return allies.getOrDefault(a.getUniqueId(),Set.of()).contains(b.getUniqueId()); }
    private boolean cooldown(Player p,String ability,int seconds) {
        String key=p.getUniqueId()+":"+ability; long now=System.currentTimeMillis(), until=cooldowns.getOrDefault(UUID.nameUUIDFromBytes(key.getBytes()),0L);
        if(until>now){p.sendMessage(color("&c"+ability+" cooldown: "+((until-now+999)/1000)+"s"));return false;}
        cooldowns.put(UUID.nameUUIDFromBytes(key.getBytes()),now+seconds*1000L); return true;
    }
    private void announce(String msg) {
        Component chat=Component.text("✦ RIVAL SMP ✦ ",TextColor.color(170,70,255)).append(gradient(msg,0));
        Bukkit.broadcast(chat);
        final int[] frame={0};
        BukkitTask task=Bukkit.getScheduler().runTaskTimer(this,()->{
            Title title=Title.title(gradient("✦ RIVAL SMP ✦",frame[0]),gradient(msg,frame[0]+4),
                    Title.Times.times(Duration.ZERO,Duration.ofMillis(650),Duration.ofMillis(150)));
            Bukkit.getOnlinePlayers().forEach(p->p.showTitle(title)); frame[0]++;
        },0L,4L);
        Bukkit.getScheduler().runTaskLater(this,task::cancel,72L);
    }
    private Component gradient(String text,int offset) {
        Component out=Component.empty(); int n=Math.max(1,text.length());
        for(int i=0;i<text.length();i++){
            double t=((double)i/n+(offset%18)/18.0)%1.0;
            int r=(int)(170+(55-170)*t),g=(int)(70+(220-70)*t),b=(int)(255+(255-255)*t);
            out=out.append(Component.text(String.valueOf(text.charAt(i)),TextColor.color(r,g,b)));
        } return out;
    }

    private void loadUniqueWeapons(){ for(String id:WEAPONS) if(getConfig().getBoolean("unique.created."+id,false)) ritualUnique.add(id); }
    private void saveUnique(String id){ ritualUnique.add(id); getConfig().set("unique.created."+id,true); saveConfig(); }

    @EventHandler public void onCraft(org.bukkit.event.inventory.CraftItemEvent e){
        String id=weaponId(e.getRecipe().getResult()); if(id==null)return;
        e.setCancelled(true);
        if(ritualUnique.contains(id) || ritualEnds.containsKey(id)){ if(e.getWhoClicked() instanceof Player p)p.sendMessage(color("&cThis unique weapon already exists or is being crafted.")); return; }
        if(!(e.getWhoClicked() instanceof Player p))return;
        ItemStack[] matrix=e.getInventory().getMatrix();
        for(int i=0;i<matrix.length;i++) if(matrix[i]!=null && !matrix[i].getType().isAir()){ matrix[i].setAmount(matrix[i].getAmount()-1); if(matrix[i].getAmount()<=0)matrix[i]=null; }
        e.getInventory().setMatrix(matrix);
        int seconds=Math.max(1,getConfig().getInt("rituals."+id+".duration-seconds",300));
        long end=System.currentTimeMillis()+seconds*1000L; ritualEnds.put(id,end);
        p.sendMessage(color("&5&lRITUAL STARTED &d"+nice(id)+" &7- &f"+seconds+" seconds"));
        BukkitTask task=Bukkit.getScheduler().runTaskTimer(this,()->{
            long left=Math.max(0,(end-System.currentTimeMillis()+999)/1000);
            for(Player online:Bukkit.getOnlinePlayers()) online.sendActionBar(Component.text(nice(id)+" ritual: "+left+"s",TextColor.color(190,80,255)));
            p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation().add(0,1,0),18,.5,.7,.5,.05);
            p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_AMBIENT,.5f,1.5f);
        },0L,20L);
        Bukkit.getScheduler().runTaskLater(this,()->{task.cancel();ritualEnds.remove(id);saveUnique(id);ItemStack reward=weapon(id); if(p.isOnline()){Map<Integer,ItemStack> rest=p.getInventory().addItem(reward);rest.values().forEach(it->p.getWorld().dropItemNaturally(p.getLocation(),it));p.sendMessage(color("&aRitual complete! You obtained "+nice(id)+"."));}else{getConfig().set("unique.pending."+id,p.getUniqueId().toString());saveConfig();}},seconds*20L);
    }

    @EventHandler public void protectDrop(org.bukkit.event.entity.ItemDespawnEvent e){ if(weaponId(e.getEntity().getItemStack())!=null)e.setCancelled(true); }
    @EventHandler public void protectDamage(org.bukkit.event.entity.EntityDamageEvent e){ if(e.getEntity() instanceof Item item && weaponId(item.getItemStack())!=null)e.setCancelled(true); }
    @EventHandler public void protectVoid(org.bukkit.event.entity.EntityDamageEvent e){ if(e.getEntity() instanceof Item item && weaponId(item.getItemStack())!=null && e.getCause()==org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID){e.setCancelled(true);item.teleport(item.getWorld().getSpawnLocation());} }

    private Inventory createAmmoGui(Player p){ Inventory inv=Bukkit.createInventory(p,27,Component.text("AutoBow Ammo Refill")); ItemStack info=new ItemStack(Material.ARROW); ItemMeta m=info.getItemMeta();m.setDisplayName(color("&eStored ammo: &f"+arrowAmmo.getOrDefault(p.getUniqueId(),0)));info.setItemMeta(m);inv.setItem(13,info);return inv; }
    @EventHandler public void autoBowOpen(PlayerInteractEvent e){ if(e.getHand()!=EquipmentSlot.HAND || !e.getPlayer().isSneaking())return; if(e.getAction()!=org.bukkit.event.block.Action.LEFT_CLICK_AIR && e.getAction()!=org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)return; if(!"auto-bow".equals(weaponId(e.getItem())))return; e.setCancelled(true); e.getPlayer().openInventory(createAmmoGui(e.getPlayer())); }
    @EventHandler public void ammoGuiClick(org.bukkit.event.inventory.InventoryClickEvent e){ if(!e.getView().title().equals(Component.text("AutoBow Ammo Refill")))return; if(!(e.getWhoClicked() instanceof Player p))return; if(e.getClickedInventory()==e.getView().getTopInventory()){e.setCancelled(true);return;} if(e.getClickedInventory()==e.getView().getBottomInventory() && e.getCurrentItem()!=null && e.getCurrentItem().getType()==Material.ARROW){e.setCancelled(true);int amount=e.isShiftClick()?e.getCurrentItem().getAmount():1;arrowAmmo.merge(p.getUniqueId(),amount,Integer::sum);e.getCurrentItem().setAmount(e.getCurrentItem().getAmount()-amount);p.openInventory(createAmmoGui(p));} }

    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p=e.getPlayer(); String id=weaponId(p.getInventory().getItemInMainHand());
        if(id==null)return; e.setCancelled(true);
        switch(id) {
            case "blood-sword" -> heartbreaker(p);
            case "dooms-hammer" -> hammerDash(p);
            case "scythe" -> catastrophe(p);
            case "killer-knife" -> twinExecution(p);
            case "auto-bow" -> startAutoBow(p);
            case "crossbow" -> sonicMultishot(p);
            case "ban-hammer" -> p.sendMessage(color("&eBanHammer: kills apply the configured ban."));
        }
    }

    private void heartbreaker(Player p) {
        if(!cooldown(p,"Heartbreaker",getConfig().getInt("weapons.blood-sword.heartbreaker.cooldown-seconds",600)))return;
        Player target=target(p,12); if(target==null||allied(p,target)){p.sendMessage(color("&cNo valid enemy in range."));return;}
        AttributeInstance attr=target.getAttribute(Attribute.MAX_HEALTH); if(attr==null)return;
        double reduction=getConfig().getDouble("weapons.blood-sword.heartbreaker.max-health-reduction-hearts",3)*2;
        double old=attr.getBaseValue(); attr.setBaseValue(Math.max(2,old-reduction));
        p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+reduction));
        int duration=getConfig().getInt("weapons.blood-sword.heartbreaker.duration-seconds",20);
        Bukkit.getScheduler().runTaskLater(this,()->{if(target.isOnline()){AttributeInstance a=target.getAttribute(Attribute.MAX_HEALTH);if(a!=null)a.setBaseValue(Math.max(a.getBaseValue(),old));target.setHealth(Math.min(target.getHealth(),a.getValue()));}},duration*20L);
        p.sendMessage(color("&cHeartbreaker activated on "+target.getName()));
    }
    private void hammerDash(Player p) {
        long ready=hammerReadyAt.getOrDefault(p.getUniqueId(),0L);
        if(System.currentTimeMillis()<ready){p.sendMessage(color("&cMace launch cooldown: "+((ready-System.currentTimeMillis()+999)/1000)+"s"));return;}
        int used=hammerLaunches.merge(p.getUniqueId(),1,Integer::sum);
        if(used>=2){hammerLaunches.put(p.getUniqueId(),0);hammerReadyAt.put(p.getUniqueId(),System.currentTimeMillis()+20_000L);p.sendMessage(color("&eTwo launches used. 20-second cooldown."));}
        p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(0.65));
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_MACE_SMASH_AIR,1,1);
        p.sendMessage(color("&5DoomsHammer dash!"));
    }
    private void catastrophe(Player p) {
        if(!cooldown(p,"Catastrophe",getConfig().getInt("weapons.scythe.catastrophe-cooldown-seconds",180)))return;
        LivingEntity t=targetLiving(p,18); if(t==null || (t instanceof Player pl && allied(p,pl)))return;
        t.damage(getConfig().getDouble("weapons.scythe.catastrophe-damage",12),p);
        t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,2));
        t.setVelocity(t.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.7).setY(0.25));
        t.getWorld().strikeLightningEffect(t.getLocation()); p.sendMessage(color("&dCatastrophe unleashed!"));
    }
    private void twinExecution(Player p) {
        if(!cooldown(p,"Twin Execution",getConfig().getInt("weapons.killer-knife.twin-execution-cooldown-seconds",90)))return;
        Player t=target(p,18); if(t==null||allied(p,t))return;
        for(int i=0;i<2;i++){
            Snowball knife=p.launchProjectile(Snowball.class);
            knife.setVelocity(p.getLocation().getDirection().multiply(1.8).rotateAroundY((i==0?-.08:.08)));
            knife.setCustomName("RIVAL_KNIFE:"+p.getUniqueId()+":"+getConfig().getDouble("weapons.killer-knife.twin-execution-damage",6));
            knife.setCustomNameVisible(false);
        }
        p.sendMessage(color("&4Twin Execution thrown!"));
    }
    private void startAutoBow(Player p) {
        if(!cooldown(p,"AutoBow",getConfig().getInt("weapons.auto-bow.cooldown-seconds",240)))return;
        int ammo=arrowAmmo.getOrDefault(p.getUniqueId(),0); if(ammo<=0){p.sendMessage(color("&cNo AutoBow ammo. Admin: /rivalsmp ammo <amount>"));return;}
        int duration=getConfig().getInt("weapons.auto-bow.burst-duration-seconds",10);
        final int[] ticks={0}; final BukkitTask[] holder=new BukkitTask[1];
        holder[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(!p.isOnline()||ticks[0]++>=duration*2||arrowAmmo.getOrDefault(p.getUniqueId(),0)<=0){holder[0].cancel();return;}
            arrowAmmo.put(p.getUniqueId(),arrowAmmo.get(p.getUniqueId())-1);
            Arrow arrow=p.launchProjectile(Arrow.class); arrow.setVelocity(p.getLocation().getDirection().multiply(2.4));
        },0L,10L);
    }
    private void sonicMultishot(Player p) {
        if(!cooldown(p,"Sonic Multishot",getConfig().getInt("weapons.crossbow.cooldown-seconds",300)))return;
        Vector dir=p.getLocation().getDirection();
        for(int i=-1;i<=1;i++){
            Vector ray=dir.clone().rotateAroundY(i*0.18);
            for(double d=1;d<=18;d+=0.5){
                Location at=p.getEyeLocation().add(ray.clone().multiply(d));
                at.getWorld().spawnParticle(Particle.SONIC_BOOM,at,1,0,0,0,0);
                for(Entity ent:at.getWorld().getNearbyEntities(at,0.65,0.65,0.65)){
                    if(ent instanceof Player victim && victim!=p && !allied(p,victim)){
                        victim.damage(getConfig().getDouble("weapons.crossbow.sonic-damage",10),p); d=99; break;
                    }
                }
            }
        }
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,1,1);
    }
    private LivingEntity targetLiving(Player p,double range) {
        for(Entity e:p.getNearbyEntities(range,range,range)) if(e instanceof LivingEntity t && t!=p && p.hasLineOfSight(t)) return t;
        return null;
    }
    private Player target(Player p,double range) {
        for(Entity e:p.getNearbyEntities(range,range,range)) if(e instanceof Player t && t!=p && p.hasLineOfSight(t)) return t;
        return null;
    }

    @EventHandler public void onHit(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Player p))return;
        LivingEntity victim = e.getEntity() instanceof LivingEntity le ? le : null;
        if(victim==null || victim==p)return;
        Player v = victim instanceof Player pl ? pl : null;
        if(v!=null && allied(p,v)){e.setCancelled(true);return;}
        String id=weaponId(p.getInventory().getItemInMainHand()); if(id==null)return;
        switch(id){
            case "blood-sword" -> {
                double heal=getConfig().getDouble("weapons.blood-sword.soul-feast-hearts",1)*2;
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+heal));
                int strength=getConfig().getInt("weapons.blood-sword.battle-instinct-strength",1);
                if(strength>0)p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,60,strength-1,true,false));
            }
            case "dooms-hammer" -> {
                int count=hammerHits.merge(p.getUniqueId(),1,Integer::sum);
                int needed=getConfig().getInt("weapons.dooms-hammer.hits-required",10);
                if(count>=needed){hammerHits.put(p.getUniqueId(),0);int strikes=3+random.nextInt(2);
                    for(int i=0;i<strikes;i++){Location l=victim.getLocation().clone().add(random.nextDouble()*6-3,0,random.nextDouble()*6-3);victim.getWorld().strikeLightningEffect(l);}
                    e.setDamage(e.getDamage()+getConfig().getDouble("weapons.dooms-hammer.thunderfall-damage",8));
                    p.sendMessage(color("&5&lTHUNDERFALL!"));}
            }
            case "scythe" -> {
                if(p.getFallDistance()>0.0f && !p.isOnGround())victim.getWorld().strikeLightningEffect(victim.getLocation());
                int n=scytheHits.merge(p.getUniqueId(),1,Integer::sum);
                if(n%getConfig().getInt("weapons.scythe.rhythm-hits",3)==0){
                    int sec=getConfig().getInt("weapons.scythe.rhythm-duration-seconds",4);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,sec*20,1));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,sec*20,1));
                }
            }
            case "killer-knife" -> {
                int n=knifeHits.merge(p.getUniqueId(),1,Integer::sum);
                if(n%getConfig().getInt("weapons.killer-knife.combo-hits",3)==0)e.setDamage(e.getDamage()+getConfig().getDouble("weapons.killer-knife.combo-bonus-damage",3));
                if(v==null) break;
                Vector facing=v.getLocation().getDirection().setY(0).normalize();
                Vector toward=p.getLocation().toVector().subtract(v.getLocation().toVector()).setY(0).normalize();
                if(facing.dot(toward)<-0.5)e.setDamage(e.getDamage()+getConfig().getDouble("weapons.killer-knife.backstab-bonus-damage",4));
            }
        }
    }

    @EventHandler public void onProjectileHit(ProjectileHitEvent e) {
        if(!(e.getEntity() instanceof Snowball snow)||snow.getCustomName()==null||!snow.getCustomName().startsWith("RIVAL_KNIFE:"))return;
        String[] parts=snow.getCustomName().split(":");
        if(e.getHitEntity() instanceof Player target){
            Player shooter=Bukkit.getPlayer(UUID.fromString(parts[1]));
            if(shooter!=null&&!allied(shooter,target))target.damage(Double.parseDouble(parts[2]),shooter);
        }
    }

    @EventHandler public void onInteract(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND)return;
        String id=weaponId(e.getItem()); if(id==null)return;
        if(id.equals("auto-bow")&&e.getPlayer().isSneaking()&&(e.getAction()==org.bukkit.event.block.Action.RIGHT_CLICK_AIR||e.getAction()==org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)){
            Player p=e.getPlayer(); int ammo=arrowAmmo.getOrDefault(p.getUniqueId(),0);
            if(ammo>0){arrowAmmo.put(p.getUniqueId(),ammo-1);Arrow ar=p.launchProjectile(Arrow.class);ar.setVelocity(p.getLocation().getDirection().multiply(2.2));}
            else p.sendMessage(color("&cNo AutoBow ammo."));
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent e){
        Player p=e.getPlayer();
        for(String id:WEAPONS){String owner=getConfig().getString("unique.pending."+id);if(p.getUniqueId().toString().equals(owner)){p.getInventory().addItem(weapon(id));getConfig().set("unique.pending."+id,null);saveConfig();}}
    }

    @EventHandler public void onDeath(PlayerDeathEvent e) {
        if(!getConfig().getBoolean("lifesteal.enabled",true))return;
        Player dead=e.getEntity(), killer=dead.getKiller();
        if(killer==null)return;
        AttributeInstance k=killer.getAttribute(Attribute.MAX_HEALTH), d=dead.getAttribute(Attribute.MAX_HEALTH);
        if(k!=null)k.setBaseValue(Math.min(getConfig().getDouble("lifesteal.maximum-hearts",20)*2,k.getBaseValue()+getConfig().getDouble("lifesteal.hearts-per-kill",1)*2));
        if(d!=null)d.setBaseValue(Math.max(getConfig().getDouble("lifesteal.minimum-hearts",1)*2,d.getBaseValue()-getConfig().getDouble("lifesteal.hearts-lost-on-death",1)*2));
        if(weaponId(killer.getInventory().getItemInMainHand())!=null && weaponId(killer.getInventory().getItemInMainHand()).equals("ban-hammer")
                && !dead.hasPermission(getConfig().getString("weapons.ban-hammer.protect-permission","rivalsmp.banhammer.protected"))){
            String reason="BanHammer: defeated by "+killer.getName();
            Bukkit.getScheduler().runTask(this,()->Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"ban "+dead.getName()+" "+reason));
            AttributeInstance max=killer.getAttribute(Attribute.MAX_HEALTH); if(max!=null)max.setBaseValue(Math.min(40,max.getBaseValue()+getConfig().getDouble("weapons.ban-hammer.kill-hearts",2)*2));
        }
    }

    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(args.length==1)return List.of("help","list","recipes","recipe","give","announce","reload","allies","ammo");
        if(args.length==2&&args[0].equalsIgnoreCase("give"))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if(args.length==3&&args[0].equalsIgnoreCase("give"))return WEAPONS;
        if(args.length==2&&args[0].equalsIgnoreCase("recipe"))return WEAPONS;
        if(args.length==2&&args[0].equalsIgnoreCase("allies"))return List.of("add","remove","list");
        return Collections.emptyList();
    }
}
