package i2.act.reduction.regex;

import i2.act.peg.ast.*;

import java.util.*;

public final class SmallStringGenerator {

  public static final String generateString(final RegularExpression regularExpression) {
    return generateStrings(regularExpression, 1, false).get(0);
  }

  public static final List<String> generateStrings(final RegularExpression regularExpression,
      final int count, final boolean eachAlternative) {
    final Set<String> strings = new LinkedHashSet<>();

    final RegularExpressionEnumerator enumerator =
        (RegularExpressionEnumerator) createEnumerator(regularExpression, eachAlternative);

    // generate more strings
    while (((strings.size() < count)
        || (eachAlternative && strings.size() < enumerator.numberOfAlternatives()))
        && enumerator.hasNext()) {
      final String next = enumerator.next();
      strings.add(next);
    }

    return new ArrayList<String>(strings);
  }

  private static final Enumerator createEnumerator(final ASTNode node) {
    return createEnumerator(node, false);
  }

  private static final Enumerator createEnumerator(final ASTNode node,
      final boolean eachAlternative) {
    if (node instanceof RegularExpression) {
      return new RegularExpressionEnumerator((RegularExpression) node, eachAlternative);
    } else if (node instanceof Sequence) {
      return new SequenceEnumerator((Sequence) node);
    } else if (node instanceof Group) {
      return new GroupEnumerator((Group) node);
    } else if (node instanceof Literal) {
      return new LiteralEnumerator((Literal) node);
    } else if (node instanceof Alternatives) {
      return new AlternativesEnumerator((Alternatives) node);
    } else {
      assert (false) : "unknown regex node: " + node;
      return null;
    }
  }

  private interface Enumerator extends Iterator<String> {

    public Enumerator clone();

    public void reset();

    public String peek();

  }

  private abstract static class AtomEnumerator implements Enumerator {

    public static final int UNBOUNDED = -1;

    protected Enumerator[] enumerators;

    protected int minCount;
    protected int maxCount;

    protected boolean consumed;

    protected AtomEnumerator(final Atom.Quantifier quantifier) {
      init(quantifier);
    }

    protected AtomEnumerator() {
      /* intentionally left blank -- required for cloning */
    }

    protected void init(final Atom.Quantifier quantifier) {
      init(
          ((quantifier == Atom.Quantifier.QUANT_NONE || quantifier == Atom.Quantifier.QUANT_PLUS)
              ? 1 : 0),
          ((quantifier == Atom.Quantifier.QUANT_STAR || quantifier == Atom.Quantifier.QUANT_PLUS)
              ? UNBOUNDED : 1));
    }

    protected void init(final int minCount, final int maxCount) {
      this.minCount = minCount;
      this.maxCount = maxCount;

      reset();
    }

    protected abstract Enumerator createBaseEnumerator();

    @Override
    public abstract AtomEnumerator clone();

    protected final void clone(final AtomEnumerator atomEnumerator) {
      final Enumerator[] enumeratorsClone = new Enumerator[this.enumerators.length];

      for (int index = 0; index < this.enumerators.length; ++index) {
        enumeratorsClone[index] = this.enumerators[index].clone();
      }

      atomEnumerator.enumerators = enumeratorsClone;

      atomEnumerator.minCount = this.minCount;
      atomEnumerator.maxCount = this.maxCount;

      atomEnumerator.consumed = this.consumed;
    }

    @Override
    public final void reset() {
      this.enumerators = new Enumerator[this.minCount];

      for (int index = 0; index < this.enumerators.length; ++index) {
        this.enumerators[index] = createBaseEnumerator();
      }

      this.consumed = false;
    }

    @Override
    public final String peek() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      final StringBuilder builder = new StringBuilder();

      for (final Enumerator enumerator : this.enumerators) {
        builder.append(enumerator.peek());
      }

      return builder.toString();
    }

    @Override
    public final String next() {
      final String next = peek();

      boolean foundNext = false;

      for (int pos = 0; pos < this.enumerators.length; ++pos) {
        this.enumerators[pos].next();

        if (this.enumerators[pos].hasNext()) {
          foundNext = true;
          break;
        } else {
          this.enumerators[pos].reset();
        }
      }

      if (!foundNext) {
        if (this.maxCount == UNBOUNDED || this.enumerators.length < this.maxCount) {
          this.enumerators = new Enumerator[this.enumerators.length + 1];

          for (int index = 0; index < this.enumerators.length; ++index) {
            this.enumerators[index] = createBaseEnumerator();
          }
        } else {
          this.consumed = true;
        }
      }

      return next;
    }

    @Override
    public final boolean hasNext() {
      return !this.consumed;
    }

  }

  private static final class LiteralEnumerator extends AtomEnumerator {

    private Literal literal;

    public LiteralEnumerator(final Literal literal) {
      super(literal.getQuantifier());

      this.literal = literal;
      reset();
    }

    private LiteralEnumerator() {
      /* intentionally left blank -- required for cloning */
    }

    @Override
    public final LiteralEnumerator clone() {
      // do not use the actual constructor, as this would call the super constructor...
      final LiteralEnumerator clone = new LiteralEnumerator();
      clone.literal = this.literal;

      super.clone(clone);

      return clone;
    }

    @Override
    protected final Enumerator createBaseEnumerator() {
      class LiteralBaseEnumerator implements Enumerator {

        private boolean consumed;

        public LiteralBaseEnumerator() {
          reset();
        }

        public LiteralBaseEnumerator(final boolean consumed) {
          this.consumed = consumed;
        }

        @Override
        public final LiteralBaseEnumerator clone() {
          return new LiteralBaseEnumerator(this.consumed);
        }

        @Override
        public final void reset() {
          this.consumed = false;
        }

        @Override
        public final String peek() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          return LiteralEnumerator.this.literal.getValue();
        }

        @Override
        public final String next() {
          final String next = peek();

          this.consumed = true;
          return next;
        }

        @Override
        public final boolean hasNext() {
          return !this.consumed;
        }

      }

      return new LiteralBaseEnumerator();
    }

  }

  private static final class GroupEnumerator extends AtomEnumerator {

    private final List<Character> characters;

    public GroupEnumerator(final Group group) {
      super(group.getQuantifier());

      // determine characters
      {
        final Set<Character> characterSet = new LinkedHashSet<>();

        for (final Range range : group.getRanges()) {
          if (range instanceof SingleCharacter) {
            final SingleCharacter singleCharacter = (SingleCharacter) range;
            characterSet.add(singleCharacter.getValue());
          } else if (range instanceof CharacterRange) {
            final CharacterRange characterRange = (CharacterRange) range;

            char lowerCharacter = characterRange.getLowerCharacter().getValue();
            char upperCharacter = characterRange.getUpperCharacter().getValue();

            if (lowerCharacter > upperCharacter) {
              final char temp = upperCharacter;
              upperCharacter = lowerCharacter;
              lowerCharacter = temp;
            }

            for (char character = lowerCharacter; character <= upperCharacter; ++character) {
              characterSet.add(character);
            }
          } else {
            assert (false) : "unknown range: " + range;
          }
        }

        if (group.isInverted()) {
          final List<Character> characters = new ArrayList<>();

          for (char character = ' '; character <= '~'; ++character) {
            if (!characterSet.contains(character)) {
              characters.add(character);
            }
          }

          this.characters = characters;
        } else {
          this.characters = new ArrayList<>(characterSet);
        }
      }

      reset();
    }

    private GroupEnumerator(final List<Character> characters) {
      this.characters = characters;
    }

    @Override
    public final Enumerator createBaseEnumerator() {
      class GroupBaseEnumerator implements Enumerator {

        private int index;

        public GroupBaseEnumerator() {
          this(0);
        }

        public GroupBaseEnumerator(final int index) {
          this.index = index;
        }

        @Override
        public final Enumerator clone() {
          return new GroupBaseEnumerator(this.index);
        }

        @Override
        public final void reset() {
          this.index = 0;
        }

        @Override
        public final String peek() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          return String.valueOf(GroupEnumerator.this.characters.get(this.index));
        }

        @Override
        public final String next() {
          final String next = peek();

          ++this.index;

          return next;
        }

        @Override
        public final boolean hasNext() {
          return this.index < GroupEnumerator.this.characters.size();
        }

      }

      return new GroupBaseEnumerator();
    }

    @Override
    public final GroupEnumerator clone() {
      final List<Character> charactersClone = new ArrayList<>(this.characters);

      final GroupEnumerator clone = new GroupEnumerator(charactersClone);
      super.clone(clone);

      return clone;
    }

  }

  private static final class SingleSequenceEnumerator implements Enumerator {

    private final Enumerator[] enumerators;
    private final int lowestIndex;

    private boolean consumed;

    public SingleSequenceEnumerator(final Enumerator[] enumerators, final int lowestIndex) {
      this(enumerators, lowestIndex, false);
    }

    public SingleSequenceEnumerator(final Enumerator[] enumerators, final int lowestIndex,
        final boolean consumed) {
      this.enumerators = enumerators;
      this.lowestIndex = lowestIndex;
      this.consumed = consumed;
    }

    public final List<SingleSequenceEnumerator> nextEnumerators() {
      final List<SingleSequenceEnumerator> nextEnumerators = new ArrayList<>();

      for (int index = this.lowestIndex; index < this.enumerators.length; ++index) {
        if (this.enumerators[index].hasNext()) {
          final Enumerator[] enumeratorsClone = cloneEnumerators();
          enumeratorsClone[index].next();

          if (enumeratorsClone[index].hasNext()) {
            final SingleSequenceEnumerator nextEnumerator =
                new SingleSequenceEnumerator(enumeratorsClone, index, false);

            nextEnumerators.add(nextEnumerator);
          }
        }
      }

      return nextEnumerators;
    }

    private final Enumerator[] cloneEnumerators() {
      final Enumerator[] enumeratorsClone = new Enumerator[this.enumerators.length];
      for (int index = 0; index < this.enumerators.length; ++index) {
        enumeratorsClone[index] = this.enumerators[index].clone();
      }

      return enumeratorsClone;
    }

    @Override
    public final SingleSequenceEnumerator clone() {
      final Enumerator[] enumeratorsClone = cloneEnumerators();
      return new SingleSequenceEnumerator(enumeratorsClone, this.lowestIndex, this.consumed);
    }

    @Override
    public final void reset() {
      throw new AssertionError("cannot reset a SingleSequenceEnumerator");
    }

    @Override
    public final String peek() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      final StringBuilder builder = new StringBuilder();

      for (final Enumerator enumerator : this.enumerators) {
        builder.append(enumerator.peek());
      }

      return builder.toString();
    }

    @Override
    public final String next() {
      throw new AssertionError("cannot advance a SingleSequenceEnumerator");
    }

    @Override
    public final boolean hasNext() {
      return !this.consumed;
    }

  }

  private static final class SequenceEnumerator implements Enumerator {

    private final Sequence sequence;

    private final List<SingleSequenceEnumerator> singleSequenceEnumerators;

    public SequenceEnumerator(final Sequence sequence) {
      this.sequence = sequence;
      this.singleSequenceEnumerators = new ArrayList<SingleSequenceEnumerator>();

      reset();
    }

    private SequenceEnumerator(final Sequence sequence,
        final List<SingleSequenceEnumerator> singleSequenceEnumerators) {
      this.sequence = sequence;
      this.singleSequenceEnumerators = singleSequenceEnumerators;
    }

    @Override
    public final SequenceEnumerator clone() {
      final List<SingleSequenceEnumerator> singleSequenceEnumeratorsClone = new ArrayList<>();

      for (final SingleSequenceEnumerator singleSequenceEnumerator
          : this.singleSequenceEnumerators) {
        singleSequenceEnumeratorsClone.add(singleSequenceEnumerator.clone());
      }

      return new SequenceEnumerator(this.sequence, singleSequenceEnumeratorsClone);
    }

    @Override
    public final void reset() {
      final List<Atom> elements = this.sequence.getElements();
      final Enumerator[] enumerators = new Enumerator[elements.size()];

      for (int index = 0; index < elements.size(); ++index) {
        enumerators[index] = createEnumerator(elements.get(index));
      }

      final SingleSequenceEnumerator singleSequenceEnumerator =
          new SingleSequenceEnumerator(enumerators, 0);
      assert (singleSequenceEnumerator.hasNext());

      this.singleSequenceEnumerators.clear();
      this.singleSequenceEnumerators.add(singleSequenceEnumerator);
    }

    private final String peek(final boolean advance) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      SingleSequenceEnumerator shortestEnumerator = null;
      String shortestSequence = null;

      for (final SingleSequenceEnumerator enumerator : this.singleSequenceEnumerators) {
        assert (enumerator.hasNext());
        final String sequence = enumerator.peek();

        if (shortestSequence == null || sequence.length() < shortestSequence.length()) {
          shortestSequence = sequence;
          shortestEnumerator = enumerator;
        }
      }

      assert (shortestSequence != null);
      assert (shortestEnumerator != null);

      if (advance) {
        this.singleSequenceEnumerators.remove(shortestEnumerator);
        this.singleSequenceEnumerators.addAll(shortestEnumerator.nextEnumerators());
      }

      return shortestSequence;
    }

    @Override
    public final String peek() {
      return peek(false);
    }

    @Override
    public final String next() {
      return peek(true);
    }

    @Override
    public final boolean hasNext() {
      return !this.singleSequenceEnumerators.isEmpty();
    }

  }

  private static final class AlternativesBaseEnumerator implements Enumerator {

    private final Enumerator[] alternativeEnumerators;

    public AlternativesBaseEnumerator(final Alternatives alternatives) {
      this.alternativeEnumerators = new Enumerator[alternatives.getNumberOfAlternatives()];

      int index = 0;
      for (final Sequence alternative : alternatives) {
        this.alternativeEnumerators[index] = createEnumerator(alternative);
        ++index;
      }
    }

    public AlternativesBaseEnumerator(final Enumerator[] alternativeEnumerators) {
      this.alternativeEnumerators = alternativeEnumerators;
    }

    @Override
    public final AlternativesBaseEnumerator clone() {
      final Enumerator[] alternativeEnumeratorsClone =
          new Enumerator[this.alternativeEnumerators.length];

      for (int index = 0; index < this.alternativeEnumerators.length; ++index) {
        alternativeEnumeratorsClone[index] = this.alternativeEnumerators[index].clone();
      }

      return new AlternativesBaseEnumerator(alternativeEnumeratorsClone);
    }

    @Override
    public final void reset() {
      for (final Enumerator enumerator : this.alternativeEnumerators) {
        enumerator.reset();
      }
    }

    private final String peek(final boolean advance) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      int shortestAlternativeIndex = -1;
      String shortestAlternative = null;

      for (int index = 0; index < this.alternativeEnumerators.length; ++index) {
        if (this.alternativeEnumerators[index].hasNext()) {
          final String alternative = this.alternativeEnumerators[index].peek();

          if (shortestAlternative == null
              || alternative.length() < shortestAlternative.length()) {
            shortestAlternative = alternative;
            shortestAlternativeIndex = index;
          }
        }
      }

      if (advance) {
        assert (shortestAlternativeIndex != -1);
        this.alternativeEnumerators[shortestAlternativeIndex].next();
      }

      assert (shortestAlternative != null);
      return shortestAlternative;
    }

    @Override
    public final String peek() {
      return peek(false);
    }

    @Override
    public final String next() {
      return peek(true);
    }

    @Override
    public final boolean hasNext() {
      for (final Enumerator enumerator : this.alternativeEnumerators) {
        if (enumerator.hasNext()) {
          return true;
        }
      }

      return false;
    }

  }

  private static class AlternativesEnumerator extends AtomEnumerator {

    private Alternatives alternatives;

    public AlternativesEnumerator(final Alternatives alternatives) {
      this.alternatives = alternatives;
      init(alternatives.getQuantifier());
    }

    private AlternativesEnumerator() {
      /* intentionally left blank -- required for cloning */
    }

    @Override
    protected final Enumerator createBaseEnumerator() {
      return new AlternativesBaseEnumerator(this.alternatives);
    }

    @Override
    public final AlternativesEnumerator clone() {
      // do not use the actual constructor, as this would call the super constructor...
      final AlternativesEnumerator clone = new AlternativesEnumerator();
      clone.alternatives = this.alternatives;

      super.clone(clone);

      return clone;
    }

  }

  private static final class RegularExpressionEnumerator implements Enumerator {

    private final RegularExpression regularExpression;
    private final boolean eachAlternative;

    private int alternativeIndex;

    private Enumerator[] topLevelAlternativeEnumerators;
    private AlternativesBaseEnumerator alternativesEnumerator;

    public RegularExpressionEnumerator(final RegularExpression regularExpression,
        final boolean eachAlternative) {
      assert (regularExpression.getAlternatives().getQuantifier() == Atom.Quantifier.QUANT_NONE);

      this.regularExpression = regularExpression;
      this.eachAlternative = eachAlternative;

      reset();
    }

    private RegularExpressionEnumerator(final RegularExpression regularExpression,
        final boolean eachAlternative, final int alternativeIndex,
        final Enumerator[] topLevelAlternativeEnumerators,
        final AlternativesBaseEnumerator alternativesEnumerator) {
      this.regularExpression = regularExpression;
      this.eachAlternative = eachAlternative;
      this.alternativeIndex = alternativeIndex;
      this.topLevelAlternativeEnumerators = topLevelAlternativeEnumerators;
      this.alternativesEnumerator = alternativesEnumerator;
    }

    public final int numberOfAlternatives() {
      return this.topLevelAlternativeEnumerators.length;
    }

    @Override
    public final RegularExpressionEnumerator clone() {
      final Enumerator[] topLevelAlternativeEnumeratorsClone =
          new Enumerator[this.topLevelAlternativeEnumerators.length];

      for (int index = 0; index < this.topLevelAlternativeEnumerators.length; ++index) {
        topLevelAlternativeEnumeratorsClone[index] =
            this.topLevelAlternativeEnumerators[index].clone();
      }

      final AlternativesBaseEnumerator alternativesEnumeratorClone;

      if (this.alternativesEnumerator == null) {
        alternativesEnumeratorClone = null;
      } else {
        alternativesEnumeratorClone = this.alternativesEnumerator.clone();
      }

      return new RegularExpressionEnumerator(this.regularExpression, this.eachAlternative,
          this.alternativeIndex, topLevelAlternativeEnumeratorsClone, alternativesEnumeratorClone);
    }

    @Override
    public final void reset() {
      this.alternativeIndex = 0;

      final List<Sequence> alternatives =
          this.regularExpression.getAlternatives().getAlternatives();
      this.topLevelAlternativeEnumerators = new Enumerator[alternatives.size()];

      for (int index = 0; index < this.topLevelAlternativeEnumerators.length; ++index) {
        final Sequence alternative = alternatives.get(index);
        this.topLevelAlternativeEnumerators[index] = createEnumerator(alternative);
      }

      if (this.eachAlternative) {
        this.alternativesEnumerator = null;
      } else {
        createAlternativesEnumerator();
      }
    }

    private final void createAlternativesEnumerator() {
      final List<Enumerator> alternativesWithAlternatives = new ArrayList<>();

      for (final Enumerator enumerator : this.topLevelAlternativeEnumerators) {
        if (enumerator.hasNext()) {
          alternativesWithAlternatives.add(enumerator);
        }
      }

      this.alternativesEnumerator =
          new AlternativesBaseEnumerator(alternativesWithAlternatives.toArray(new Enumerator[0]));
    }

    @Override
    public final String peek() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      if (this.eachAlternative
          && this.alternativeIndex < this.topLevelAlternativeEnumerators.length) {
        return this.topLevelAlternativeEnumerators[this.alternativeIndex].peek();
      } else {
        assert (this.alternativesEnumerator != null);
        assert (this.alternativesEnumerator.hasNext());

        return this.alternativesEnumerator.peek();
      }
    }

    @Override
    public final String next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      if (this.eachAlternative
          && this.alternativeIndex < this.topLevelAlternativeEnumerators.length) {
        final String next = this.topLevelAlternativeEnumerators[this.alternativeIndex].next();

        ++this.alternativeIndex;

        if (this.alternativeIndex >= this.topLevelAlternativeEnumerators.length) {
          createAlternativesEnumerator();
        }

        return next;
      } else {
        assert (this.alternativesEnumerator != null);
        assert (this.alternativesEnumerator.hasNext());

        return this.alternativesEnumerator.next();
      }
    }

    @Override
    public final boolean hasNext() {
      return (this.eachAlternative
          && this.alternativeIndex < this.topLevelAlternativeEnumerators.length)
          || (this.alternativesEnumerator != null && this.alternativesEnumerator.hasNext());
    }

  }

}
