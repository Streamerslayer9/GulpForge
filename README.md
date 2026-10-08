# Gulp (prototype) - Forge 1.20.1

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
- Capacity is based on mob volume (width * width * height). Chicken ~0.1, villager ~0.7, cow ~1.1, horse ~3, iron golem ~5.
- Soft mode: mobs are held safely, and you earn slow XP for holding them.
- Hard mode: mobs digest over time and drop their normal loot; you earn more XP. They "struggle" and hurt you (armor helps).
- Level ups raise capacity and give 1 perk point each. Perks: Roomy Stomach (+capacity), Healing Stomach
  (soft mode heals held mobs), Rich Digestion (bonus loot chance). Add more in Perk.java.
- Stomach contents are saved with the world. Dying releases everything.

## Visible belly
The belly of every player model swells forward as their stomach fills (using the player's own skin).
Other players see it too. Settings > Belly style switches between Classic (whole torso) and Stomach only.
Tweak MAX_EXTRA_DEPTH / MAX_EXTRA_WIDTH in BellyLayer.java for how far it sticks out, and
BELLY_TOP / BELLY_HEIGHT in BellyModel.java for the stomach-only shape.
Custom player model mods can read the fullness (0..1) from ClientState.bellyAmount(playerUuid).

## Art/sound hooks
- HUD is drawn with plain rectangles in GulpHud.java; perk icons are in PerksScreen.iconFor().
- Drop .ogg files in src/main/resources/assets/gulp/sounds/ named swallow1-3, digest1-2, release1,
  then set CUSTOM_SFX = true in Gulp.java. (Vanilla sounds are used until then.)
