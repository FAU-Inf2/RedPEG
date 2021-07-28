package i2.act.reduction.hdd;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.Reducer;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.dd.DDMin;
import i2.act.reduction.lists.ListReduction;
import i2.act.reduction.lists.ListReductionCallback;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.util.TokenJoiner;

import java.util.*;

public abstract class HDDVariant implements Reducer {

  public static final boolean CONSIDER_EQUAL_LENGTH_REPLACEMENTS = false;

  protected final ListReduction<Node<?>> listReduction;
  protected final TokenJoiner joiner;

  protected final boolean skipTerminalNodes;
  protected final boolean skipTerminalTrees;
  protected final boolean hideUnremovable;

  protected final Map<Symbol<?>, List<Token>> replacements;
  protected final Map<Symbol<?>, String> replacementTexts;

  public HDDVariant(final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean skipTerminalNodes, final boolean skipTerminalTrees,
      final boolean hideUnremovable, final Map<Symbol<?>, List<Token>> replacements) {
    this.listReduction = createListReduction(listReductionFactory);
    this.joiner = joiner;
    this.skipTerminalNodes = skipTerminalNodes;
    this.skipTerminalTrees = skipTerminalTrees;
    this.hideUnremovable = hideUnremovable;
    this.replacements = replacements;
    this.replacementTexts = this.joiner.join(replacements);
  }

  private final ListReduction<Node<?>> createListReduction(
      final ListReductionFactory listReductionFactory) {
    if (listReductionFactory == null) {
      return new DDMin<Node<?>>(); // ddMin in its default configuration
    } else {
      return listReductionFactory.<Node<?>>createListReduction();
    }
  }

  @Override
  public abstract String reduce(final Node<?> syntaxTree, final ReductionRun run);

  protected final String getReplacementText(final Node<?> node) {
    if (node.getSymbol() == ParserSymbol.LIST_ITEM) {
      assert (node.getParent() instanceof NonTerminalNode);

      final NonTerminalNode quantifierNode = (NonTerminalNode) node.getParent();
      assert (quantifierNode.isQuantifierNode());

      // NOTE: if a +-quantified list contains more than one element, each element has an empty
      // replacement, but during serialization we have to make sure that we keep at least one
      // element (or the minimum replacement for one element, respectively)
      // therefore, the following check is actually not necessary, but it may save some unnecessary
      // test evaluations in certain cases
      if (quantifierNode.getSymbol() != ParserSymbol.PLUS
          || quantifierNode.numberOfChildren() >= 2) {
        return "";
      }
    }

    assert (node.getExpectedSymbol() != null);
    return this.replacementTexts.getOrDefault(node.getExpectedSymbol(), "");
  }

  protected final boolean isUnremovable(final Node<?> node, final Set<Node<?>> removedNodes) {
    final String replacement = getReplacementText(node).trim();
    final String nodeText = serialize(node, removedNodes).trim();

    if (CONSIDER_EQUAL_LENGTH_REPLACEMENTS) {
      return nodeText.length() == replacement.length();
    } else {
      return nodeText.equals(replacement);
    }
  }

  protected final List<Node<?>> getNodes(final Node<?> syntaxTree, final int level,
      final Set<Node<?>> removedNodes) {
    final List<Node<?>> nodes = new ArrayList<>();

    syntaxTree.accept(new SyntaxTreeVisitor<Integer, Void>() {

      @Override
      public final Void visit(final NonTerminalNode node, final Integer currentLevel) {
        if (removedNodes.contains(node)) {
          return null;
        }

        if (node.isQuantifierNode()) {
          for (final Node<?> child : node.getChildren()) {
            child.accept(this, currentLevel);
          }
        } else {
          if (currentLevel == level) {
            if (!HDDVariant.this.skipTerminalTrees || node.numberOfTerminals() > 1) {
              if (!HDDVariant.this.hideUnremovable || !isUnremovable(node, removedNodes)) {
                nodes.add(node);
              }
            }
          } else {
            for (final Node<?> child : node.getChildren()) {
              child.accept(this, currentLevel + 1);
            }
          }
        }

        return null;
      }

      @Override
      public final Void visit(final TerminalNode node, final Integer currentLevel) {
        if (HDDVariant.this.skipTerminalNodes) {
          return null;
        }

        if (removedNodes.contains(node)) {
          return null;
        }

        if (currentLevel == level) {
          if (!HDDVariant.this.hideUnremovable || !isUnremovable(node, removedNodes)) {
            nodes.add(node);
          }
        }

        return null;
      }

    }, 0);

    return nodes;
  }

  protected final String serialize(final Node<?> syntaxTree) {
    return serialize(syntaxTree, null);
  }

  protected final String serialize(final Node<?> syntaxTree, final Set<Node<?>> removedNodes) {
    return this.joiner.join(syntaxTree, removedNodes, this.replacements);
  }

  private final boolean containsEmptiedPlusQuantifiedList(final List<Node<?>> nodes,
      final Set<Node<?>> removedNodes) {
    final Set<NonTerminalNode> checkedParents = new HashSet<>();

    for (final Node<?> node : nodes) {
      if (node.getParent() == null) {
        continue;
      }

      assert (node.getParent() instanceof NonTerminalNode);
      final NonTerminalNode parent = (NonTerminalNode) node.getParent();

      if (checkedParents.contains(parent)) {
        // already checked this parent node; no need to check it twice
        continue;
      }

      if (parent.getSymbol() == ParserSymbol.PLUS) {
        boolean atLeastOneChildRemains = false;
        boolean hasEmptyReplacement = false;

        // check if at least one item remains
        for (final Node<?> sibling : parent.getChildren()) {
          if (!removedNodes.contains(sibling)) {
            atLeastOneChildRemains = true;
            break;
          }

          // if one of the item nodes has an empty replacement, the whole list can safely be
          // removed => no need to keep an element
          assert (this.replacementTexts.containsKey(sibling.getExpectedSymbol()));
          if (this.replacementTexts.get(sibling.getExpectedSymbol()).isEmpty()) {
            hasEmptyReplacement = true;
            break;
          }
        }

        if (!(atLeastOneChildRemains || hasEmptyReplacement)) {
          return true;
        }

        checkedParents.add(parent);
      }
    }

    return false;
  }

  protected final List<Node<?>> reduceList(final Node<?> syntaxTree, final ReductionRun run,
      final List<Node<?>> nodes, final Set<Node<?>> removedNodes,
      final boolean keepOnePlusQuantified) {
    final boolean TEST_EMPTY_LIST = true;

    final ListReductionCallback<Node<?>> callback = (list) -> {
      final Set<Node<?>> newRemovedNodes = new HashSet<>(removedNodes);

      newRemovedNodes.addAll(nodes);
      newRemovedNodes.removeAll(list);

      // if 'keepOnePlusQuantified' is set, we discard all reduction candidates that contain an
      // empty +-quantified list
      if (keepOnePlusQuantified && containsEmptiedPlusQuantifiedList(nodes, newRemovedNodes)) {
        return false;
      }

      final String serialized = serialize(syntaxTree, newRemovedNodes);

      final boolean triggersBug = run.test(serialized);
      return triggersBug;
    };

    return this.listReduction.reduce(nodes, callback, TEST_EMPTY_LIST);
  }

}
