# Vore Mod (mod id: gulp) - Forge 1.20.1

Forge 1.20.1 (47.x) mod: swallow mobs, carry them in a stomach, digest them for loot.

## Build
Easiest: push this folder to a GitHub repo. The workflow in .github/workflows/build.yml builds it
(Actions tab > newest run > Artifacts > gulp-mod).
Locally: install JDK 17 + Gradle 8.1.1, run `gradle build`. The jar lands in build/libs/gulp-0.1.0.jar.
For testing in a dev environment: `gradle runClient`.

## Install
Install Minecraft Forge for 1.20.1, then put gulp-0.1.0.jar in the mods folder. (No Fabric API needed.)

## Controls (rebindable)
- V = swallow the mob you're looking at (within 4 blocks). Looking UP makes the cooldown shorter.
- B = toggle Soft / Hard mode
- G = release the most recent mob. Sneak + G = release everything.
- N = open the stomach screen (see contents, release or digest each mob, spend perk points, settings)

## Mechanics
- Capacity is a volume number. A mob's size is its hitbox width x width x height (chicken ~0.1, villager ~0.7, cow ~1.1,
  horse ~3, iron golem ~5, modded mobs anywhere from 1 to hundreds). A mob fits if it is no bigger than your capacity and
  everything inside adds up to no more than your capacity. You can hold up to 100 creatures at once.
- Capacity = (1 + 0.75 x level + 0.08 x level^2) x (1 + 0.06 x Roomy Stomach rank). Every level gives more room than the
  last: 1.8 at level 1, 16.5 at 10, 48 at 20, 239 at 50, 859 at 99, and up to 2406 with Roomy Stomach 30/30.
- Levels go to 99. XP per level: 100, 280, 520 ... 8940 at level 20, then +300 per level (about 32,000 at 98).
- Soft mode: mobs are held safely. A completely full stomach earns one level's worth of XP every 4 minutes, so the
  fuller you are the faster you level.
- Hard mode: mobs digest over time (13s for a cow, 22s for an iron golem, about 3 minutes for a size-500 monster).
  When the digestion bar is full they don't die instantly: for about 8 more seconds they take steady damage (their red
  health bar drains on the stomach screen), then they die and drop their normal loot. (DISSOLVE_SECONDS in StomachLogic.java.) Digesting gives 15 + 3 x the mob's max health in XP. Now and then (about 8% per mob per second)
  a mob struggles and hurts you: 2 HP plus 0.75 per 1.0 of mob volume, capped at 5 HP. Hard mode only; armor and Iron
  Stomach reduce it, and it never takes you below 2 hearts.
- Level ups give 1 perk point each (98 by level 99). Perks (N > Perks; click = 1 rank, Shift = 5, Ctrl = as many as you can):
  Roomy Stomach (30 ranks, +6% capacity each), Healing Stomach (15, soft mode: held mobs heal 1% of max HP per rank
  every 5s), Better Loot (10, +1 Looting level per 2 ranks on digested mobs, up to V), Quick Gulp (10, looking up:
  swallow cooldown -0.15s per rank, down to 0.5s), Iron Stomach (15, -5% struggle damage per rank), Gourmet (20, +5%
  XP from digesting per rank). 100 ranks in total, so a max level player has nearly everything. Add more in Perk.java.
- Names: each swallowed mob keeps its real name (including modded mobs and name-tagged mobs) and is shown in each
  player's own language. A modded mob with no translation gets a readable name made from its id (modid:big_mob -> Big Mob).
- Icons: the stomach screen shows each mob's spawn egg (vanilla and modded). A mob with no spawn egg gets no icon.
- Aiming: mobs have a slightly bigger hitbox for swallowing, and aiming near a mob (about 12 degrees) still counts.
- The Ender Dragon, the Wither and players can never be swallowed.
- Stomach contents are saved with the world. Dying releases everything.

## Visible belly
The belly of every player model swells forward as their stomach fills (using the player's own skin).
Other players see it too. Settings > Belly style switches between Classic (whole torso) and Stomach only.
Tweak MAX_EXTRA_DEPTH / MAX_EXTRA_WIDTH in BellyLayer.java for how far it sticks out, and
BELLY_TOP / BELLY_HEIGHT in BellyModel.java for the stomach-only shape.
Custom player model mods can read the fullness (0..1) from ClientState.bellyAmount(playerUuid).

## Sounds
The sounds are in src/main/resources/assets/gulp/sounds/ (.ogg) and wired up in sounds.json:
- Swallow: the 12 gulp*_*.ogg files (random each time) - Spit out: dropping1-3 (random) - Digestion (mob digests on its own): digestion1-3 (random)
- Digest button (stomach screen): digestion_button - Screen music (loops quietly, as background ambience, on the stomach,
  perks and settings screens): screen_loop. Its volume is VOLUME in ScreenLoopSound.java (0.0 - 1.0).
Settings > Sounds has a "Sounds: Custom / Minecraft" switch (Minecraft = the vanilla stand-in sounds), an ON/OFF
button and a 0% - 200% volume slider for each sound (100% is the middle; drag to hear a preview). Every player's
choice is their own (saved in config/gulp-client.properties).
Minecraft can't play a file louder than it was recorded, so each effect also has a "_loud" twin (+6 dB, exactly
double the amplitude) that the slider switches to above 100%. If you replace a sound, make its _loud twin too
(gain it by 6 dB through a soft limiter) and keep the same file name plus "_loud".
The Minecraft stand-in sounds can't go above 100%. The screen music is quieter by design: its 100% is BASE_VOLUME in
ScreenLoopSound.java.
To replace a sound, convert it to .ogg (mono for in-world sounds) and keep the same file name.

## Art hooks
- HUD is drawn with plain rectangles in GulpHud.java; perk icons are in PerksScreen.iconFor().

## Multiplayer
- Everyone (server and every player) needs the mod installed. It is Forge 1.20.1 and works on dedicated servers.
- Each player has their own stomach, saved with the world. Other players see your belly and hear your sounds.
- Settings (belly style, sounds, volumes, level-up messages) are per player and saved on that player's computer.
- Pets are protected: you can't swallow another player's tamed animal or horse.
- Dead and spectating players can't use the stomach; if you die, everything inside is let out.
- Server admins can edit serverconfig/gulp-server.toml (in the world folder) to: turn hard mode / digesting off
  (hardModeEnabled), scale struggle damage (struggleDamageMultiplier, 0 = never hurts), and scale XP (xpMultiplier).

## Mod info
Name: Vore Mod. Description: This Mod adds Vore to your world with progression, perks and capacity to carry mobs
wherever you want. The icon (src/main/resources/logo.png, also pack.png) is a pixel-art drumstick, the hunger icon.
Replace logo.png with your own 256x256 art any time.
