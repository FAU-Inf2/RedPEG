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
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PossibleSubTreesComputation extends PropertyComputation<List<SubTreeSequence>> {

  public static final Map<Symbol<?>, List<SubTreeSequence>> computePossibleSubTrees(
      final GrammarGraph grammarGraph) {
    final PossibleSubTreesComputation computation = new PossibleSubTreesComputation();
    return computation.filter(computation.compute(grammarGraph));
  }

  // ===============================================================================================

  private PossibleSubTreesComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  @Override
  protected final List<SubTreeSequence> init(final AlternativeNode node,
      final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      assert (node.getGrammarSymbol() instanceof LexerSymbol);
      final LexerSymbol symbol = (LexerSymbol) node.getGrammarSymbol();

      return Arrays.asList(new SubTreeSequence(SubTree.leafNode(symbol)));
    } else {
      return null;
    }
  }

  @Override
  protected final List<SubTreeSequence> init(final SequenceNode node,
      final GrammarGraph grammarGraph) {
    if (node.numberOfSuccessors() == 0) {
      return Arrays.asList(new SubTreeSequence());
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

    final Set<SubTreeSequence> resultSet = new LinkedHashSet<>();

    for (final SubTreeSequence subTreeSequence : in) {
      assert (subTreeSequence != null);

      if (subTreeSequence.isEmptySequence()) {
        resultSet.add(subTreeSequence);
      } else {
        final List<SubTree> children = new ArrayList<>();
        {
          for (final SubTree subTree : subTreeSequence.getSubTrees()) {
            final SubTree childTree = getChildSubTree(subTree);
            children.add(childTree);
          }
        }

        final SubTree newSubTree = SubTree.innerNode(symbol, children);
        final SubTreeSequence newSubTreeSequence = new SubTreeSequence(newSubTree);

        resultSet.add(newSubTreeSequence);
      }
    }

    return new ArrayList<>(resultSet);
  }

  @Override
  protected final List<SubTreeSequence> transfer(final SequenceNode node,
      final List<SubTreeSequence> in) {
    return in;
  }

  @Override
  protected final List<SubTreeSequence> confluence(final AlternativeNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>>> inSets) {
    final List<SubTreeSequence> possibleSubTreeSequences = new ArrayList<>();

    for (final Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>> inSet : inSets) {
      final List<SubTreeSequence> subTreeSequences = inSet.getSecond();

      if (subTreeSequences == null) {
        continue;
      }

      possibleSubTreeSequences.addAll(subTreeSequences);
    }

    return possibleSubTreeSequences;
  }

  @Override
  protected final List<SubTreeSequence> confluence(final SequenceNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>>> inSets) {
    final List<List<SubTreeSequence>> preprocessedInSets = new ArrayList<>();
    {
      for (final Pair<GrammarGraphEdge<?, ?>, List<SubTreeSequence>> inSet : inSets) {
        final GrammarGraphEdge<?, ?> edge = inSet.getFirst();
        final List<SubTreeSequence> subTreeSequences = inSet.getSecond();

        if (subTreeSequences == null) {
          return null;
        }

        assert (edge instanceof SequenceEdge);
        final Quantifier quantifier = ((SequenceEdge) edge).getQuantifier();

        if (quantifier == Quantifier.QUANT_NONE) {
          preprocessedInSets.add(subTreeSequences);
        } else {
          final List<SubTreeSequence> quantifierSequences = new ArrayList<>();
          {
            for (final SubTreeSequence subTreeSequence : subTreeSequences) {
              final Symbol<?> quantifierSymbol = getQuantifierSymbol(quantifier);

              final SubTree quantifierSubTree =
                  SubTree.innerNode(quantifierSymbol, subTreeSequence.getSubTrees());
              final SubTreeSequence quantifierSubTreeSequence =
                  new SubTreeSequence(quantifierSubTree);

              quantifierSequences.add(quantifierSubTreeSequence);
            }
          }

          preprocessedInSets.add(quantifierSequences);
        }
      }
    }

    if (preprocessedInSets.isEmpty()) {
      return Arrays.asList(new SubTreeSequence());
    } else {
      final List<SubTreeSequence> combinations = combinations(preprocessedInSets, 0);
      return combinations;
    }
  }

  private final SubTree getChildSubTree(final SubTree subTree) {
    final Symbol<?> symbol = subTree.getSymbol();

    if (isQuantifierSymbol(symbol)) {
      final List<SubTree> children = new ArrayList<>();

      for (final SubTree originalChildTree : subTree.getChildren()) {
        final SubTree newChildTree = getChildSubTree(originalChildTree);
        children.add(newChildTree);
      }

      return SubTree.innerNode(symbol, children);
    } else {
      return SubTree.leafNode(symbol);
    }
  }

  private final boolean isQuantifierSymbol(final Symbol<?> symbol) {
    return symbol == ParserSymbol.OPTIONAL
        || symbol == ParserSymbol.STAR
        || symbol == ParserSymbol.PLUS;
  }

  private final Symbol<?> getQuantifierSymbol(final Quantifier quantifier) {
    switch (quantifier) {
      case QUANT_OPTIONAL: {
        return ParserSymbol.OPTIONAL;
      }
      case QUANT_STAR: {
        return ParserSymbol.STAR;
      }
      case QUANT_PLUS: {
        return ParserSymbol.PLUS;
      }
      default: {
        assert (false) : quantifier;
        return null;
      }
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

      for (final SubTreeSequence subTreeSequence : current) {
        for (final SubTreeSequence recursiveSubTreeSequence : recursiveResult) {
          final SubTreeSequence newSubTreeSequence =
              new SubTreeSequence(subTreeSequence, recursiveSubTreeSequence);
          combinations.add(newSubTreeSequence);
        }
      }

      return combinations;
    }
  }

}
