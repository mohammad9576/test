# Kodomo — art pack brief for Gemini (image generation)

Attach the five boards in `reference/` (00_overview … 04_parent) to the chat, then paste the **Master style** block
once, and after it ask for the images in batches (one message per group in `asset_manifest.csv`, 4–6 images per
message). Every image must be a separate file named exactly as its `path` in the manifest.

---

## Master style (paste once at the start)

You are the lead 3D character and icon artist for **Kodomo**, an Iranian children's launcher app. Match the attached
reference boards exactly: soft 3D clay / Pixar-like rendering, rounded shapes, big friendly eyes, glossy highlights,
soft light from the top-left, soft contact shadow, cheerful pastel palette (mint green #3DBE7A, lilac #A98BEA,
sky blue #5AB0F5, warm yellow #FFC93C, pink #F6A5C8, coral #FF8A65).

Cultural rules (must always hold):
- Iranian, Islamic, family-friendly. **Every girl and woman wears a hijab that fully covers the hair and neck**, with
  modest long-sleeved clothes. Boys and men wear modest clothes (no shorts above the knee).
- Gentle Persian touches where it fits: turquoise tile patterns (girih), pomegranates, Persian rugs, a small dome
  silhouette in skylines. Never religious text, never any written words, letters or numbers in the image (unless the
  line says so).
- No brand logos, no copyrighted characters, no weapons, no scary faces.

Recurring cast (keep them identical in every image — same face, colours and outfit):
- **Girl (7)**: pink hijab, lilac top, flower. **Boy (7)**: green cap, yellow hoodie, backpack.
- **Little girl/boy (4)**, **teen girl/boy (13)** as described in each line.
- **Kodomo robot**: round glossy white body, black face screen with green smiling eyes, green headphones, antenna.
- **Star wizard**: yellow star with a purple wizard hat.

Technical rules:
- One subject per image, centred, nothing cut off at the edges, 8% empty margin.
- Background: **transparent PNG**. If you cannot export transparency, use a perfectly flat **#00FF00** background
  (use **#FF00FF** for the robot, the dragon and anything green). No gradient, no floor, no shadow on the background.
- Size as the manifest says (square 1024×1024 for characters; 512×512 for icons; 1080×2400 for scenes and maps).
- Icons (`icons/…`): the object in clay 3D sitting on a glossy rounded-square tile of the given colour, like iOS app
  icons, no text.
- Scenes (`scenes/…`): three separate layers per theme — `_sky` (full background), `_hills` (landscape, transparent
  above the horizon), `_front` (grass and flowers along the bottom only).

Reply with the images only, each labelled with its exact file name.

---

## Batch messages (examples)

> Batch 1 — make these 4 files: `characters/girl.png` (half-body portrait, warm smile), `characters/girl_wave.png`,
> `characters/girl_happy.png` (full body, jumping), `characters/girl_sleep.png` (sleeping, hugging a yellow star).

> Batch 2 — robot: `robot/happy.png`, `robot/wave.png`, `robot/think.png`, `robot/sleep.png`, `robot/cheer.png`,
> `robot/point.png`.

Continue with the remaining lines of `asset_manifest.csv` (the `subject_en` column is the description to paste).

## Consistency tips
- Make the **character sheet first** (girl, boy, robot from front) and keep re-attaching it in later batches:
  "same character as the attached sheet".
- If one image drifts (hair visible, different face), ask only for that file again.
- Save everything in folders matching the paths (`characters/`, `robot/`, `icons/` …) and zip the folder.
