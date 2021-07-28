package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.grammargraph.properties.PropertyComputation;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SubsumptionComputation extends PropertyComputation<Set<Symbol<?>>> {

  public static final Map<Symbol<?>, Set<Symbol<?>>> computeSubsumption(final Grammar grammar) {
    return computeSubsumption(GrammarGraph.fromGrammar(grammar));
  }

  public static final Map<Symbol<?>, Set<Symbol<?>>> computeSubsumption(
      final GrammarGraph grammarGraph) {
    final SubsumptionComputation computation = new SubsumptionComputation();
    return computation.filter(computation.compute(grammarGraph));
  }

  // -----------------------------------------------------------------------------------------------

  public static final Symbol<?> EMPTY = new ParserSymbol("EMPTY");

  public SubsumptionComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  @Override
  protected final Set<Symbol<?>> init(final AlternativeNode node, final GrammarGraph grammarGraph) {
    final Set<Symbol<?>> init = new HashSet<>();

    if (node.hasGrammarSymbol()) {
      init.add(node.getGrammarSymbol());
    }

    return init;
  }

  @Override
  protected final Set<Symbol<?>> init(final SequenceNode node, final GrammarGraph grammarGraph) {
    final Set<Symbol<?>> init = new HashSet<>();

    if (allElementsOptional(node)) {
      init.add(EMPTY);
    }

    return init;
  }

  @Override
  protected final Set<Symbol<?>> transfer(final AlternativeNode node, final Set<Symbol<?>> in) {
    final Set<Symbol<?>> out;

    if (!node.hasGrammarSymbol() || in.contains(node.getGrammarSymbol())) {
      out = in;
    } else {
      out = new HashSet<Symbol<?>>(in);
      out.add(node.getGrammarSymbol());
    }

    return out;
  }

  @Override
  protected final Set<Symbol<?>> transfer(final SequenceNode node, final Set<Symbol<?>> in) {
    return in;
  }

  @Override
  protected final Set<Symbol<?>> confluence(final AlternativeNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSets) {
    final Set<Symbol<?>> confluence = new HashSet<>();

    for (final Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>> inPair : inSets) {
      final Set<Symbol<?>> inSet = inPair.getSecond();
      confluence.addAll(inSet);
    }

    return confluence;
  }

  @Override
  protected final Set<Symbol<?>> confluence(final SequenceNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSets) {
    final Set<Symbol<?>> confluence = new HashSet<>();

    final List<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSetsList = new ArrayList<>();
    inSets.forEach(inSetsList::add);

    for (int index = 0; index < inSetsList.size(); ++index) {
      if (allOtherElementsNullable(inSetsList, index)) {
        final Set<Symbol<?>> inSet = inSetsList.get(index).getSecond();
        confluence.addAll(inSet);
      }
    }

    if (allElementsOptional(node)) {
      confluence.add(EMPTY);
    }

    return confluence;
  }

  private static final boolean allElementsOptional(final SequenceNode node) {
    for (final SequenceEdge successorEdge : node.getSuccessorEdges()) {
      if (!isOptionalEdge(successorEdge)) {
        return false;
      }
    }

    return true;
  }

  private static final boolean isOptionalEdge(final SequenceEdge edge) {
    final SequenceEdge.Quantifier quantifier = edge.getQuantifier();
    return quantifier == SequenceEdge.Quantifier.QUANT_OPTIONAL
        || quantifier == SequenceEdge.Quantifier.QUANT_STAR;
  }

  private static final boolean allOtherElementsNullable(
      final List<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSets, final int index) {
    for (int otherIndex = 0; otherIndex < inSets.size(); ++otherIndex) {
      if (index == otherIndex) {
        continue;
      }

      assert (inSets.get(otherIndex).getFirst() instanceof SequenceEdge);
      final SequenceEdge edge = (SequenceEdge) inSets.get(otherIndex).getFirst();
      final Set<Symbol<?>> inSet = inSets.get(otherIndex).getSecond();

      if (!isOptionalEdge(edge) && !inSet.contains(EMPTY)) {
        return false;
      }
    }

    return true;
  }

}
