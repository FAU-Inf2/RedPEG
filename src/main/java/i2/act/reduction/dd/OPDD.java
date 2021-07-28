package i2.act.reduction.dd;

import i2.act.reduction.lists.ListReductionCallback;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class OPDD<E> extends DDVariant<E> {

  public static final boolean DEFAULT_REVERSE_SUBSETS = false;
  public static final boolean DEFAULT_REVERSE_COMPLEMENTS = false;

  private final boolean reverseSubsets;
  private final boolean reverseComplements;

  public OPDD() {
    this(DEFAULT_REVERSE_SUBSETS, DEFAULT_REVERSE_COMPLEMENTS);
  }

  public OPDD(final boolean reverseSubsets, final boolean reverseComplements) {
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

    return opdd(list, initialConfiguration, 2, failCache, callback);
  }

  private final List<E> opdd(final List<E> list, final BitSet configuration, final int stepSize,
      final Set<BitSet> failCache, final ListReductionCallback<E> callback) {
    final int size = configuration.cardinality();

    if (size < 2) {
      final List<E> current = applyConfiguration(list, configuration);
      return current;
    }

    // reduce to subset
    {
      final IntStream subsetIndices = getIndices(stepSize, this.reverseSubsets);
      for (final int subsetIndex : (Iterable<Integer>) subsetIndices::iterator) {
        final BitSet subsetConfiguration = subset(configuration, stepSize, subsetIndex);

        if (!failCache.contains(subsetConfiguration)) {
          final List<E> subset = applyConfiguration(list, subsetConfiguration);

          if (callback.test(subset)) {
            return opdd(list, subsetConfiguration, 2, failCache, callback);
          } else {
            failCache.add(subsetConfiguration);
          }
        }
      }
    }

    // for n=2, the complements equal the subsets
    if (stepSize == 2) {
      return refine(list, configuration, stepSize, failCache, callback);
    }

    // reduce to complement
    {
      final List<BitSet> subsets = allSubsets(configuration, stepSize);

      List<BitSet> reduced = new ArrayList<>(subsets);
      int newStepSize = stepSize;

      // the 'for' loop realizes the 'foldl' over all subsets
      final IntStream subsetIndices = getIndices(stepSize, this.reverseComplements); // sic!
      for (final int subsetIndex : (Iterable<Integer>) subsetIndices::iterator) {
        final BitSet subset = subsets.get(subsetIndex);

        final List<BitSet> candidateConfigurations = new ArrayList<>(reduced);
        candidateConfigurations.remove(subset);

        final BitSet candidateConfiguration = flatten(candidateConfigurations);

        if (!failCache.contains(candidateConfiguration)) {
          final List<E> candidate = applyConfiguration(list, candidateConfiguration);

          if (callback.test(candidate)) {
            reduced = candidateConfigurations;
            newStepSize -= 1;
          } else {
            failCache.add(candidateConfiguration);
          }
        }
      }

      final BitSet resultConfiguration = flatten(reduced);
      return refine(list, resultConfiguration, newStepSize, failCache, callback);
    }
  }

  private final List<E> refine(final List<E> list, final BitSet configuration, final int stepSize,
      final Set<BitSet> failCache, final ListReductionCallback<E> callback) {
    if (stepSize < configuration.cardinality()) {
      final int newStepSize = Math.min(configuration.cardinality(), 2 * stepSize);
      return opdd(list, configuration, newStepSize, failCache, callback);
    } else {
      final List<E> current = applyConfiguration(list, configuration);
      return current;
    }
  }

  private final List<BitSet> allSubsets(final BitSet configuration, final int stepSize) {
    final List<BitSet> subsets = new ArrayList<>();

    for (int subsetIndex = 0; subsetIndex < stepSize; ++subsetIndex) {
      final BitSet subset = subset(configuration, stepSize, subsetIndex);
      subsets.add(subset);
    }

    return subsets;
  }

  private final BitSet flatten(final List<BitSet> configurations) {
    assert (configurations.size() > 0);

    final BitSet flattened = new BitSet(configurations.get(0).size());

    for (final BitSet configuration : configurations) {
      flattened.or(configuration);
    }

    return flattened;
  }

}
