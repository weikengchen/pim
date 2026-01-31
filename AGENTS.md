# Pinhoarder Mod - Agent Goals & Features

This document outlines the goals and features for the Pinhoarder Minecraft mod. The mod enhances the pin collection and trading experience by providing visual aids, quality-of-life improvements, and trading utilities for pin traders and collectors.

## Implemented Features

### Visual Highlighting System

**Overview:** Color-coded backgrounds and overlays help players quickly identify which pins to collect, trade, or ignore.

**Color Legend:**
- 🟢 **Green background**: Mint pins you're missing - add these to your pinbook!
- 🟡 **Golden background**: Mint pins you already have - useful for trading
- 🔴 **Magenta overlay**: Pins not relevant to you - can ignore
- 🟠 **Yellow background**: Incomplete pin series - you're still missing some pins
- **Blinking**: Pin series with missing information - open the series to load pin details

**Implemented Screens:**
- **Pin Trading Boards**: Shows green for pins to trade for, golden for owned mint pins, magenta for irrelevant
- **Inventory & Player HUD**: Green for mint pins that can be added to pinbook, golden for already owned, magenta for irrelevant
- **Pinbook Series**: Blinking for missing info, yellow for incomplete, green for complete
- **Pin Packs**: Magenta overlay for irrelevant, green for addable, golden for owned

### Pin Trading Warp Mode

**Command:** `/pim:trade`

**Workflow:**
1. Run `/pim:trade` to enable pin-trading mode
2. Hold an IFone and right-click
3. Warps to the first pin-trader location with boss bar guidance
4. Right-click the IFone again to advance to the next distinct warp point
5. Automatically warps back to Westward Ho Trading Company when all traders visited and disables mode

**Technical Details:**
- Skips traders at the same location to avoid redundant warps
- Boss bar shows current trader and progress
- Plays bell sound when switching to next trader
- Auto-disables when all traders visited

### Magic String System

Allows players to share and compare pin collections via encoded strings.

**Commands:**
- `/pim:export` - Exports your current mint pin collection as a magic string
- `/pim:match <magic_string>` - Compares a shared magic string against your collection to show missing pins
- `/pim:view <magic_string>` - Displays all pins in a magic string without comparison

**Features:**
- Encodes only REQUIRED series mint pins
- Shows inline suggested values for missing pins (if `/pim:compute` has been run)
- Two-level tree display organized by series and pin name

### Price Calculation System

**Commands:**
- `/pim:compute` - Calculates marginal values for all pin series using dynamic programming
- `/pim:price` - Displays suggested values for all REQUIRED pin series by rarity type

**Algorithm:**
- Uses dynamic programming to calculate marginal value of adding one pin of each rarity type
- Suggested value = (delta * pinbox_price) / 2
- Caches results to avoid recalculation
- Supports: Signature, Deluxe, Rare, Uncommon, Common rarity types

### Data Management

**Handlers:**
- `PinRarityHandler` - Manages pin series metadata (REQUIRED vs OPTIONAL, pinbox prices)
- `PinBookHandler` - Tracks player's pinbook collection and completion status
- `PinDetailHandler` - Tracks individual pin details including condition (mint vs non-mint)
- `PinCalculationUtils` - Utility class for value calculations

**Command:**
- `/pim:reset` - Resets all cached pin data

**Persistence:**
- Pin rarity data saved to `config/pim_pin_rarity.json`
- Cached algorithm results stored in memory

## Project Structure

```
src/main/java/com/chenweikeng/pim/
├── command/           # Client commands (/pim:*)
│   ├── PimCommand.java         # Command registration
│   ├── PimTradeCommand.java    # Pin trading warp
│   ├── PimResetCommand.java    # Reset data
│   ├── PimComputeCommand.java  # Price calculation
│   ├── PimExportCommand.java   # Export magic string
│   ├── PimMatchCommand.java    # Match magic strings
│   ├── PimViewCommand.java     # View magic strings
│   └── PimPriceCommand.java    # Display prices
├── pin/              # Pin-related utilities
│   ├── PinCalculationUtils.java # Value calculation logic
│   ├── MagicStringUtils.java    # Magic string parsing/display
│   ├── MagicString.java         # Magic string generation
│   ├── Algorithm.java           # Dynamic programming algorithm
│   └── Rarity.java              # Rarity enum
├── screen/           # Screen data handlers
│   ├── PinRarityHandler.java    # Pin series metadata
│   ├── PinBookHandler.java      # Pinbook collection
│   └── PinDetailHandler.java    # Pin details
├── mixin/            # Minecraft client hooks
│   ├── AbstractContainerScreenMixin.java  # Inventory rendering
│   ├── GuiMixin.java                    # HUD rendering
│   ├── ClientTickMixin.java             # Tick handlers
│   └── ClientPacketListenerMixin.java   # Packet handlers
├── tracker/          # Boss bar tracking
│   └── BossBarTracker.java
└── PimClient.java    # Mod entry point
```

## Key Technical Patterns

### Mixin Usage
- Hook into rendering pipelines for visual highlights
- Intercept container packets to parse pin pack data
- Modify GUI rendering for inventory and pinbook screens

### Dynamic Programming
- Used in `Algorithm.java` to calculate optimal pin completion paths
- Caches start points and results for performance
- Handles cases where algorithm fails gracefully (returns empty values)

### Color System
- `PinPackColor` enum maps pin pack colors to prices
- Uses Minecraft's `ChatFormatting` for colored text
- Component API for rich text formatting with styles

## Configuration Files

- `gradle.properties` - Version 1.1.0, Minecraft 1.21.11, Fabric API 0.141.1
- `fabric.mod.json` - Mod metadata
- `pim.mixins.json` - Mixin configuration

## Terminology

- **Mint Pin**: A pin in perfect, undamaged condition
- **REQUIRED Series**: Pin series available at pin shops (currently relevant)
- **OPTIONAL Series**: Pin series no longer sold (legacy content)
- **Pinbook**: Command `/pinbook` to view pin collection interface
- **Pin Trader**: NPC or location where pins can be traded
- **Warp Point**: Designated teleport location on the server
- **Magic String**: Encoded string representing a player's mint pin collection
- **Suggested Value**: Calculated trading value based on marginal utility

## Notes for Development

- All features are client-side for server compatibility
- Visual effects are subtle to avoid UI clutter
- Performance is prioritized (caching, efficient rendering)
- Error handling is graceful (returns empty/null on failure)
- Data loading happens automatically from GUI interactions (/pinbook, /pinrarity)
