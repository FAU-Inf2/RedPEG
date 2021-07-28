package i2.act.reduction.hdd;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.util.TokenJoiner;

import java.util.*;

public final class HDDrReducer extends HDDVariant {

  public static enum Traversal {
    DEPTH_FIRST, BREADTH_FIRST;
  }

  public static enum Append {
    FORWARD, BACKWARD;
  }

  public static final Traversal DEFAULT_TRAVERSAL = Traversal.DEPTH_FIRST;
  public static final Append DEFAULT_APPEND = Append.BACKWARD;

  public static final boolean DEFAULT_SKIP_TERMINAL_NODES = false;
  public static final boolean DEFAULT_SKIP_TERMINAL_TREES = false;
  public static final boolean DEFAULT_HIDE_UNREMOVABLE = true;

  // ===============================================================================================

  // ~~~ HDDr ~~~

  public static final HDDrReducer createHDDrReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return createHDDrReducer(listReductionFactory, joiner, replacements,
        DEFAULT_TRAVERSAL, DEFAULT_APPEND);
  }

  public static final HDDrReducer createHDDrReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements,
      final Traversal traversal, final Append append) {
    return new HDDrReducer(listReductionFactory, joiner, traversal, append, false, replacements);
  }

  public static final HDDrReducer createHDDrFixpointReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements) {
    return createHDDrFixpointReducer(listReductionFactory, joiner, replacements,
        DEFAULT_TRAVERSAL, DEFAULT_APPEND);
  }

  public static final HDDrReducer createHDDrFixpointReducer(
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Map<Symbol<?>, List<Token>> replacements,
      final Traversal traversal, final Append append) {
    return new HDDrReducer(listReductionFactory, joiner, traversal, append, true, replacements);
  }

  // ===============================================================================================

  private final Traversal traversal;
  private final Append append;

  private final boolean fixpoint;

  public HDDrReducer(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Traversal traversal, final Append append, final boolean fixpoint) {
    this(listReductionFactory, joiner, traversal, append, fixpoint, null,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES, DEFAULT_HIDE_UNREMOVABLE);
  }

  public HDDrReducer(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Traversal traversal, final Append append, final boolean fixpoint,
      final Map<Symbol<?>, List<Token>> replacements) {
    this(listReductionFactory, joiner, traversal, append, fixpoint, replacements,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES, DEFAULT_HIDE_UNREMOVABLE);
  }

  public HDDrReducer(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final Traversal traversal, final Append append, final boolean fixpoint,
      final Map<Symbol<?>, List<Token>> replacements, final boolean skipTerminalNodes,
      final boolean skipTerminalTrees, final boolean hideUnremovable) {
    super(listReductionFactory, joiner, skipTerminalNodes, skipTerminalTrees, hideUnremovable,
        replacements);

    this.traversal = traversal;
    this.append = append;
    this.fixpoint = fixpoint;
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
    final LinkedList<Node<?>> queue = new LinkedList<>();
    queue.add(syntaxTree);

    while (!queue.isEmpty()) {
      final Node<?> currentNode = pop(queue);
      final List<Node<?>> nodes = getNodes(currentNode, 1, removedNodes);

      if (nodes.isEmpty()) {
        continue;
      }

      final boolean keepOnePlusQuantified = false;
      final List<Node<?>> keptNodesLevel =
          reduceList(syntaxTree, run, nodes, removedNodes, keepOnePlusQuantified);

      removedNodes.addAll(nodes);
      removedNodes.removeAll(keptNodesLevel);

      append(queue, keptNodesLevel);
    }

    run.finishIteration();
  }

  private final Node<?> pop(final LinkedList<Node<?>> queue) {
    assert (!queue.isEmpty());

    return queue.removeFirst();
  }

  private static final List<Node<?>> reverse(final Collection<Node<?>> nodes) {
    final List<Node<?>> reversed = new LinkedList<>();

    for (final Node<?> node : nodes) {
      reversed.add(0, node);
    }

    return reversed;
  }

  private final void append(final LinkedList<Node<?>> queue, final Collection<Node<?>> nodes) {
    if (this.append == Append.FORWARD) {
      if (this.traversal == Traversal.DEPTH_FIRST) {
        queue.addAll(0, nodes);
      } else {
        assert (this.traversal == Traversal.BREADTH_FIRST);
        queue.addAll(nodes);
      }
    } else {
      assert (this.append == Append.BACKWARD);
      if (this.traversal == Traversal.DEPTH_FIRST) {
        queue.addAll(0, reverse(nodes));
      } else {
        assert (this.traversal == Traversal.BREADTH_FIRST);
        queue.addAll(reverse(nodes));
      }
    }
  }

}
