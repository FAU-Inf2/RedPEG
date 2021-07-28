package i2.act.reduction.dd;

import i2.act.reduction.lists.ListReductionCallback;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class DDMin<E> extends DDVariant<E> {

  public static final boolean DEFAULT_REDUCE_TO_SUBSET = true;
  public static final boolean DEFAULT_REDUCE_TO_COMPLEMENT = true;
  public static final boolean DEFAULT_REVERSE_SUBSETS = false;
  public static final boolean DEFAULT_REVERSE_COMPLEMENTS = false;

  private final boolean reduceToSubset;
  private final boolean reduceToComplement;

  private final boolean reverseSubsets;
  private final boolean reverseComplements;

  public DDMin() {
    this(DEFAULT_REDUCE_TO_SUBSET, DEFAULT_REDUCE_TO_COMPLEMENT,
        DEFAULT_REVERSE_SUBSETS, DEFAULT_REVERSE_COMPLEMENTS);
  }

  public DDMin(final boolean reduceToSubset, final boolean reduceToComplement,
      final boolean reverseSubsets, final boolean reverseComplements) {
    this.reduceToSubset = reduceToSubset;
    this.reduceToComplement = reduceToComplement;
    this.reverseSubsets = reverseSubsets;
    this.reverseComplements = reverseComplements;
  }

  @Override
  public final List<E> reduce(final List<E> list, final ListReductionCallback<E> callback,
      final boolean testEmptyList) {
    if (testEmptyList) {
      final List<E> emptyList = new ArrayList<>();
      if (callback.test(emptyList)) {
        return emptyList;
      }
    }

    final BitSet initialConfiguration = new BitSet(list.size());
    initialConfiguration.flip(0, list.size());

    final Set<BitSet> failCache = new HashSet<>();

    final List<E> reduced = ddMin(list, initialConfiguration, 2, failCache, callback);
    return reduced;
  }

  private final List<E> ddMin(final List<E> list, final BitSet configuration, final int stepSize,
      final Set<BitSet> failCache, final ListReductionCallback<E> callback) {
    final int size = configuration.cardinality();

    if (size < 2) {
      final List<E> current = applyConfiguration(list, configuration);
      return current;
    }

    if (this.reduceToSubset) {
      final IntStream subsetIndices = getIndices(stepSize, this.reverseSubsets);
      for (final int subsetIndex : (Iterable<Integer>) subsetIndices::iterator) {
        final BitSet subsetConfiguration = subset(configuration, stepSize, subsetIndex);

        if (!failCache.contains(subsetConfiguration)) {
          final List<E> subset = applyConfiguration(list, subsetConfiguration);

          if (callback.test(subset)) {
            return ddMin(list, subsetConfiguration, 2, failCache, callback);
          } else {
            failCache.add(subsetConfiguration);
          }
        }
      }
    }

    // for n=2, complement 1 equals subset 2 and vice versa
    if (stepSize != 2 && this.reduceToComplement) {

      final IntStream complementIndices = getIndices(stepSize, this.reverseComplements);
      for (final int complementIndex : (Iterable<Integer>) complementIndices::iterator) {
        final BitSet complementConfiguration = complement(configuration, stepSize, complementIndex);

        if (!failCache.contains(complementConfiguration)) {
          final List<E> complement = applyConfiguration(list, complementConfiguration);

          if (callback.test(complement)) {
            final int newStepSize = Math.max(stepSize - 1, 2);
            return ddMin(list, complementConfiguration, newStepSize, failCache, callback);
          } else {
            failCache.add(complementConfiguration);
          }
        }
      }
    }

    if (stepSize < size) {
      final int newStepSize = Math.min(size, 2 * stepSize);
      return ddMin(list, configuration, newStepSize, failCache, callback);
    }

    final List<E> current = applyConfiguration(list, configuration);
    return current;
  }

}
