package i2.act.reduction.dd;

import i2.act.reduction.lists.ListReduction;
import i2.act.reduction.lists.ListReductionCallback;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

public abstract class DDVariant<E> implements ListReduction<E> {

  @Override
  public abstract List<E> reduce(final List<E> list, final ListReductionCallback<E> callback,
      final boolean testEmptyList);

  protected final List<E> applyConfiguration(final List<E> list, final BitSet configuration) {
    final List<E> current = new ArrayList<>(configuration.cardinality());

    for (int index = 0; index < configuration.size(); ++index) {
      if (configuration.get(index)) {
        current.add(list.get(index));
      }
    }

    return current;
  }

  protected final BitSet subset(final BitSet configuration, final int stepSize,
      final int subsetIndex) {
    final BitSet subsetConfiguration = new BitSet(configuration.size());

    final int subsetSize = subsetSize(configuration.cardinality(), stepSize);

    final int firstActualIndex = subsetIndex * subsetSize;
    final int lastActualIndex;
    {
      if (subsetIndex == stepSize - 1) {
        lastActualIndex = configuration.cardinality() - 1;
      } else {
        lastActualIndex = (subsetIndex + 1) * subsetSize - 1;
      }
    }

    int actualIndex = 0;

    for (int index = 0; index < configuration.size(); ++index) {
      if (configuration.get(index)) {
        if (actualIndex >= firstActualIndex && actualIndex <= lastActualIndex) {
          subsetConfiguration.set(index);
        }

        ++actualIndex;
      }
    }

    return subsetConfiguration;
  }

  protected final BitSet complement(final BitSet configuration, final int stepSize,
      final int complementIndex) {
    final BitSet complementConfiguration = new BitSet(configuration.size());

    final int subsetSize = subsetSize(configuration.cardinality(), stepSize);

    final int firstActualIndex = complementIndex * subsetSize;
    final int lastActualIndex;
    {
      if (complementIndex == stepSize - 1) {
        lastActualIndex = configuration.cardinality() - 1;
      } else {
        lastActualIndex = (complementIndex + 1) * subsetSize - 1;
      }
    }

    int actualIndex = 0;

    for (int index = 0; index < configuration.size(); ++index) {
      if (configuration.get(index)) {
        if (actualIndex < firstActualIndex || actualIndex > lastActualIndex) {
          complementConfiguration.set(index);
        }

        ++actualIndex;
      }
    }

    return complementConfiguration;
  }

  protected final int subsetSize(final int size, final int stepSize) {
    // this ensures that the first n - 1 subsets are as large as possible, but that the last subset
    // is not empty
    if (size % stepSize == 0 || (size / stepSize + 1) * (stepSize - 1) >= size) {
      return size / stepSize;
    } else {
      return size / stepSize + 1;
    }
  }

  protected final IntStream getIndices(final int stepSize, final boolean reverse) {
    if (reverse) {
      return IntStream.rangeClosed(0, stepSize - 1).map(i -> stepSize - i - 1);
    } else {
      return IntStream.rangeClosed(0, stepSize - 1);
    }
  }

}
