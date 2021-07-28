package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge.Quantifier;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.grammargraph.properties.PropertyComputation;
import i2.act.grammargraph.properties.subtree.SubTree;
import i2.act.grammargraph.properties.subtree.SubTreeSequence;
import i2.act.packrat.Token;
import i2.act.peg.ast.RegularExpression;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.regex.SmallStringGenerator;
import i2.act.reduction.util.TokenJoiner;
import i2.act.util.Pair;

import java.util.*;

public final class MinTreeComputation extends PropertyComputation<List<SubTreeSequence>> {

  public static Map<Symbol<?>, List<SubTreeSequence>> computeMinTrees(
      final GrammarGraph grammarGraph) {
    final MinTreeComputation computation = new MinTreeComputation();
    return computation.filter(computation.compute(grammarGraph));
  }

  public static final Map<Symbol<?>, List<Token>> computeMinTokenSequences(
      final GrammarGraph grammarGraph, final TokenJoiner joiner) {
    final Map<Symbol<?>, List<Token>> replacements = computeMinTokens(grammarGraph);
    return computeMinTokenSequences(grammarGraph, joiner, replacements);
  }

  public static final Map<Symbol<?>, List<Token>> computeMinTokenSequences(
      final GrammarGraph grammarGraph, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    final Map<Symbol<?>, List<Token>> minTokenSequences = new HashMap<>();

    final Map<Symbol<?>, List<SubTreeSequence>> minTrees = computeMinTrees(grammarGraph);

    for (final Map.Entry<Symbol<?>, List<SubTreeSequence>> entry : minTrees.entrySet()) {
      final Symbol<?> symbol = entry.getKey();
      final List<SubTreeSequence> subTreeSequences = entry.getValue();

      List<Token> shortestMinTokenSequence = null;
      int shortestMinTokenSequenceSize = -1;

      for (final SubTreeSequence subTreeSequence : subTreeSequences) {
        final List<Token> minTokenSequence = subTreeSequence.toTokens(replacements);
        final int minTokenSequenceSize = joiner.join(minTokenSequence).length();

        if (shortestMinTokenSequence == null
            || minTokenSequenceSize < shortestMinTokenSequenceSize) {
          shortestMinTokenSequence = minTokenSequence;
          shortestMinTokenSequenceSize = minTokenSequenceSize;
        }
      }

      assert (shortestMinTokenSequence != null);

      minTokenSequences.put(symbol, shortestMinTokenSequence);
    }

    return minTokenSequences;
  }

  public static final Map<Symbol<?>, List<Token>> computeMinTokens(
      final GrammarGraph grammarGraph) {
    final Map<Symbol<?>, List<Token>> minTokens = new HashMap<>();

    for (final LexerSymbol lexerSymbol : grammarGraph.gatherLexerSymbols()) {
      final String minString;
      {
        if (lexerSymbol.getProduction() == null) {
          minString = "";
        } else {
          final RegularExpression regularExpression =
              lexerSymbol.getProduction().getRegularExpression();
          assert (regularExpression != null);

          minString = SmallStringGenerator.generateString(regularExpression);
        }
      }

      final Token minToken = new Token(lexerSymbol, minString);
      final List<Token> minTokenSequence = Arrays.asList(minToken);

      minTokens.put(lexerSymbol, minTokenSequence);
    }

    return minTokens;
  }


  // ===============================================================================================


  private MinTreeComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  private static final List<SubTreeSequence> listOf(final SubTreeSequence minTreeSequence) {
    return Arrays.asList(minTreeSequence);
  }

  @Override
  protected final List<SubTreeSequence> init(final AlternativeNode node,
      final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      assert (node.getGrammarSymbol() instanceof LexerSymbol);
      final LexerSymbol symbol = (LexerSymbol) node.getGrammarSymbol();

      return listOf(new SubTreeSequence(SubTree.leafNode(symbol)));
    } else {
      return null;
    }
  }

  @Override
  protected final List<SubTreeSequence> init(final SequenceNode node,
      final GrammarGraph grammarGraph) {
    if (node.numberOfSuccessors() == 0) {
      return listOf(new SubTreeSequence());
    } else {
      return null;
    }
  }

  @Override
  protected final List<SubTreeSequence> transfer(final AlternativeNode node,
      final List<SubTreeSequence> in) {
    if (in == null || !node.hasGrammarSymbol()) {
      return in;
    }

    assert (node.getGrammarSymbol() instanceof ParserSymbol);
    final ParserSymbol symbol = (ParserSymbol) node.getGrammarSymbol();

    final List<SubTreeSequence> result = new ArrayList<>();

    for (final SubTreeSequence minTreeSequence : in) {
      assert (minTreeSequence != null);

      final SubTree newMinTree = SubTree.innerNode(symbol, minTreeSequence.getSubTrees());
      final SubTreeSequence newMinTreeSequence = new SubTreeSequence(newMinTree);

      result.add(newMinTreeSequence);
    }

    return result;
  }

  @Override
  protected final List<SubTreeSequence> transfer(final SequenceNode node,
      final List<SubTreeSequence> in) {
    return in;
  }

  @Override
  protected final List<SubTreeSequence> confluence(final AlternativeNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>>> inSets) {
    int minSize = -1;
    final List<SubTreeSequence> newMinTreeSequences = new ArrayList<>();

    for (final Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>> inSet : inSets) {
      final List<SubTreeSequence> minTreeSequences = inSet.getSecond();

      if (minTreeSequences == null) {
        continue;
      }

      for (final SubTreeSequence minTreeSequence : minTreeSequences) {
        final int size = minTreeSequence.numberOfTerminals();
        if (minSize == -1 || size <= minSize) {
          if (size != minSize) {
            minSize = size;
            newMinTreeSequences.clear();
          }

          newMinTreeSequences.add(minTreeSequence);
        }
      }
    }

    if (newMinTreeSequences.isEmpty()) {
      return null;
    }

    return newMinTreeSequences;
  }

  @Override
  protected final List<SubTreeSequence> confluence(final SequenceNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>>> inSets) {
    final List<List<SubTreeSequence>> filteredInSets = new ArrayList<>();
    {
      for (final Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>> inSet : inSets) {
        final GrammarGraphEdge<?, ?> edge = inSet.getFirst();
        final List<SubTreeSequence> minTreeSequences = inSet.getSecond();

        assert (edge instanceof SequenceEdge);
        final Quantifier quantifier = ((SequenceEdge) edge).getQuantifier();

        if (quantifier == Quantifier.QUANT_NONE || quantifier == Quantifier.QUANT_PLUS) {
          if (minTreeSequences == null) {
            return null;
          }

          filteredInSets.add(minTreeSequences);
        }
      }
    }

    if (filteredInSets.isEmpty()) {
      return listOf(new SubTreeSequence());
    } else {
      final List<SubTreeSequence> combinations = combinations(filteredInSets, 0);
      return combinations;
    }
  }

  private final List<SubTreeSequence> combinations(final List<List<SubTreeSequence>> inSets,
      final int index) {
    final List<SubTreeSequence> current = inSets.get(index);

    if (index == inSets.size() - 1) {
      return current;
    } else {
      final List<SubTreeSequence> combinations = new ArrayList<>();

      final List<SubTreeSequence> recursiveResult = combinations(inSets, index + 1);

      for (final SubTreeSequence minTreeSequence : current) {
        for (final SubTreeSequence recursiveMinTreeSequence : recursiveResult) {
          final SubTreeSequence newMinTreeSequence =
              new SubTreeSequence(minTreeSequence, recursiveMinTreeSequence);
          combinations.add(newMinTreeSequence);
        }
      }

      return combinations;
    }
  }

}
