package i2.act.reduction.pardis;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.Reducer;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.dd.OPDD;
import i2.act.reduction.lists.ListReduction;
import i2.act.reduction.lists.ListReductionCallback;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.util.TokenJoiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public final class PardisReducer implements Reducer {

  public static final boolean LIST_DELETION = true;
  public static final boolean NULLABILITY_PRUNING = true;

  // ===============================================================================================

  // ~~~ Pardis ~~~

  public static final PardisReducer createPardisReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPardisReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPardisReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PardisPriority, false, listReductionFactory, joiner, false);
  }

  public static final PardisReducer createPardisFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPardisFixpointReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPardisFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PardisPriority, false, listReductionFactory, joiner, true);
  }

  // ~~~ Pardis Hybrid ~~~

  public static final PardisReducer createPardisHybridReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPardisHybridReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPardisHybridReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PardisHybridPriority, true, listReductionFactory, joiner, false);
  }

  public static final PardisReducer createPardisHybridFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPardisHybridFixpointReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPardisHybridFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PardisHybridPriority, true, listReductionFactory, joiner, true);
  }

  // ~~~ Perses N ~~~

  public static final PardisReducer createPersesNReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPersesNReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPersesNReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PersesPriority, false, listReductionFactory, joiner, false);
  }

  public static final PardisReducer createPersesNFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPersesNFixpointReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPersesNFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PersesPriority, false, listReductionFactory, joiner, true);
  }

  // ~~~ Perses {DD, OPDD} ~~~

  public static final PardisReducer createPersesReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPersesReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPersesReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PersesPriority, true, listReductionFactory, joiner, false);
  }

  public static final PardisReducer createPersesFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createPersesFixpointReducer(grammar, null, joiner);
  }

  public static final PardisReducer createPersesFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PardisReducer(PersesPriority, true, listReductionFactory, joiner, true);
  }


  // ===============================================================================================

  public static interface Priority {

    public abstract int[] getPriorityVector(final Node<?> node,
        final Map<Node<?>, Integer> bfsOrder);

  }

  public static final Priority PardisPriority =
      (node, bfsOrder) -> new int[] {node.numberOfTerminals(), bfsOrder.get(node)};

  public static final Priority PardisHybridPriority =
      (node, bfsOrder) -> {
        final int parentOrder =
            (node.getParent() == null) ? Integer.MAX_VALUE : bfsOrder.get(node.getParent());
        return new int[] {node.numberOfTerminals(), parentOrder, bfsOrder.get(node)};
      };

  public static final Priority PersesPriority =
      (node, bfsOrder) -> {
        final int parentWeight =
            (node.getParent() == null) ? Integer.MAX_VALUE : node.getParent().numberOfTerminals();
        final int parentOrder =
            (node.getParent() == null) ? Integer.MAX_VALUE : bfsOrder.get(node.getParent());
        // NOTE: In the original definition, the priority vector only consists of the first two
        // elements. However, to faithfully imitate Perses w/o replacements, we have to make sure
        // that we remove the nodes in the correct order.
        return new int[] {parentWeight, parentOrder, -bfsOrder.get(node)};
      };

  // ===============================================================================================

  private final Priority priority;
  private final boolean hybrid;

  private final ListReduction<Node<?>> listReduction;
  private final TokenJoiner joiner;

  private final boolean fixpoint;

  public PardisReducer(final Priority priority, final boolean hybrid,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean fixpoint) {
    this(priority, hybrid, createListReduction(listReductionFactory), joiner, fixpoint);
  }

  public PardisReducer(final Priority priority, final boolean hybrid,
      final ListReduction<Node<?>> listReduction, final TokenJoiner joiner,
      final boolean fixpoint) {
    this.priority = priority;
    this.hybrid = hybrid;
    this.listReduction = listReduction;
    this.joiner = joiner;
    this.fixpoint = fixpoint;
  }

  private static final ListReduction<Node<?>> createListReduction(
      final ListReductionFactory listReductionFactory) {
    if (listReductionFactory == null) {
      return new OPDD<>(); // OPDD in its default configuration
    } else {
      return listReductionFactory.<Node<?>>createListReduction();
    }
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    final Node<?> reduced = syntaxTree.cloneTree();

    int sizeBeforeIteration;
    int sizeAfterIteration = serialize(reduced).length();

    do {
      sizeBeforeIteration = sizeAfterIteration;

      reductionIteration(reduced, run);

      sizeAfterIteration = serialize(reduced).length();
    } while (this.fixpoint && sizeAfterIteration < sizeBeforeIteration);

    return serialize(reduced);
  }

  private final void reductionIteration(final Node<?> syntaxTree, final ReductionRun run) {
    final Set<Node<?>> removedNodes = new HashSet<>();

    final Map<Node<?>, Integer> bfsOrder = computeBFSOrder(syntaxTree);
    final Map<Node<?>, int[]> priorities = new HashMap<>();

    final Queue<Node<?>> worklist = new PriorityQueue<>(
        (t1, t2) -> {
          assert (t1 != t2);

          final int[] priorityVector1 = getPriorityVector(t1, priorities, bfsOrder);
          final int[] priorityVector2 = getPriorityVector(t2, priorities, bfsOrder);

          assert (priorityVector1 != null);
          assert (priorityVector2 != null);
          assert (priorityVector1.length == priorityVector2.length);

          for (int index = 0; index < priorityVector1.length; ++index) {
            if (priorityVector1[index] != priorityVector2[index]) {
              return -Integer.compare(priorityVector1[index], priorityVector2[index]);
            }
          }

          // for all priority variants, the ordering should be unique
          assert (false);
          return 0;
        });

    worklist.add(syntaxTree);

    while (!worklist.isEmpty()) {
      final List<Node<?>> nextNodes = getNextNodes(worklist, priorities, bfsOrder);

      final List<Node<?>> nullable;
      final List<Node<?>> nonNullable;
      {
        final List<Node<?>>[] partition = partitionNullable(nextNodes, removedNodes);
        nullable = partition[0];
        nonNullable = partition[1];
      }

      if (!nullable.isEmpty()) {
        final List<Node<?>> retained = reduceList(syntaxTree, run, nullable, removedNodes);

        removedNodes.addAll(nullable);
        removedNodes.removeAll(retained);

        addChildrenToWorklist(retained, worklist, removedNodes);
      }

      addChildrenToWorklist(nonNullable, worklist, removedNodes);
    }

    syntaxTree.prune(removedNodes);

    run.finishIteration();
  }

  private static final void addChildrenToWorklist(final List<Node<?>> nodes,
      final Queue<Node<?>> worklist, final Set<Node<?>> removedNodes) {
    for (final Node<?> node : nodes) {
      for (final Node<?> child : node.getChildren()) {
        if ((child instanceof NonTerminalNode) && !removedNodes.contains(child)) {
          worklist.add(child);
        }
      }
    }
  }

  private final Map<Node<?>, Integer> computeBFSOrder(final Node<?> syntaxTree) {
    final Map<Node<?>, Integer> bfsOrder = new HashMap<>();

    int bfsNumber = syntaxTree.size();

    final Queue<Node<?>> queue = new LinkedList<>();
    queue.add(syntaxTree);

    while (!queue.isEmpty()) {
      final Node<?> node = queue.remove();
      bfsOrder.put(node, bfsNumber--);

      final List<Node<?>> children = node.getChildren();
      for (int index = children.size() - 1; index >= 0; --index) {
        final Node<?> child = children.get(index);
        queue.add(child);
      }
    }

    return bfsOrder;
  }

  private final int[] getPriorityVector(final Node<?> node, final Map<Node<?>, int[]> priorities,
      final Map<Node<?>, Integer> bfsOrder) {
    if (priorities.containsKey(node)) {
      return priorities.get(node);
    }

    final int[] priorityVector = this.priority.getPriorityVector(node, bfsOrder);
    priorities.put(node, priorityVector);

    return priorityVector;
  }

  private final List<Node<?>> getNextNodes(final Queue<Node<?>> worklist,
      final Map<Node<?>, int[]> priorities, final Map<Node<?>, Integer> bfsOrder) {
    final List<Node<?>> nextNodes = new ArrayList<>();

    final Node<?> head = worklist.remove();
    nextNodes.add(head);

    if (this.hybrid) {
      // in 'hybrid' mode, we not only remove the first element of the worklist but all first
      // elements with the same weight and parent

      final int headWeight;
      {
        final int[] headPriority = getPriorityVector(head, priorities, bfsOrder);
        assert (headPriority != null);

        // the first element of the priority vector corresponds to the node's weight
        // (this is also true for the Perses priority)
        headWeight = headPriority[0];
      }

      while (!worklist.isEmpty()) {
        final Node<?> next = worklist.peek();
        final int nextWeight;
        {
          final int[] nextPriority = priorities.get(next);
          assert (nextPriority != null);

          nextWeight = nextPriority[0]; // see above
        }

        if (headWeight == nextWeight && head.getParent() == next.getParent()) {
          worklist.remove();
          nextNodes.add(next);
        } else {
          break;
        }
      }
    }

    return nextNodes;
  }

  @SuppressWarnings("unchecked")
  private final List<Node<?>>[] partitionNullable(final List<Node<?>> nodes,
      final Set<Node<?>> removedNodes) {
    final List<Node<?>> nullable = new ArrayList<>();
    final List<Node<?>> nonNullable = new ArrayList<>();

    for (final Node<?> node : nodes) {
      if (isNullable(node, removedNodes)) {
        nullable.add(node);
      } else {
        nonNullable.add(node);
      }
    }

    return (List<Node<?>>[]) new List[] {nullable, nonNullable};
  }

  private static final boolean isNullable(final Node<?> node, final Set<Node<?>> removedNodes) {
    final Symbol symbol = node.getSymbol();

    if (symbol != ParserSymbol.LIST_ITEM
        && !(LIST_DELETION && (symbol == ParserSymbol.STAR || symbol == ParserSymbol.OPTIONAL))) {
      // the following nodes are nullable:
      // - list items (if they are not the only (remaining) item of a +-quantified list, see below)
      // - *-quantified lists and optional sub-trees (if enabled)
      // all other nodes are _not_ nullable
      return false;
    }

    assert (symbol == ParserSymbol.LIST_ITEM
        || symbol == ParserSymbol.STAR || symbol == ParserSymbol.OPTIONAL);

    assert (node.getParent() != null);

    // if the node is the only list item of a '+' node, it is _not_ nullable
    if (node.getParent().getSymbol() == ParserSymbol.PLUS
        && numberOfKeptChildren(node.getParent(), removedNodes) == 1) {
      return false;
    }

    // nullability pruning tries to avoid unnecessary checks
    if (NULLABILITY_PRUNING) {
      // if the node is part of a linear chain from another nullable node, removing the node does
      // _not_ have to be checked (the node is not actually nullable)

      if (numberOfKeptChildren(node.getParent(), removedNodes) != 1) {
        // not the only list element --> node is potentially nullable
        return true;
      } else {
        if (LIST_DELETION && symbol == ParserSymbol.LIST_ITEM) {
          // node is the only item of a *- or ?-quantified list and the list itself has already been
          // checked for deletion => node is _not_ actually nullable
          assert (node.getParent().getSymbol() == ParserSymbol.STAR
              || node.getParent().getSymbol() == ParserSymbol.OPTIONAL);
          return false;
        }
      }

      Node<?> chainNode = node.getParent();

      while (true) {
        if (chainNode.getParent() == null) {
          // reached root without finding a chain from another '*' or '?' node --> node is
          // potentially nullable
          return true;
        }

        if (numberOfKeptChildren(chainNode.getParent(), removedNodes) != 1) {
          // not a chain --> node is potentially nullable
          return true;
        }

        final Symbol<?> parentSymbol = chainNode.getParent().getSymbol();
        if (parentSymbol == ParserSymbol.STAR || parentSymbol == ParserSymbol.OPTIONAL) {
          // found a chain from a '*' or '?' node => node is _not_ actually nullable
          return false;
        }

        chainNode = chainNode.getParent();
      }
    }

    return true;
  }

  private static final int numberOfKeptChildren(final Node<?> node,
      final Set<Node<?>> removedNodes) {
    int numberOfKeptChildren = 0;

    for (final Node<?> child : node.getChildren()) {
      if (!removedNodes.contains(child)) {
        ++numberOfKeptChildren;
      }
    }

    return numberOfKeptChildren;
  }

  protected final String serialize(final Node<?> syntaxTree) {
    return serialize(syntaxTree, null);
  }

  protected final String serialize(final Node<?> syntaxTree, final Set<Node<?>> removedNodes) {
    return this.joiner.join(syntaxTree, removedNodes);
  }

  protected final List<Node<?>> reduceList(final Node<?> syntaxTree, final ReductionRun run,
      final List<Node<?>> nodes, final Set<Node<?>> removedNodes) {
    final boolean keepOne;
    {
      assert (!nodes.isEmpty());
      final Node<?> node = nodes.get(0);

      if (node.getParent().getSymbol() == ParserSymbol.PLUS) {
        // parent quantifier is '+'
        // -> if there is at least one list item that is still 'kept' and not contained in the list
        // we are about to reduce, we can remove all nodes of the list; otherwise, we have to keep
        // at least one element of the list
        boolean foundKeptItem = false;
        for (final Node<?> listItem : node.getParent().getChildren()) {
          if (!removedNodes.contains(listItem) && !nodes.contains(listItem)) {
            foundKeptItem = true;
            break;
          }
        }

        keepOne = !foundKeptItem;
      } else {
        // parent quantifier is either '*' or '?' (or the node we are currently considering is a
        // quantifier node itself); either way, we may delete all nodes
        keepOne = false;
      }
    }

    final boolean TEST_EMPTY_LIST = !keepOne;

    final ListReductionCallback<Node<?>> callback = (list) -> {
      if (keepOne && list.isEmpty()) {
        return false;
      }

      final Set<Node<?>> newRemovedNodes = new HashSet<>(removedNodes);

      newRemovedNodes.addAll(nodes);
      newRemovedNodes.removeAll(list);

      final String serialized = serialize(syntaxTree, newRemovedNodes);

      final boolean triggersBug = run.test(serialized);
      return triggersBug;
    };

    // NOTE: for lists with one element, the list reduction should perform exactly one check (it
    // should only check the empty list)
    return this.listReduction.reduce(nodes, callback, TEST_EMPTY_LIST);
  }

}
