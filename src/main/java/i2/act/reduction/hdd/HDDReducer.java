package i2.act.reduction.hdd;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.util.TokenJoiner;

import java.util.*;

public final class HDDReducer extends HDDVariant {

  public static final boolean DEFAULT_SKIP_TERMINAL_NODES = false;
  public static final boolean DEFAULT_SKIP_TERMINAL_TREES = false;
  public static final boolean DEFAULT_HIDE_UNREMOVABLE = true;

  public static final boolean COARSE_KEEP_ONE_PLUS_QUANTIFIED = true;

  // ===============================================================================================

  // ~~~ HDD ~~~

  public static final HDDReducer createHDDReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return new HDDReducer(listReductionFactory, joiner, false, false, replacements);
  }

  public static final HDDReducer createHDDFixpointReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return new HDDReducer(listReductionFactory, joiner, false, true, replacements);
  }

  // ~~~ BaseHDD ~~~

  public static final HDDReducer createBaseHDDReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new HDDReducer(listReductionFactory, joiner, false, false,
        new HashMap<Symbol<?>, List<Token>>(), false, false, false);
  }

  public static final HDDReducer createBaseHDDFixpointReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new HDDReducer(listReductionFactory, joiner, false, true,
        new HashMap<Symbol<?>, List<Token>>(), false, false, false);
  }

  // ~~~ CoarseHDD ~~~

  public static final HDDReducer createCoarseHDDReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return new HDDReducer(listReductionFactory, joiner, true, false, replacements);
  }

  public static final HDDReducer createCoarseHDDFixpointReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return new HDDReducer(listReductionFactory, joiner, true, true, replacements);
  }

  // ===============================================================================================

  private final boolean coarse;
  private final boolean fixpoint;

  public HDDReducer(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean coarse, final boolean fixpoint,
      final Map<Symbol<?>, List<Token>> replacements) {
    this(listReductionFactory, joiner, coarse, fixpoint, replacements,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES, DEFAULT_HIDE_UNREMOVABLE);
  }

  public HDDReducer(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean coarse, final boolean fixpoint, final Map<Symbol<?>, List<Token>> replacements,
      final boolean skipTerminalNodes, final boolean skipTerminalTrees,
      final boolean hideUnremovable) {
    super(listReductionFactory, joiner, skipTerminalNodes, skipTerminalTrees, hideUnremovable,
        replacements);
    this.coarse = coarse;
    this.fixpoint = fixpoint;
  }

  @Override
  public final String getName() {
    if (this.coarse) {
      return "CoarseHDD";
    } else {
      return "HDD";
    }
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    final Set<Node<?>> removedNodes = new HashSet<>();

    int sizeBeforeIteration;
    int sizeAfterIteration = syntaxTree.print().length();

    do {
      sizeBeforeIteration = sizeAfterIteration;

      reductionIteration(syntaxTree, removedNodes, run);

      sizeAfterIteration = serialize(syntaxTree, removedNodes).length();
    } while (this.fixpoint && sizeAfterIteration < sizeBeforeIteration);

    return serialize(syntaxTree, removedNodes);
  }

  private final void reductionIteration(final Node<?> syntaxTree, final Set<Node<?>> removedNodes,
      final ReductionRun run) {
    List<Node<?>> nodes = getNodes(syntaxTree, 1, removedNodes);
    while (!nodes.isEmpty()) {
      final List<Node<?>> toReduce;
      {
        if (this.coarse) {
          toReduce = new ArrayList<Node<?>>();

          for (final Node<?> node : nodes) {
            if (hasEmptyReplacement(node)) {
              toReduce.add(node);
            }
          }
        } else {
          toReduce = nodes;
        }
      }

      if (!toReduce.isEmpty()) {
        final boolean keepOnePlusQuantified = COARSE_KEEP_ONE_PLUS_QUANTIFIED && this.coarse;

        final List<Node<?>> keptNodesLevel =
            reduceList(syntaxTree, run, toReduce, removedNodes, keepOnePlusQuantified);

        removedNodes.addAll(toReduce);
        removedNodes.removeAll(keptNodesLevel);
      }

      final List<Node<?>> newNodes = new ArrayList<>();
      {
        for (final Node<?> node : nodes) {
          newNodes.addAll(getNodes(node, 1, removedNodes));
        }
      }

      nodes = newNodes;
    }

    run.finishIteration();
  }

  private final boolean hasEmptyReplacement(final Node<?> node) {
    if (node.getSymbol() == ParserSymbol.LIST_ITEM) {
      return true;
    }

    return getReplacementText(node).isEmpty();
  }

}
