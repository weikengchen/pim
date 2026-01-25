# Pinhoarder Mod - Agent Goals

This document outlines the primary goals and features for the Pinhoarder Minecraft mod. The mod focuses on enhancing the pin collection experience by providing visual aids and quality-of-life improvements for pin traders and collectors.

## Goal 1: Highlight Mint Pins in Player Inventory and Pinboards

**Objective:** Visually distinguish mint-condition pins from used/damaged pins in the player's inventory and on pinboard displays.

**Acceptance Criteria:**
- Mint pins display a distinctive visual indicator (e.g., border glow, icon overlay, or color tint)
- Indicator appears in both player inventory screens and on pinboard GUI
- Visual effect is configurable (color, intensity) via client settings
- Performance impact minimal (no significant FPS drop)

**Technical Considerations:**
- Client-side rendering hook for item stack rendering
- Custom item NBT tag or metadata detection for mint condition
- Integration with existing pin rendering systems

## Goal 2: Highlight Player's Missing Mint Pins in Inventory and Pinboards

**Objective:** Help players identify which mint pins they are missing from their collection when viewing inventory or pinboards.

**Acceptance Criteria:**
- Missing mint pins show a different visual indicator (e.g., translucent overlay, question mark icon)
- Works in inventory and pinboard interfaces
- Toggleable via hotkey or settings menu
- Respects player's collection tracking (requires integration with collection database)

**Technical Considerations:**
- Client-server communication for collection state
- Local caching of player's pin collection
- Custom rendering pipeline for missing item indication

## Goal 3: Defocus Non-Achievement Pin Series in /pinbook

**Objective:** When viewing the pin collection via `/pinbook` command, non-achievement pin series should be visually de-emphasized to focus attention on achievement-related pins.

**Acceptance Criteria:**
- Non-achievement pin series appear dimmed, blurred, or moved to secondary tabs
- Achievement pin series remain at full visibility
- Configurable intensity of defocus effect
- Option to toggle defocus on/off via command or GUI button

**Technical Considerations:**
- Modify `/pinbook` GUI rendering
- Pin series categorization metadata
- Custom GUI widget for tabbed interface (if needed)

## Goal 4: Client Commands to Warp to Closest Warp Points of Pin Traders

**Objective:** Provide quick navigation to pin traders through client-side commands that resolve to server warp commands.

**Acceptance Criteria:**
- Command `/pt <number>` warps player to the nearest warp point of the specified pin trader
- Command translates internally to `/warp [warp_point_name]` when sending to server
- Supports at least 10 pin trader shortcuts (e.g., `/pt 1` through `/pt 10`)
- Configurable mapping of numbers to warp points (via config file)
- Client-side command validation and error messages

**Technical Considerations:**
- Client command registration with Fabric API
- Server command translation and injection
- Config file management for warp point mappings
- Integration with server's warp system (IF - Inventory Framework?)

## Implementation Priority

1. Goal 4 (Warp Commands) - Most immediate utility for players
2. Goal 1 (Mint Pin Highlighting) - Core visual enhancement
3. Goal 3 (Pinbook Defocus) - Interface improvement
4. Goal 2 (Missing Pin Highlighting) - Requires collection tracking infrastructure

## Notes

- All features should be client-side where possible to maintain server compatibility
- Visual effects should be subtle and configurable to avoid UI clutter
- Consider accessibility: colorblind modes, configurable indicators
- Performance: Use efficient rendering techniques to minimize impact on gameplay

## Terminology Reference

- **Mint Pin:** A pin in perfect, undamaged condition
- **Pinboard:** A display board where pins can be placed and shown
- **/pinbook:** Command to view pin collection interface
- **Pin Trader:** NPC or location where pins can be traded
- **Warp Point:** Designated teleport location on the server