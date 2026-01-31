package com.chenweikeng.pim.pin;

import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class MagicString {

  private static final String PREFIX = "Pim!";

  public static String generateChecksum() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      PinRarityHandler handler = PinRarityHandler.getInstance();
      java.util.Set<String> allSeriesNames = handler.getAllSeriesNames();

      TreeMap<String, String> sortedSeries = new TreeMap<>();
      for (String seriesName : allSeriesNames) {
        PinRarityHandler.PinSeriesEntry seriesEntry = handler.getSeriesEntry(seriesName);
        if (seriesEntry != null
            && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
          sortedSeries.put(seriesName, seriesName);
        }
      }

      for (String seriesName : sortedSeries.keySet()) {
        Map<String, PinDetailHandler.PinDetailEntry> detailMap =
            PinDetailHandler.getInstance().getSeriesDetails(seriesName);
        int totalPins = detailMap != null ? detailMap.size() : 0;

        String hashInput = seriesName + ":" + totalPins;
        digest.update(hashInput.getBytes(StandardCharsets.UTF_8));
      }

      byte[] fullHash = digest.digest();
      byte[] truncatedHash = new byte[4];
      System.arraycopy(fullHash, 0, truncatedHash, 0, 4);

      StringBuilder hexString = new StringBuilder();
      for (byte b : truncatedHash) {
        hexString.append(String.format("%02x", b));
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  public static String generate() {
    String checksum = generateChecksum();
    Set<String> playerMintPins = getPlayerInventoryMintPins();
    String bitmap = generateBitmap(playerMintPins);
    return PREFIX + checksum + bitmap;
  }

  private static String generateBitmap(Set<String> playerMintPins) {
    PinRarityHandler handler = PinRarityHandler.getInstance();
    java.util.Set<String> allSeriesNames = handler.getAllSeriesNames();

    TreeMap<String, List<String>> sortedRequiredSeries = new TreeMap<>();

    for (String seriesName : allSeriesNames) {
      PinRarityHandler.PinSeriesEntry seriesEntry = handler.getSeriesEntry(seriesName);
      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        Map<String, PinDetailHandler.PinDetailEntry> detailMap =
            PinDetailHandler.getInstance().getSeriesDetails(seriesName);
        if (detailMap != null && !detailMap.isEmpty()) {
          List<String> sortedPinNames = new ArrayList<>(detailMap.keySet());
          sortedPinNames.sort(String::compareTo);
          sortedRequiredSeries.put(seriesName, sortedPinNames);
        }
      }
    }

    List<Boolean> bitmapBits = new ArrayList<>();

    for (Map.Entry<String, List<String>> seriesEntry : sortedRequiredSeries.entrySet()) {
      String seriesName = seriesEntry.getKey();
      List<String> sortedPinNames = seriesEntry.getValue();

      for (String pinName : sortedPinNames) {
        String key = seriesName + ":" + pinName;
        bitmapBits.add(playerMintPins.contains(key));
      }
    }

    BitSet bitSet = new BitSet(bitmapBits.size());
    for (int i = 0; i < bitmapBits.size(); i++) {
      if (bitmapBits.get(i)) {
        bitSet.set(i);
      }
    }

    byte[] bitmapBytes = bitSet.toByteArray();
    return encodeBase58(bitmapBytes);
  }

  private static String encodeBase58(byte[] data) {
    String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    java.math.BigInteger bigInt = new java.math.BigInteger(1, data);
    StringBuilder result = new StringBuilder();

    while (bigInt.compareTo(java.math.BigInteger.ZERO) > 0) {
      java.math.BigInteger[] divmod = bigInt.divideAndRemainder(java.math.BigInteger.valueOf(58));
      bigInt = divmod[0];
      result.insert(0, ALPHABET.charAt(divmod[1].intValue()));
    }

    for (byte b : data) {
      if (b == 0) {
        result.insert(0, ALPHABET.charAt(0));
      } else {
        break;
      }
    }

    return result.toString();
  }

  public static Set<String> getPlayerInventoryMintPins() {
    Set<String> mintPins = new HashSet<>();
    Minecraft mc = Minecraft.getInstance();

    if (mc.player == null) {
      return mintPins;
    }

    var inv = mc.player.getInventory();
    var items = inv.getNonEquipmentItems();

    for (ItemStack stack : items) {
      processItemStack(stack, mintPins);
    }

    return mintPins;
  }

  /**
   * Gets player mint pins from PinDetail data instead of inventory. This provides a more reliable
   * source of what pins the player actually has in mint condition.
   */
  public static Set<String> getPlayerDetailMintPins() {
    Set<String> mintPins = new HashSet<>();
    PinDetailHandler handler = PinDetailHandler.getInstance();

    // Get all series from the detail handler
    Set<String> allSeries = handler.getAllSeriesNames();

    for (String seriesName : allSeries) {
      Map<String, PinDetailHandler.PinDetailEntry> seriesPins =
          handler.getSeriesDetails(seriesName);
      if (seriesPins != null) {
        // Check each pin in the series
        for (Map.Entry<String, PinDetailHandler.PinDetailEntry> pinEntry : seriesPins.entrySet()) {
          String pinName = pinEntry.getKey();
          PinDetailHandler.PinDetailEntry detail = pinEntry.getValue();

          // Only include pins that are in mint condition
          if (detail.condition == PinDetailHandler.PinCondition.MINT) {
            mintPins.add(seriesName + ":" + pinName);
          }
        }
      }
    }

    return mintPins;
  }

  private static void processItemStack(ItemStack stack, Set<String> mintPins) {
    if (stack.isEmpty()) {
      return;
    }

    if (isShulkerBox(stack)) {
      if (stack.has(DataComponents.CONTAINER)) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);

        for (ItemStack inside : container.stream().toList()) {
          processItemStack(inside, mintPins);
        }
      }
      return;
    }

    PinDetailHandler.PinDetailEntry entry = PinDetailHandler.getInstance().parsePinEntry(stack);

    if (entry != null && entry.condition == PinDetailHandler.PinCondition.MINT) {
      String pinSeries = PinDetailHandler.getInstance().parsePinSeriesFromLore(stack);
      String pinName = entry.pinName;

      if (pinSeries != null && pinName != null) {
        PinRarityHandler.PinSeriesEntry seriesEntry =
            PinRarityHandler.getInstance().getSeriesEntry(pinSeries);

        if (seriesEntry != null
            && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
          mintPins.add(pinSeries + ":" + pinName);
        }
      }
    }
  }

  private static boolean isShulkerBox(ItemStack stack) {
    return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
  }

  public static Set<String> decodeBitmap(String base58Bitmap) {
    byte[] bitmapBytes = decodeBase58(base58Bitmap);
    BitSet bitSet = BitSet.valueOf(bitmapBytes);

    PinRarityHandler handler = PinRarityHandler.getInstance();
    java.util.Set<String> allSeriesNames = handler.getAllSeriesNames();

    TreeMap<String, List<String>> sortedRequiredSeries = new TreeMap<>();

    for (String seriesName : allSeriesNames) {
      PinRarityHandler.PinSeriesEntry seriesEntry = handler.getSeriesEntry(seriesName);
      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        Map<String, PinDetailHandler.PinDetailEntry> detailMap =
            PinDetailHandler.getInstance().getSeriesDetails(seriesName);
        if (detailMap != null && !detailMap.isEmpty()) {
          List<String> sortedPinNames = new ArrayList<>(detailMap.keySet());
          sortedPinNames.sort(String::compareTo);
          sortedRequiredSeries.put(seriesName, sortedPinNames);
        }
      }
    }

    Set<String> result = new HashSet<>();
    int bitIndex = 0;

    for (Map.Entry<String, List<String>> seriesEntry : sortedRequiredSeries.entrySet()) {
      String seriesName = seriesEntry.getKey();
      List<String> sortedPinNames = seriesEntry.getValue();

      for (String pinName : sortedPinNames) {
        if (bitIndex < bitSet.length() && bitSet.get(bitIndex)) {
          result.add(seriesName + ":" + pinName);
        }
        bitIndex++;
      }
    }

    return result;
  }

  private static byte[] decodeBase58(String base58) {
    String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    java.math.BigInteger bigInt = java.math.BigInteger.ZERO;

    for (char c : base58.toCharArray()) {
      int digit = ALPHABET.indexOf(c);
      if (digit == -1) {
        throw new IllegalArgumentException("Invalid Base58 character: " + c);
      }
      bigInt =
          bigInt
              .multiply(java.math.BigInteger.valueOf(58))
              .add(java.math.BigInteger.valueOf(digit));
    }

    byte[] bytes = bigInt.toByteArray();
    if (bytes[0] == 0 && bytes.length > 1) {
      byte[] temp = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, temp, 0, temp.length);
      bytes = temp;
    }

    for (int i = 0; i < base58.length(); i++) {
      if (base58.charAt(i) == ALPHABET.charAt(0)) {
        byte[] temp = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, temp, 1, bytes.length);
        temp[0] = 0;
        bytes = temp;
      } else {
        break;
      }
    }

    return bytes;
  }
}
