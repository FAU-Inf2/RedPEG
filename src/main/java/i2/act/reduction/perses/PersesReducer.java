package i2.act.reduction.perses;

import i2.act.grammargraph.properties.SubsumptionComputation;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.Reducer;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.dd.DDMin;
import i2.act.reduction.lists.ListReduction;
import i2.act.reduction.lists.ListReductionCallback;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.util.TokenJoiner;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

public final class PersesReducer implements Reducer {

  public static final int UNBOUNDED_BFS = -1;

  public static final int DEFAULT_BFS_DEPTH = 4;

  // ===============================================================================================

  // ~~~ Perses ~~~

  public static final PersesReducer createPersesReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, DEFAULT_BFS_DEPTH, false, listReductionFactory,
        joiner, false);
  }

  public static final PersesReducer createPersesFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, DEFAULT_BFS_DEPTH, false, listReductionFactory,
        joiner, true);
  }

  // ~~~ Unbounded Perses ~~~

  public static final PersesReducer createUnboundedPersesReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, UNBOUNDED_BFS, false, listReductionFactory, joiner, false);
  }

  public static final PersesReducer createUnboundedPersesFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, UNBOUNDED_BFS, false, listReductionFactory, joiner, true);
  }

  // ~~~ Perses (no replacements) ~~~

  public static final PersesReducer createPersesNoReplacementsReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, 0, true, listReductionFactory, joiner, false);
  }

  public static final PersesReducer createPersesNoReplacementsFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, 0, true, listReductionFactory, joiner, true);
  }

  // ~~~ Perses Pre-Reducer ~~~

  public static final PersesReducer createPersesPreReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new PersesReducer(grammar, DEFAULT_BFS_DEPTH, false, listReductionFactory, joiner, false,
        1000, 50000);
  }

  // ===============================================================================================

  private final Map<Symbol<?>, Set<Symbol<?>>> subsumption;

  private final int bfsDepth;
  private final boolean skipRegularNodes;

  private final ListReduction<Node<?>> listReduction;
  private final TokenJoiner joiner;

  private final boolean fixpoint;

  private final int minNodeLimit;
  private final int maxNodeLimit;

  public PersesReducer(final Grammar grammar, final int bfsDepth, final boolean skipRegularNodes,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean fixpoint) {
    this(grammar, bfsDepth, skipRegularNodes, listReductionFactory, joiner, fixpoint, -1, -1);
  }

  public PersesReducer(final Grammar grammar, final int bfsDepth, final boolean skipRegularNodes,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean fixpoint, final int minNodeLimit, final int maxNodeLimit) {
    this.bfsDepth = bfsDepth;
    this.skipRegularNodes = skipRegularNodes;
    this.listReduction = createListReduction(listReductionFactory);
    this.joiner = joiner;
    this.fixpoint = fixpoint;
    this.minNodeLimit = minNodeLimit;
    this.maxNodeLimit = maxNodeLimit;

    if (skipRegularNodes) {
      this.subsumption = null;
    } else {
      this.subsumption = SubsumptionComputation.computeSubsumption(grammar);
    }
  }

  private final ListReduction<Node<?>> createListReduction(
      final ListReductionFactory listReductionFactory) {
    if (listReductionFactory == null) {
      return new DDMin<>(); // ddMin in its default configuration
    } else {
      return listReductionFactory.<Node<?>>createListReduction();
    }
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    Node<?> best = syntaxTree;

    int sizeBeforeIteration;
    int sizeAfterIteration = syntaxTree.print().length();

    do {
      sizeBeforeIteration = sizeAfterIteration;

      best = reductionIteration(best, run);

      sizeAfterIteration = best.print().length();
    } while (this.fixpoint && sizeAfterIteration < sizeBeforeIteration);

    return best.print();
  }

  private final Node<?> reductionIteration(final Node<?> syntaxTree, final ReductionRun run) {
    final Node<?>[] best = { syntaxTree.cloneTree() };

    final Queue<Node<?>> worklist = new PriorityQueue<>(
        (node1, node2) -> {
          final int size1 = node1.numberOfTerminals();
          final int size2 = node2.numberOfTerminals();

          if (size1 == size2) {
            final int bfsOrder1 = bfsOrder(node1, best[0]);
            final int bfsOrder2 = bfsOrder(node2, best[0]);

            assert (bfsOrder1 != bfsOrder2);
            return -Integer.compare(bfsOrder1, bfsOrder2);
          }
          return -Integer.compare(size1, size2);
        });

    worklist.add(best[0]);

    while (!worklist.isEmpty()) {
      final Node<?> current = worklist.remove();
      assert (best[0].containsNode(current));

      final Pair<Node<?>, List<Node<?>>> reductionResult;

      if (this.minNodeLimit > 0 && current.size() < this.minNodeLimit) {
        // sub-tree is too small to be considered
        continue;
      } else if (this.maxNodeLimit > 0 && current.size() > this.maxNodeLimit) {
        // sub-tree is too big to be considered
        reductionResult = new Pair<>(current, current.getChildren());
      } else if (isRegularNode(current)) {
        if (this.skipRegularNodes) {
          reductionResult = new Pair<>(current, current.getChildren());
        } else {
          reductionResult = reduceRegular((NonTerminalNode) current, best[0], run);
        }
      } else if (isOptionalNode(current) || isStarNode(current)) {
        reductionResult = reduceStar((NonTerminalNode) current, best[0], run);
      } else if (isPlusNode(current)) {
        reductionResult = reducePlus((NonTerminalNode) current, best[0], run);
      } else {
        assert (current instanceof TerminalNode);
        continue;
      }

      final Node<?> newCurrent = reductionResult.getFirst();
      final List<Node<?>> newCandidates = reductionResult.getSecond();

      if (current == best[0]) {
        assert (worklist.isEmpty());
        best[0] = newCurrent;
      } else {
        current.replaceWith(newCurrent);
      }

      for (final Node<?> newCandidate : newCandidates) {
        if (newCandidate instanceof NonTerminalNode) {
          worklist.add(newCandidate);
        }
      }
    }

    run.finishIteration();

    return best[0];
  }

  private final int bfsOrder(final Node<?> node, final Node<?> syntaxTree) {
    int bfsNumber = syntaxTree.size();

    final Queue<Node<?>> queue = new LinkedList<>();
    queue.add(syntaxTree);

    while (!queue.isEmpty()) {
      final Node<?> nextNode = queue.remove();

      if (nextNode == node) {
        return bfsNumber;
      }

      final List<Node<?>> children = nextNode.getChildren();
      for (int index = children.size() - 1; index >= 0; --index) {
        final Node<?> child = children.get(index);
        queue.add(child);
      }

      --bfsNumber;
    }

    assert (false);
    return -1;
  }

  private final boolean isRegularNode(final Node<?> node) {
    if (!(node instanceof NonTerminalNode)) {
      return false;
    }

    final NonTerminalNode nonTerminalNode = (NonTerminalNode) node;

    return !nonTerminalNode.isQuantifierNode();
  }

  private final boolean isOptionalNode(final Node<?> node) {
    return node != null && node.getSymbol() == ParserSymbol.OPTIONAL;
  }

  private final boolean isStarNode(final Node<?> node) {
    return node != null && node.getSymbol() == ParserSymbol.STAR;
  }

  private final boolean isPlusNode(final Node<?> node) {
    return node != null && node.getSymbol() == ParserSymbol.PLUS;
  }

  private final boolean isListItemNode(final Node<?> node) {
    return node != null && node.getSymbol() == ParserSymbol.LIST_ITEM;
  }

  private final boolean isQuantifierNode(final Node<?> node) {
    return isOptionalNode(node) || isStarNode(node) || isPlusNode(node);
  }

  private final Pair<Node<?>, List<Node<?>>> reduceRegular(final NonTerminalNode node,
      final Node<?> tree, final ReductionRun run) {
    final Pair<Node<?>, Node<?>> cloneResult = tree.cloneTree(node);
    final Node<?> clonedTree = cloneResult.getFirst();
    final Node<?> clonedNode = cloneResult.getSecond();

    final Symbol<?> expectedSymbol = clonedNode.getExpectedSymbol();
    assert (expectedSymbol != null);

    final Set<Symbol<?>> subsumed = this.subsumption.get(expectedSymbol);
    // NOTE: subsubmed may be 'null' (in case of helper symbols)

    final Node<?> bfsStartNode;
    {
      if (isListItemNode(clonedNode) && clonedNode.numberOfChildren() == 1) {
        bfsStartNode = clonedNode.getChild(0);
      } else {
        bfsStartNode = clonedNode;
      }
    }

    final List<Node<?>> candidates = new ArrayList<>();

    // subsumed nodes
    {
      final List<Node<?>> replacementCandidates = boundedBFS(bfsStartNode,
          (candidate) ->
              candidate.getSymbol() == expectedSymbol
                  || (subsumed != null && subsumed.contains(candidate.getSymbol())),
          this.bfsDepth);

      candidates.addAll(replacementCandidates);
    }

    // quantified nodes
    {
      if (isStarNode(clonedNode.getParent()) || isPlusNode(clonedNode.getParent())) {
        assert (clonedNode.getParent() instanceof NonTerminalNode);
        final NonTerminalNode quantifierParent = (NonTerminalNode) clonedNode.getParent();

        final List<Node<?>> quantifiedCandidates = boundedBFS(bfsStartNode,
            (candidate) -> {
              if (!isQuantifierNode(candidate)) {
                return false;
              }

              boolean allListItemsSubsumed = true;
              {
                list_items: for (final Node<?> child : candidate.getChildren()) {
                  if (child.getExpectedSymbol() == expectedSymbol
                      || (subsumed != null && subsumed.contains(child.getExpectedSymbol()))) {
                    // child is subsumed -> everything okay
                  } else {
                    // child is _not_ subsumed -> no valid candidate
                    allListItemsSubsumed = false;
                    break list_items;
                  }
                }
              }

              return allListItemsSubsumed;
            }, this.bfsDepth);

        candidates.addAll(quantifiedCandidates);
      }
    }

    Node<?> best = clonedNode;

    // sorting by size allows us to end the search for candidates as soon as we have found one
    Collections.sort(candidates,
        (c1, c2) -> Integer.compare(c1.print().length(), c2.print().length()));

    candidates: for (final Node<?> candidate : candidates) {
      assert (clonedNode != candidate);

      final Node<?> replacement;
      {
        if (isListItemNode(clonedNode)) {
          replacement = new NonTerminalNode(ParserSymbol.LIST_ITEM, Arrays.asList(candidate));
          replacement.setExpectedSymbol(clonedNode.getExpectedSymbol());
        } else {
          replacement = candidate;
        }
      }

      clonedNode.replaceWith(replacement);

      final Node<?> newTree = (clonedNode == clonedTree) ? replacement : clonedTree;
      final String serialized = serialize(newTree);

      if (run.test(serialized)) {
        best = replacement;
        break candidates;
      }

      replacement.replaceWith(clonedNode);
    }

    if (best == clonedNode) {
      return new Pair<>(node, node.getChildren());
    } else {
      best.setParentReferences();
      best.setExpectedSymbol(node.getExpectedSymbol());

      return new Pair<>(best, Arrays.asList(best));
    }
  }

  private final Pair<Node<?>, List<Node<?>>> reduceStar(final NonTerminalNode quantifierNode,
      final Node<?> tree, final ReductionRun run) {
    return reduceQuantifier(quantifierNode, tree, quantifierNode.getSymbol(), run);
  }

  private final Pair<Node<?>, List<Node<?>>> reducePlus(final NonTerminalNode quantifierNode,
      final Node<?> tree, final ReductionRun run) {
    return reduceQuantifier(quantifierNode, tree, quantifierNode.getSymbol(), run);
  }

  private final Pair<Node<?>, List<Node<?>>> reduceQuantifier(final NonTerminalNode quantifierNode,
      final Node<?> tree, final ParserSymbol symbol, final ReductionRun run) {
    final boolean keepOne = (symbol == ParserSymbol.PLUS);
    final List<Node<?>> reducedListItems = reduceList(quantifierNode, tree, run, keepOne);

    final NonTerminalNode newList = constructNewList(reducedListItems, symbol);

    return new Pair<>(newList, newList.getChildren());
  }

  private final List<Node<?>> reduceList(final NonTerminalNode quantifierNode,
      final Node<?> tree, final ReductionRun run, final boolean keepOne) {
    assert (quantifierNode.isQuantifierNode());

    final boolean TEST_EMPTY_LIST = !keepOne;

    final List<Node<?>> listItems = quantifierNode.getChildren();

    if (listItems.isEmpty()) {
      return listItems;
    }

    final ListReductionCallback<Node<?>> callback = (list) -> {
      if (keepOne && list.isEmpty()) {
        return false;
      }

      final Set<Node<?>> removedNodes = new HashSet<>(listItems);
      removedNodes.removeAll(list);

      final String serialized = serialize(tree, removedNodes);
      return run.test(serialized);
    };

    return this.listReduction.reduce(listItems, callback, TEST_EMPTY_LIST);
  }

  private final NonTerminalNode constructNewList(final List<Node<?>> listItems,
      final ParserSymbol symbol) {
    final List<Node<?>> clonedListItems = new ArrayList<>(listItems.size());
    {
      for (final Node<?> listItem : listItems) {
        clonedListItems.add(listItem.cloneTree());
      }
    }

    final NonTerminalNode newList = new NonTerminalNode(symbol, clonedListItems);
    newList.setParentReferences();

    return newList;
  }

  private final String serialize(final Node<?> tree) {
    return serialize(tree, null);
  }

  private final String serialize(final Node<?> tree, final Set<Node<?>> removedNodes) {
    return this.joiner.join(tree, removedNodes);
  }

  private final List<Node<?>> boundedBFS(final Node<?> node,
      final Function<Node<?>, Boolean> predicate, final int maxDepth) {
    final List<Node<?>> result = new ArrayList<>();

    final Queue<Node<?>> queue = new LinkedList<>();
    queue.addAll(node.getChildren());

    int remainingDepth = maxDepth;
    while (!queue.isEmpty() && remainingDepth != 0) {
      if (remainingDepth != UNBOUNDED_BFS) {
        --remainingDepth;
      }

      final int queueSize = queue.size();
      candidates: for (int i = 0; i < queueSize; ++i) {
        final Node<?> candidate = queue.remove();

        if (predicate.apply(candidate)) {
          result.add(candidate);
          continue candidates;
        }

        if (remainingDepth != 0) {
          queue.addAll(candidate.getChildren());
        }
      }
    }

    return result;
  }

}
