package com.chenweikeng.pim.command;

import com.chenweikeng.pim.pin.Algorithm;
import com.chenweikeng.pim.pin.Algorithm.DPResult;
import com.chenweikeng.pim.pin.Rarity;
import com.chenweikeng.pim.screen.PinBookHandler;
import com.chenweikeng.pim.screen.PinDetailHandler;
import com.chenweikeng.pim.screen.PinRarityHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for sharing pin calculation methods between commands. Provides reusable methods for
 * getting pin series counts and calculating fair prices.
 */
public class PinCalculationUtils {

  // Shared cache for start points and results
  private static final ConcurrentHashMap<String, Algorithm.DPStartPoint> cachedStartPoints =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, DPResult> cachedResults =
      new ConcurrentHashMap<>();

  /**
   * Gets pin series counts for a given series, reusing the logic from PimComputeCommand. Returns
   * null if series data is incomplete or unavailable.
   */
  public static Algorithm.PinSeriesCounts getPinSeriesCounts(String seriesName) {
    try {
      // Get series details
      Map<String, PinDetailHandler.PinDetailEntry> detailMap =
          PinDetailHandler.getInstance().getSeriesDetails(seriesName);
      if (detailMap == null || detailMap.isEmpty()) {
        return null;
      }

      // Get book entry for total counts
      PinBookHandler.PinBookEntry bookEntry = PinBookHandler.getInstance().getBookEntry(seriesName);
      if (bookEntry == null) {
        return null;
      }

      // Count pins by rarity
      int signature = 0;
      int deluxe = 0;
      int rare = 0;
      int uncommon = 0;
      int common = 0;

      int mintSignature = 0;
      int mintDeluxe = 0;
      int mintRare = 0;
      int mintUncommon = 0;
      int mintCommon = 0;

      for (Map.Entry<String, PinDetailHandler.PinDetailEntry> entry : detailMap.entrySet()) {
        PinDetailHandler.PinDetailEntry detailEntry = entry.getValue();
        if (detailEntry.rarity == null) {
          continue; // Skip entries without rarity
        }

        switch (detailEntry.rarity) {
          case SIGNATURE:
            signature++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintSignature++;
            }
            break;
          case DELUXE:
            deluxe++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintDeluxe++;
            }
            break;
          case RARE:
            rare++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintRare++;
            }
            break;
          case UNCOMMON:
            uncommon++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintUncommon++;
            }
            break;
          case COMMON:
            common++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintCommon++;
            }
            break;
        }
      }

      // Create goal and start point
      Algorithm.DPGoal goal = new Algorithm.DPGoal(signature, deluxe, rare, uncommon, common);
      Algorithm.DPStartPoint startPoint =
          new Algorithm.DPStartPoint(mintSignature, mintDeluxe, mintRare, mintUncommon, mintCommon);

      return new Algorithm.PinSeriesCounts(goal, startPoint);

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets cached algorithm result or runs the algorithm if needed. Returns null if calculation
   * fails.
   */
  public static DPResult getCachedOrCalculateResult(
      String seriesName, Algorithm.PinSeriesCounts counts) {
    // Check cache for start point
    Algorithm.DPStartPoint cachedStartPoint = cachedStartPoints.get(seriesName);

    DPResult result;
    if (cachedStartPoint != null && cachedStartPoint.equals(counts.startPoint)) {
      result = cachedResults.get(seriesName);
    } else {
      result = Algorithm.runDynamicProgramming(seriesName, counts);
      if (result != null && !result.isError()) {
        cachedStartPoints.put(seriesName, counts.startPoint);
        cachedResults.put(seriesName, result);
      }
    }

    return result;
  }

  /**
   * Calculates fair prices for each rarity type in a series using delta values from the algorithm.
   * Fair price = (delta * pinbox_price) / 2 Returns empty map if delta values are not available.
   */
  public static Map<Rarity, Double> calculateFairPricesForSeries(String seriesName) {
    Map<Rarity, Double> fairPrices = new HashMap<>();

    // Get series entry to find pinbox price
    PinRarityHandler.PinSeriesEntry seriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(seriesName);
    if (seriesEntry == null || seriesEntry.color == null) {
      return fairPrices; // Empty map if no price data
    }

    double pinboxPrice = seriesEntry.color.price;

    // Get pin series counts
    Algorithm.PinSeriesCounts counts = getPinSeriesCounts(seriesName);
    if (counts == null) {
      return fairPrices; // Empty map if no series data
    }

    // Get cached or calculated result
    DPResult result = getCachedOrCalculateResult(seriesName, counts);
    if (result == null || result.isError()) {
      return fairPrices; // Empty map if calculation fails
    }

    // Calculate fair prices for each rarity type where delta is available
    if (result.whatIfOneMoreSignature.isPresent()) {
      fairPrices.put(Rarity.SIGNATURE, (result.whatIfOneMoreSignature.get() * pinboxPrice) / 2.0);
    }
    if (result.whatIfOneMoreDeluxe.isPresent()) {
      fairPrices.put(Rarity.DELUXE, (result.whatIfOneMoreDeluxe.get() * pinboxPrice) / 2.0);
    }
    if (result.whatIfOneMoreRare.isPresent()) {
      fairPrices.put(Rarity.RARE, (result.whatIfOneMoreRare.get() * pinboxPrice) / 2.0);
    }
    if (result.whatIfOneMoreUncommon.isPresent()) {
      fairPrices.put(Rarity.UNCOMMON, (result.whatIfOneMoreUncommon.get() * pinboxPrice) / 2.0);
    }
    if (result.whatIfOneMoreCommon.isPresent()) {
      fairPrices.put(Rarity.COMMON, (result.whatIfOneMoreCommon.get() * pinboxPrice) / 2.0);
    }

    return fairPrices;
  }
}
