# Cosmetic Loader (Fabric, Minecraft 26.3)

Client-side mod. Press **K** (or use the **Cosmetics...** button in the escape menu) to open the menu.
Press **K** again, **Done**, or Esc to close it. Press F5 to see yourself.
The "Add:" button at the top of the menu switches between what you can add.

- **Models**: drop `.obj` files in `.minecraft/config/cosmetic_loader/models/`
  (the "Open models folder" button opens it), pick one, press Equip. Texture coordinates in the .obj are used
  when you give the model a texture (see below); `.mtl` material files are ignored.
- **Shapes**: built-in cube, square, triangle, pyramid and sphere.
- **Blocks**: pick a block preset or type any block id (e.g. `diamond_block`).
- **Effects**: pick a preset (angry_villager, heart, flame, ...) or type a particle id.
  Only particles without extra options work (no dust or block-crack particles).
- **Textures**: a flat square showing a texture: `item/diamond`, `block/stone`, a resource pack path like
  `mypack:textures/hat.png`, or one of your own pngs.
- **Capes**: pick a png (standard 64x32 cape layout, HD capes work too) or type a resource pack texture id.
- **Elytra**: leave everything empty for the vanilla elytra, or pick a png / type a texture id
  (standard elytra texture layout).

**Texturing models and shapes:** select the item on the right side of the menu. The "Texture" button cycles
through none, your own pngs and preset textures; the box under it takes any texture id (`block/stone`,
`item/diamond`, `mypack:textures/skin.png`) and the Set button applies it. Without a texture a model is a solid color
(use the Color button to tint it).

**Editing what you've equipped:** pick it with the "Editing" button, then use the sliders: move left/right, up/down
and forward, size, tilt forward/back, turn, tilt left/right, and opacity (see-through). Opacity works on every item
except particle effects. A see-through block is drawn as a cube with the block's own texture on every face, so it
works for simple blocks (stone, glass, planks, gold block...) but not for blocks that have no single flat texture;
those stay solid.

Your own pngs go in `config/cosmetic_loader/textures/` (the "Folder" button opens it).
Models, shapes, blocks, textures and effects can be attached to the head, back, chest, either hand,
either leg or either foot, and moved, turned, and resized. Capes and elytra always sit on the back.
Only you see your cosmetics (the mod runs on your game only, so other players don't see them). Your loadout is saved in `config/cosmetic_loader/loadout.json`.

## Build
This folder is the **source code**, not a finished mod. You need to compile it once to get the mod jar.

**Easiest: GitHub (nothing to install)**
1. Create a new GitHub repository and upload everything in this folder (keep `.github/` and `src/`).
2. Open the repo's **Actions** tab. The "Build mod jar" workflow runs on its own (or press "Run workflow").
3. When it finishes (a couple of minutes), open the repo's **Releases** page and download the `.jar`.
   (Or open the run and download the **cosmetic-loader-jar** artifact.) Never use a jar ending in `-sources` or `-dev`.

**Locally:** needs JDK 25 and Gradle 9.7+ installed:

    gradle build

The jar lands in `build/libs/` (the one without `-sources`).

## Install
1. Install Minecraft 26.3 with Fabric Loader 0.19.5 or newer (https://fabricmc.net/use/).
2. Put the built jar **and** Fabric API 0.166.x for 26.3 (https://modrinth.com/mod/fabric-api) in `.minecraft/mods/`.
3. Launch with Java 25 (the official launcher bundles it).
