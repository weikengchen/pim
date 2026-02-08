# Pim! - Pin Improvement Mod 📌

A Fabric mod designed to enhance the pin collection and trading experience with visual aids and quality-of-life features for pin traders and collectors.

## ⚙️ Setup

After installation, the user needs to open all pages in `/pinrarity` and `/pinbook` to load the current pin series data. The user may need to open some pin series (blinking) to load the specific pins in the series to the mod.

## ✨ Highlights

The mod adds colored backgrounds and overlays to pins in various screens to help you quickly identify which pins to collect, trade, or ignore.

### 🎨 Color Legend

| Color | Meaning |
|-------|---------|
| 🟢 **Green background** | Mint pins you're missing - add these to your pinbook! |
| 🟡 **Golden background** | Mint pins you already have - still useful for trading |
| 🔴 **Magenta overlay** | Pins not relevant to you - can ignore |
| 🟠 **Yellow background** | Pin series not yet complete |

### 📊 Pin Trading Boards

When viewing pin trading boards (boards where you can exchange pins):

- **Green background**: Mint pins you should trade for - these are pins you don't have mint copies of yet
- **Golden background**: Mint pins you already own - can still be traded for other pins
- **Magenta overlay**: Pins you don't need - these aren't relevant to your collection

### 🎒 Inventory & Player HUD

When looking at your inventory or pins in your hotbar:

- **Green background**: Mint pins that can be added to your pinbook - you're missing the mint version!
- **Golden background**: Mint pins you already have in your pinbook - ready for trading
- **Magenta overlay**: Pins you don't need - these aren't relevant to your collection

### 📚 Pinbook Series

When browsing your pinbook collection:

- **Blinking**: Pin series with missing information - open the series to load pin details
- **Yellow background**: Incomplete series - you're still missing some pins
- **Green background**: Complete series with pins ready to add from inventory

### 📦 Pin Packs

When opening pin packs in the pinbook:

- **Magenta overlay**: Pins not relevant to you - can ignore
- **Green background**: Mint pins you can add to your pinbook
- **Golden background**: Mint pins you already have - can still be used for trading

## 🚀 Pin trading warp

1. Run the command `/pim:trade` to enable pin‑trading mode. You will see a confirmation message.
2. Hold an IFone and right‑click.
3. The mod will warp you to the first pin‑trader location and guide you to meet the pin trader.
4. Right‑click the IFone again to advance to the next distinct warp point. The boss bar updates accordingly.
5. When all traders have been visited, you will be warped back to Westward Ho Trading Company and pin‑trading mode will turn off automatically.

## 💻 Commands

### `/pim:compute` 🔢
Calculates how many pin packs and total cost you need to finish your required pin series collection.

**Usage:** `/pim:compute`

**Output:** Shows pin packs and cost estimates for each series.

### `/pim:export` 📦
Exports your pin collection as a human-readable text format designed for sharing with other players on Discord.

**Usage:** `/pim:export`

**Output:** A shareable text with your player name, :lookingfor: section (missing pins by series, skipped if none), and :forsale: section (available mint pins by series, skipped if none). Automatically copied to clipboard.

**Automatic Clipboard Parsing:** When you copy another player's export to your clipboard, the mod automatically parses it and displays matches in player chat:
- **You can offer to them:** Pins from your inventory that match the other player's :lookingfor: (series names in blue)
- **You need from them:** Pins from the other player's :forsale: that you don't have in your pinbook (series names in blue)

The mod also copies an exchange message to your clipboard formatted as:
```
I want to offer:
- [Series Name]: [pin names, ...]
I want to take:
- [Series Name]: [pin names, ...]
```

This message can be sent directly to the other player to confirm the exchange.

### `/pim:reset` 🔄
Clears all cached pin data. Use if you're having issues or need to reload pin info.

**Usage:** `/pim:reset`

**Output:** Confirmation that cache has been cleared.

### `/pim:trade` 🌀
Enables automatic warping between pin traders.

**Usage:** `/pim:trade`

**Output:** Confirmation when mode is toggled on/off.

### `/pim:value` 💰
Shows player specific values for all required pin series by rarity (signature, deluxe, rare, uncommon, common).

**Usage:** `/pim:value`

**Output:** Values for each rarity type that exists in each series. These values represent "how much each pin is worth" based on marginal utility.

### `/pim:fmv` 📊
Shows fair market values for pins in all required pin series.

**Usage:** `/pim:fmv`

**Output:** Displays FMV values for each rarity type (signature, deluxe, rare, uncommon, common) that exists in each series, showing floor and ceiling price ranges. Provides a clickable button to copy all values to clipboard in a compact format. Results are cached to avoid recalculation.

## 📄 License

This mod is released under the **CC0‑1.0** license (public domain). You may use, modify, and distribute it freely.