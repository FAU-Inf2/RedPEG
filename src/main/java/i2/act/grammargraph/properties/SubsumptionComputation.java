package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
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
  protected final Set<Symbol<?>> init(final Choice node, final GrammarGraph grammarGraph) {
    final Set<Symbol<?>> init = new HashSet<>();

    if (node.hasGrammarSymbol()) {
      init.add(node.getGrammarSymbol());
    }

    return init;
  }

  @Override
  protected final Set<Symbol<?>> init(final Sequence node, final GrammarGraph grammarGraph) {
    final Set<Symbol<?>> init = new HashSet<>();

    if (allElementsOptional(node)) {
      init.add(EMPTY);
    }

    return init;
  }

  @Override
  protected final Set<Symbol<?>> transfer(final Choice node, final Set<Symbol<?>> in) {
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
  protected final Set<Symbol<?>> transfer(final Sequence node, final Set<Symbol<?>> in) {
    return in;
  }

  @Override
  protected final Set<Symbol<?>> confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSets) {
    final Set<Symbol<?>> confluence = new HashSet<>();

    for (final Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>> inPair : inSets) {
      final Set<Symbol<?>> inSet = inPair.getSecond();
      confluence.addAll(inSet);
    }

    return confluence;
  }

  @Override
  protected final Set<Symbol<?>> confluence(final Sequence node,
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

  private static final boolean allElementsOptional(final Sequence node) {
    for (final Element successorEdge : node.getSuccessorEdges()) {
      if (!isOptionalEdge(successorEdge)) {
        return false;
      }
    }

    return true;
  }

  private static final boolean isOptionalEdge(final Element edge) {
    final Quantifier quantifier = edge.getQuantifier();
    return quantifier == Quantifier.QUANT_OPTIONAL
        || quantifier == Quantifier.QUANT_STAR;
  }

  private static final boolean allOtherElementsNullable(
      final List<Pair<GrammarGraphEdge<?, ?>, Set<Symbol<?>>>> inSets, final int index) {
    for (int otherIndex = 0; otherIndex < inSets.size(); ++otherIndex) {
      if (index == otherIndex) {
        continue;
      }

      assert (inSets.get(otherIndex).getFirst() instanceof Element);
      final Element edge = (Element) inSets.get(otherIndex).getFirst();
      final Set<Symbol<?>> inSet = inSets.get(otherIndex).getSecond();

      if (!isOptionalEdge(edge) && !inSet.contains(EMPTY)) {
        return false;
      }
    }

    return true;
  }

}
