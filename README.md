# RivalSMP — GitHub Actions source

Paper plugin source targeting Java 21 and Paper API 1.21.11.

## Build without installing Maven
1. Create/open a GitHub repository.
2. Upload **all extracted contents** of this ZIP to the repository root. Include the hidden `.github` folder.
3. Commit/push. Open **Actions → Build RivalSMP**.
4. When the workflow succeeds, open its run and download the `RivalSMP-JAR` artifact. Put the JAR in your server's `plugins` folder and restart.

## Current included changes
- Correct base materials: Scythe = Netherite Sword; BanHammer = Mace.
- Weapon items are unbreakable and receive their configured enchantments.
- Crafting a custom weapon starts a server-wide timed ritual; one creation per weapon ID is persisted in config.
- Dropped custom weapons do not despawn and are protected from entity damage; void damage returns the item to world spawn. Players may still drop them.
- DoomsHammer launch counter: two launches, then 20-second cooldown.
- DoomsHammer hit counter and Scythe hit effects accept living mobs as well as players; ally players are excluded.
- AutoBow F activation fires stored arrows automatically. Sneak + left-click with AutoBow opens the ammo GUI; place/shift-click arrows from your inventory to load stored ammo.

## Important limitations to verify before production
This is a development build, not a guarantee of production-tested behavior. The GitHub Actions compiler is the build verification. The crafting ritual is a timed crafting-result interception (ingredients are consumed when the ritual starts); it is not a physical altar animation. Unique tracking is per weapon type, and `/rivalsmp give` is an administrative bypass intended for testing/recovery. The current implementation does not provide a full recipe editor, full NBT preservation, or a complete resource pack. Test on a backup server first.

## Commands
- `/rivalsmp help`
- `/rivalsmp list`
- `/rivalsmp recipes`
- `/rivalsmp recipe <weapon>`
- `/rivalsmp give <player> <weapon>` (admin)
- `/rivalsmp allies <add|remove|list> [player]`
- `/rivalsmp reload` (admin)

## Ritual timers
Edit `plugins/RivalSMP/config.yml` under `rituals.<weapon-id>.duration-seconds`, then run `/rivalsmp reload`.


## Latest enchantment updates
- Blood Sword: Sharpness VI, Sweeping Edge III, Fire Aspect II.
- AutoBow: Power V, Punch II, Flame I (no Infinity; it uses stored ammo).
- CrossBow: Quick Charge III, Multishot.
- All weapons are Unbreakable.

A separate matching resource-pack ZIP is supplied. Install it on the client/server resource-pack distribution and test with the exact Minecraft client version.
