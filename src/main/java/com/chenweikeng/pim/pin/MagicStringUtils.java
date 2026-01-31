package com.chenweikeng.pim.pin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.network.chat.Component;

public class MagicStringUtils {

  public static class MagicStringParseResult {
    public final Set<String> mintPins;
    public final String error;

    public MagicStringParseResult(Set<String> mintPins) {
      this.mintPins = mintPins;
      this.error = null;
    }

    public MagicStringParseResult(String error) {
      this.mintPins = null;
      this.error = error;
    }

    public boolean isValid() {
      return error == null;
    }
  }

  public static MagicStringParseResult parseMagicString(String magicString) {
    if (!magicString.startsWith("Pim!")) {
      return new MagicStringParseResult("Invalid magic string format! Must start with 'Pim!'");
    }

    if (magicString.length() < 12) {
      return new MagicStringParseResult("Invalid magic string length!");
    }

    String expectedChecksum = MagicString.generateChecksum();
    String providedChecksum = magicString.substring(4, 12);

    if (!expectedChecksum.equals(providedChecksum)) {
      return new MagicStringParseResult(
          "Checksum mismatch! Expected: " + expectedChecksum + ", Got: " + providedChecksum);
    }

    String base58Bitmap = magicString.substring(12);
    Set<String> mintPins;
    try {
      mintPins = MagicString.decodeBitmap(base58Bitmap);
    } catch (Exception e) {
      return new MagicStringParseResult("Failed to decode bitmap: " + e.getMessage());
    }

    return new MagicStringParseResult(mintPins);
  }

  public static Map<String, List<String>> organizePinsBySeries(Set<String> pinKeys) {
    Map<String, List<String>> pinsBySeries = new TreeMap<>();

    for (String pinKey : pinKeys) {
      String[] parts = pinKey.split(":");
      if (parts.length == 2) {
        String seriesName = parts[0];
        String pinName = parts[1];
        pinsBySeries.computeIfAbsent(seriesName, k -> new ArrayList<>()).add(pinName);
      }
    }

    return pinsBySeries;
  }

  public static Map<String, Set<String>> organizePinsBySeriesAsSet(Set<String> pinKeys) {
    Map<String, Set<String>> pinsBySeries = new TreeMap<>();

    for (String pinKey : pinKeys) {
      String[] parts = pinKey.split(":");
      if (parts.length == 2) {
        String seriesName = parts[0];
        String pinName = parts[1];
        pinsBySeries.computeIfAbsent(seriesName, k -> new HashSet<>()).add(pinName);
      }
    }

    return pinsBySeries;
  }

  public static Component twoLevelTree(String title, Map<String, List<String>> data) {
    net.minecraft.network.chat.MutableComponent tree = Component.literal(title + ":\n");

    List<Map.Entry<String, List<String>>> entries = new ArrayList<>(data.entrySet());
    int totalGroups = entries.size();

    if (totalGroups == 0) {
      return tree.append(Component.literal("(none)\n"));
    }

    for (int i = 0; i < totalGroups; i++) {
      Map.Entry<String, List<String>> entry = entries.get(i);
      boolean lastGroup = (i == totalGroups - 1);
      String groupPrefix = lastGroup ? "└─ " : "├─ ";

      tree.append(Component.literal(groupPrefix + entry.getKey() + "\n"));

      List<String> children = entry.getValue();

      for (int j = 0; j < children.size(); j++) {
        boolean lastChild = (j == children.size() - 1);
        String childPrefix = (lastGroup ? "   " : "│  ") + (lastChild ? "└─ " : "├─ ");

        String pinName = children.get(j);

        tree.append(Component.literal(childPrefix + pinName)).append(Component.literal("\n"));
      }
    }

    return tree;
  }

  /** Formats a value into a human-readable string (e.g., "1.2K", "500"). */
  public static String formatPrice(double value) {
    if (value >= 1000) {
      return String.format("%.1fK", value / 1000.0);
    } else {
      return String.format("%.0f", value);
    }
  }

  public static Component twoLevelTree(
      String title,
      Map<String, List<String>> data,
      Map<String, Map<String, Double>> suggestedPrices) {
    net.minecraft.network.chat.MutableComponent tree = Component.literal(title + ":\n");

    List<Map.Entry<String, List<String>>> entries = new ArrayList<>(data.entrySet());
    int totalGroups = entries.size();

    if (totalGroups == 0) {
      return tree.append(Component.literal("(none)\n"));
    }

    for (int i = 0; i < totalGroups; i++) {
      Map.Entry<String, List<String>> entry = entries.get(i);
      boolean lastGroup = (i == totalGroups - 1);
      String groupPrefix = lastGroup ? "└─ " : "├─ ";

      tree.append(Component.literal(groupPrefix + entry.getKey() + "\n"));

      List<String> children = entry.getValue();
      Map<String, Double> seriesSuggestedPrices = suggestedPrices.get(entry.getKey());

      for (int j = 0; j < children.size(); j++) {
        boolean lastChild = (j == children.size() - 1);
        String childPrefix = (lastGroup ? "   " : "│  ") + (lastChild ? "└─ " : "├─ ");

        String pinName = children.get(j);

        net.minecraft.network.chat.MutableComponent pinComponent;
        if (seriesSuggestedPrices != null && seriesSuggestedPrices.containsKey(pinName)) {
          double value = seriesSuggestedPrices.get(pinName);
          pinComponent =
              Component.literal(pinName)
                  .append(Component.literal(" "))
                  .append(
                      Component.literal("Value: " + formatPrice(value))
                          .withStyle(
                              net.minecraft.network.chat.Style.EMPTY
                                  .withColor(net.minecraft.ChatFormatting.GRAY)
                                  .withItalic(true)));
        } else {
          pinComponent = Component.literal(pinName);
        }

        tree.append(Component.literal(childPrefix))
            .append(pinComponent)
            .append(Component.literal("\n"));
      }
    }

    return tree;
  }
}
