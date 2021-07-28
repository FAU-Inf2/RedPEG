package i2.act.reduction.gtr;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.properties.PossibleSubTreesComputation;
import i2.act.grammargraph.properties.SubsumptionComputation;
import i2.act.grammargraph.properties.subtree.SubTree;
import i2.act.grammargraph.properties.subtree.SubTreeSequence;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GTRReducer implements Reducer {

  public static final boolean DEFAULT_SKIP_TERMINAL_NODES = false;
  public static final boolean DEFAULT_SKIP_TERMINAL_TREES = false;


  // ===============================================================================================


  // ~~~ GTR ~~~

  public static final GTRReducer createGTRReducer(final Grammar grammar, final TokenJoiner joiner) {
    return createGTRReducer(grammar, null, joiner);
  }

  public static final GTRReducer createGTRReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {

    final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees =
        PossibleSubTreesComputation.computePossibleSubTrees(GrammarGraph.fromGrammar(grammar));

    final Map<Symbol<?>, Set<Symbol<?>>> subsumption =
        SubsumptionComputation.computeSubsumption(grammar);

    return new GTRReducer(
        new DeletionFilterFromGrammar(possibleSubTrees, subsumption),
        new SubstitutionFilterFromGrammar(possibleSubTrees, subsumption),
        listReductionFactory, joiner,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES,
        false);
  }

  // ~~~ GTR* ~~~

  public static final GTRReducer createGTRFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createGTRFixpointReducer(grammar, null, joiner);
  }

  public static final GTRReducer createGTRFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees =
        PossibleSubTreesComputation.computePossibleSubTrees(GrammarGraph.fromGrammar(grammar));

    final Map<Symbol<?>, Set<Symbol<?>>> subsumption =
        SubsumptionComputation.computeSubsumption(grammar);

    return new GTRReducer(
        new DeletionFilterFromGrammar(possibleSubTrees, subsumption),
        new SubstitutionFilterFromGrammar(possibleSubTrees, subsumption),
        listReductionFactory, joiner,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES,
        true);
  }

  // ~~~ GTR (no filtering) ~~~

  public static final GTRReducer createGTRNoFilteringReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createGTRNoFilteringReducer(grammar, null, joiner);
  }

  public static final GTRReducer createGTRNoFilteringReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new GTRReducer(
        DELETE_ALL, SUBSTITUTE_ALL,
        listReductionFactory, joiner,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES,
        false);
  }

  // ~~~ GTR* (no filtering) ~~~

  public static final GTRReducer createGTRNoFilteringFixpointReducer(final Grammar grammar,
      final TokenJoiner joiner) {
    return createGTRNoFilteringFixpointReducer(grammar, null, joiner);
  }

  public static final GTRReducer createGTRNoFilteringFixpointReducer(final Grammar grammar,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner) {
    return new GTRReducer(
        DELETE_ALL, SUBSTITUTE_ALL,
        listReductionFactory, joiner,
        DEFAULT_SKIP_TERMINAL_NODES, DEFAULT_SKIP_TERMINAL_TREES,
        true);
  }


  // ===============================================================================================


  public static interface DeletionFilter {

    public abstract boolean canDelete(final Node<?> node);

  }

  public static interface SubstitutionFilter {

    public abstract boolean canSubstitute(final Node<?> substituted, final Node<?> substitution);

  }

  private abstract static class GrammarBasedFilter {

    protected final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees;
    protected final Map<Symbol<?>, Set<Symbol<?>>> subsumption;

    protected GrammarBasedFilter(final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees,
        final Map<Symbol<?>, Set<Symbol<?>>> subsumption) {
      this.possibleSubTrees = possibleSubTrees;
      this.subsumption = subsumption;
    }

    protected final boolean matchesOne(final Node<?> parent, final Node<?> originalChild,
        final Node<?> newChild) {
      assert (subsumes(parent, parent))
          : String.format("%s <=> %s", parent, parent.getExpectedSymbol());

      final Symbol<?> parentSymbol = parent.getSymbol();

      if (!this.possibleSubTrees.containsKey(parentSymbol)) {
        return false;
      }

      final List<SubTreeSequence> possibleSubTreeSequences =
          this.possibleSubTrees.get(parentSymbol);

      for (final SubTreeSequence possibleSubTreeSequence : possibleSubTreeSequences) {
        if (possibleSubTreeSequence.isEmptySequence()) {
          if (newChild == null && parent.numberOfChildren() == 1) {
            assert (parent.getChild(0) == originalChild);
            return true;
          }
        } else {
          assert (possibleSubTreeSequence.numberOfTrees() == 1);

          final SubTree possibleSubTree = possibleSubTreeSequence.getSubTrees().get(0);

          if (matches(possibleSubTree, parent, originalChild, newChild)) {
            return true;
          }
        }
      }

      return false;
    }

    protected final boolean matches(final SubTree possibleSubTree, final Node<?> parent,
        final Node<?> originalChild, final Node<?> newChild) {
      // check if root of sub-tree matches
      if (!subsumes(possibleSubTree.getSymbol(), parent.getSymbol())) {
        return false;
      }

      // check if children of sub-tree match
      final int numberOfChildren = (newChild == null)
          ? parent.numberOfChildren() - 1
          : parent.numberOfChildren();

      if (possibleSubTree.numberOfChildren() != numberOfChildren) {
        return false;
      }

      int treeChildIndex = 0;

      for (int subTreeChildIndex = 0; subTreeChildIndex < numberOfChildren; ++subTreeChildIndex) {
        final SubTree subTreeChild = possibleSubTree.getChild(subTreeChildIndex);

        Node<?> treeChild = parent.getChild(treeChildIndex);

        if (treeChild == originalChild) {
          if (newChild == null) {
            ++treeChildIndex;
            treeChild = parent.getChild(treeChildIndex);
          } else {
            if (!matchesExpected(newChild, subTreeChild)) {
              return false;
            }

            ++treeChildIndex;
            continue;
          }
        }

        if (!matchesExpected(treeChild, subTreeChild)) {
          return false;
        }

        ++treeChildIndex;
      }

      return true;
    }

    protected final boolean matchesExpected(final Node<?> node, final SubTree subTreeChild) {
      if (node instanceof NonTerminalNode
          && ((NonTerminalNode) node).isQuantifierNode()) {
        // tree node is a quantifier node => check that expected node is the same quantifier
        if (node.getSymbol() != subTreeChild.getSymbol()) {
          return false;
        }

        // both quantifiers match => check that the expected item subsumes all actual items

        assert (subTreeChild.numberOfChildren() == 1);
        final Symbol<?> expectedItem = subTreeChild.getChild(0).getSymbol();

        for (final Node<?> itemNode : node.getChildren()) {
          assert (itemNode.getSymbol() == ParserSymbol.LIST_ITEM);

          if (!subsumes(expectedItem, itemNode.getExpectedSymbol())) {
            return false;
          }
        }

        // all actual items are okay
        return true;
      }

      return subsumes(subTreeChild.getSymbol(), node.getSymbol());
    }

    protected final boolean subsumes(final Node<?> nodeOne, final Node<?> nodeTwo) {
      return subsumes(nodeOne.getExpectedSymbol(), nodeTwo.getSymbol());
    }

    protected final boolean subsumes(final Symbol<?> symbolOne, final Symbol<?> symbolTwo) {
      if (symbolOne == symbolTwo) {
        return true;
      }

      final Set<Symbol<?>> subsumed = this.subsumption.get(symbolOne);

      return subsumed != null && subsumed.contains(symbolTwo);
    }

  }

  public static final class DeletionFilterFromGrammar
      extends GrammarBasedFilter implements DeletionFilter {

    protected DeletionFilterFromGrammar(
        final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees,
        final Map<Symbol<?>, Set<Symbol<?>>> subsumption) {
      super(possibleSubTrees, subsumption);
    }

    @Override
    public final boolean canDelete(final Node<?> node) {
      if (node.getSymbol() == ParserSymbol.LIST_ITEM) {
        // list items can always be deleted
        // NOTE: for '+'-quantified lists, we have to keep at least one element; however, this is
        // handled during list reduction
        return true;
      }

      final Node<?> parentNode = node.getParent();

      if (parentNode == null) {
        // do not attempt to remove root node
        return false;
      }

      if (parentNode.getSymbol() == ParserSymbol.LIST_ITEM) {
        // do not delete the children of ITEM nodes
        return false;
      }

      // check if the deletion results in a "possible" sub-tree
      return matchesOne(parentNode, node, null);
    }

  }

  public static final DeletionFilter DELETE_ALL = (node) -> true;

  public static final class SubstitutionFilterFromGrammar
      extends GrammarBasedFilter implements SubstitutionFilter {

    protected SubstitutionFilterFromGrammar(
        final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees,
        final Map<Symbol<?>, Set<Symbol<?>>> subsumption) {
      super(possibleSubTrees, subsumption);
    }

    @Override
    public final boolean canSubstitute(final Node<?> substituted, final Node<?> substitution) {
      if ((substituted instanceof NonTerminalNode)
          && ((NonTerminalNode) substituted).isAuxiliaryNode()) {
        // do not substitute auxiliary nodes (i.e., quantifier nodes and list items)
        return false;
      }

      final Node<?> parentNode = substituted.getParent();

      if (parentNode == null) {
        // do not attempt to substitute root node
        return false;
      }

      if (subsumes(substituted, substitution)) {
        // if the symbol of the substituted node subsumes the symbol of the substitution candidate,
        // the substitution is always possible
        return true;
      }

      if (((NonTerminalNode) parentNode).isAuxiliaryNode()) {
        // do not substitute if parent is an auxiliary node
        return false;
      }

      // check if the substitution results in a "possible" sub-tree
      return matchesOne(parentNode, substituted, substitution);
    }

  }

  public static final SubstitutionFilter SUBSTITUTE_ALL = (substituted, substitution) -> true;


  // ===============================================================================================


  private static interface ReductionTemplate {

    public boolean isSingleTransformation();

    public List<Node<?>> apply(final Node<?> node);

  }


  // ===============================================================================================


  private final DeletionFilter deletionFilter;
  private final SubstitutionFilter substitutionFilter;

  private final TokenJoiner joiner;
  private final ListReduction<Node<?>> listReduction;

  private final boolean skipTerminalNodes;
  private final boolean skipTerminalTrees;

  private final boolean fixpoint;

  private final List<ReductionTemplate> reductionTemplates;

  public GTRReducer(
      final DeletionFilter deletionFilter, final SubstitutionFilter substitutionFilter,
      final ListReductionFactory listReductionFactory, final TokenJoiner joiner,
      final boolean skipTerminalNodes, final boolean skipTerminalTrees,
      final boolean fixpoint) {
    this.deletionFilter = deletionFilter;
    this.substitutionFilter = substitutionFilter;

    this.joiner = joiner;
    this.listReduction = createListReduction(listReductionFactory);

    this.skipTerminalNodes = skipTerminalNodes;
    this.skipTerminalTrees = skipTerminalTrees;

    this.fixpoint = fixpoint;

    this.reductionTemplates = createReductionTemplates();
  }

  private final ListReduction<Node<?>> createListReduction(
      final ListReductionFactory listReductionFactory) {
    if (listReductionFactory == null) {
      return new DDMin<Node<?>>(); // ddMin in its default configuration
    } else {
      return listReductionFactory.<Node<?>>createListReduction();
    }
  }

  private final List<ReductionTemplate> createReductionTemplates() {
    final List<ReductionTemplate> reductionTemplates = new ArrayList<>();

    // deletion
    {
      reductionTemplates.add(new ReductionTemplate() {

        @Override
        public final boolean isSingleTransformation() {
          return true;
        }

        public final List<Node<?>> apply(final Node<?> node) {
          if (GTRReducer.this.deletionFilter.canDelete(node)) {
            return Arrays.asList((Node<?>) null);
          } else {
            return Arrays.asList();
          }
        }

      });
    }

    // substitute-by-child
    {
      reductionTemplates.add(new ReductionTemplate() {

        @Override
        public final boolean isSingleTransformation() {
          return false;
        }

        public final List<Node<?>> apply(final Node<?> node) {
          final List<Node<?>> candidates = new ArrayList<>();

          for (final Node<?> child : getChildren(node)) {
            if (GTRReducer.this.substitutionFilter.canSubstitute(node, child)) {
              final Node<?> candidate = child;
              candidates.add(candidate);
            }
          }

          return candidates;
        }

      });
    }

    return reductionTemplates;
  }

  private final List<Node<?>> getChildren(final Node<?> node) {
    final List<Node<?>> children = new ArrayList<>();
    return getChildren(node, children);
  }

  private final List<Node<?>> getChildren(final Node<?> node, final List<Node<?>> children) {
    for (final Node<?> child : node.getChildren()) {
      if ((child instanceof NonTerminalNode) && ((NonTerminalNode) child).isAuxiliaryNode()) {
        getChildren(child, children);
      } else {
        children.add(child);
      }
    }
    return children;
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    Node<?> reduced = syntaxTree.cloneTree();

    int sizeBeforeIteration;
    int sizeAfterIteration = size(reduced);

    do {
      sizeBeforeIteration = sizeAfterIteration;

      reduced = reductionIteration(reduced, run);

      sizeAfterIteration = size(reduced);
    } while (this.fixpoint && sizeAfterIteration < sizeBeforeIteration);

    return serialize(reduced);
  }

  private final Node<?> reductionIteration(final Node<?> syntaxTree, final ReductionRun run) {
    Node<?> reduced = syntaxTree;

    int level = 0;

    while (true) {
      boolean reachedEnd = true;

      for (final ReductionTemplate reductionTemplate : this.reductionTemplates) {
        final List<Node<?>> levelNodes = level(reduced, level);

        reachedEnd &= levelNodes.isEmpty();

        if (!levelNodes.isEmpty()) {
          if (reductionTemplate.isSingleTransformation()) {
            reduced = applySingleTransformation(reductionTemplate, reduced, levelNodes, run);
          } else {
            reduced = applyAlternativeTransformation(reductionTemplate, reduced, levelNodes, run);
          }
        }
      }

      ++level;

      if (reachedEnd) {
        break;
      }
    }

    run.finishIteration();

    return reduced;
  }

  private final String serialize(final Node<?> syntaxTree) {
    return this.joiner.join(syntaxTree);
  }

  private final Node<?> applySingleTransformation(final ReductionTemplate reductionTemplate,
      final Node<?> syntaxTree, final List<Node<?>> levelNodes, final ReductionRun run) {
    final boolean TEST_EMPTY_LIST = true;

    // determine all permitted replacements for the reduction template
    final Map<Node<?>, Node<?>> replacements = getReplacements(levelNodes, reductionTemplate);

    // determine which of the nodes of the current level can actually be replaced (some nodes cannot
    // be replaced due to the filtering)
    final List<Node<?>> replaceableNodes = new ArrayList<>();
    {
      for (final Node<?> levelNode : levelNodes) {
        if (replacements.containsKey(levelNode)) {
          replaceableNodes.add(levelNode);
        }
      }
    }

    // in some cases, not a single node can be replaced -> cancel
    if (replaceableNodes.isEmpty()) {
      return syntaxTree;
    }

    // we have to ensure that '+'-quantified lists keep at least one item; for this purpose, we
    // gather all parents of the currently considered nodes that are '+' nodes and later check that
    // these lists do not become empty
    final Set<Node<?>> plusNodes = new LinkedHashSet<>();
    {
      for (final Node<?> replaceableNode : replaceableNodes) {
        if (replaceableNode.getSymbol() == ParserSymbol.LIST_ITEM) {
          final Node<?> parentNode = replaceableNode.getParent();
          assert (parentNode != null);

          if (parentNode.getSymbol() == ParserSymbol.PLUS) {
            plusNodes.add(parentNode);
          }
        }
      }
    }

    final ListReductionCallback<Node<?>> callback = (keptNodes) -> {
      // get the replacements for the current configuration
      final Map<Node<?>, Node<?>> currentReplacements = new HashMap<>();
      {
        for (final Node<?> replaceableNode : replaceableNodes) {
          if (!keptNodes.contains(replaceableNode)) {
            assert (replacements.containsKey(replaceableNode));
            currentReplacements.put(replaceableNode, replacements.get(replaceableNode));
          }
        }
      }

      // check that all '+'-quantified lists keep at least one element
      for (final Node<?> plusNode : plusNodes) {
        boolean stillHasChild = false;

        for (final Node<?> listItem : plusNode.getChildren()) {
          if (!currentReplacements.containsKey(listItem)
              || currentReplacements.get(listItem) != null) {
            stillHasChild = true;
            break;
          }
        }

        if (!stillHasChild) {
          return false;
        }
      }

      final Node<?> transformed = applyReplacements(syntaxTree, currentReplacements);
      final String serialized = serialize(transformed);

      return run.test(serialized);
    };

    final List<Node<?>> keptNodes =
        this.listReduction.reduce(replaceableNodes, callback, TEST_EMPTY_LIST);

    // get the replacements for the final configuration determined via list reduction
    final Map<Node<?>, Node<?>> finalReplacements = new HashMap<>();
    {
      for (final Node<?> replaceableNode : replaceableNodes) {
        if (!keptNodes.contains(replaceableNode)) {
          assert (replacements.containsKey(replaceableNode));
          finalReplacements.put(replaceableNode, replacements.get(replaceableNode));
        }
      }
    }

    return applyReplacements(syntaxTree, finalReplacements);
  }

  private final Map<Node<?>, Node<?>> getReplacements(final List<Node<?>> levelNodes,
      final ReductionTemplate reductionTemplate) {
    final Map<Node<?>, Node<?>> replacements = new HashMap<>();

    for (final Node<?> levelNode : levelNodes) {
      final List<Node<?>> replacementCandidates = reductionTemplate.apply(levelNode);

      assert (replacementCandidates.size() <= 1);
      if (replacementCandidates.size() == 1) {
        final Node<?> replacement = replacementCandidates.get(0);
        replacements.put(levelNode, replacement);
      }
    }

    return replacements;
  }

  private final Node<?> applyAlternativeTransformation(final ReductionTemplate reductionTemplate,
      final Node<?> syntaxTree, final List<Node<?>> levelNodes, final ReductionRun run) {
    final Map<Node<?>, Node<?>> replacements = new HashMap<>();

    for (final Node<?> levelNode : levelNodes) {
      replacements.put(levelNode, levelNode);
    }

    Node<?> result = syntaxTree;

    // based on the descriptions in the paper, the reduction template is only applied to the
    // original tree => we can "cache" its results so that we do not need to evaluate it more than
    // once
    final Map<Node<?>, List<Node<?>>> reductionCandidates = new HashMap<>();
    {
      for (final Node<?> levelNode : levelNodes) {
        reductionCandidates.put(levelNode, reductionTemplate.apply(levelNode));
      }
    }

    boolean improvementFound;
    do {
      improvementFound = false;

      for (final Node<?> levelNode : levelNodes) {
        Node<?> currentReplacement = replacements.get(levelNode);

        assert (reductionCandidates.containsKey(levelNode));

        for (final Node<?> replacement : reductionCandidates.get(levelNode)) {
          if (size(replacement) >= size(currentReplacement)) {
            continue;
          }

          final Node<?> replacementClone = replacement.cloneTree();
          replacements.put(levelNode, replacementClone);

          final Node<?> transformed = applyReplacements(syntaxTree, replacements);

          final boolean successfulTransformation;

          // if the replacement consists of hoisting the only child, the transformation is always
          // successful -> do not execute test function
          if (currentReplacement.numberOfChildren() == 1
              && currentReplacement.getChild(0) == replacement) {
            successfulTransformation = true;
          } else {
            final String serialized = serialize(transformed);
            successfulTransformation = run.test(serialized);
          }

          if (successfulTransformation) {
            improvementFound = true;
            currentReplacement = replacementClone;

            result = transformed;
          } else {
            replacements.put(levelNode, currentReplacement);
          }
        }
      }
    } while (improvementFound);

    return result;
  }

  private final int size(final Node<?> replacement) {
    if (replacement == null) {
      return 0;
    } else {
      return replacement.size();
    }
  }

  private final Node<?> applyReplacements(final Node<?> syntaxTree,
      final Map<Node<?>, Node<?>> replacements) {
    return syntaxTree.accept(new SyntaxTreeVisitor<Void, Node<?>>() {

      @Override
      public final Node<?> visit(final NonTerminalNode node, final Void parameter) {
        if (replacements.containsKey(node)) {
          final Node<?> replacement = replacements.get(node);
          return replacement;
        }

        final List<Node<?>> transformedChildren = new ArrayList<>();

        for (final Node<?> originalChild : node.getChildren()) {
          final Node<?> transformedChild = originalChild.accept(this, parameter);

          if (transformedChild != null) {
            transformedChildren.add(transformedChild);
            transformedChild.setExpectedSymbol(originalChild.getExpectedSymbol());
          }
        }

        return node.cloneNode(transformedChildren);
      }

      @Override
      public final Node<?> visit(final TerminalNode node, final Void parameter) {
        if (replacements.containsKey(node)) {
          final Node<?> replacement = replacements.get(node);
          return replacement;
        }

        return node.cloneNode();
      }

    }, null);
  }

  protected final List<Node<?>> level(final Node<?> syntaxTree, final int level) {
    final List<Node<?>> nodes = new ArrayList<>();

    if (syntaxTree == null) {
      return nodes;
    }

    syntaxTree.accept(new SyntaxTreeVisitor<Integer, Void>() {

      @Override
      public final Void visit(final NonTerminalNode node, final Integer currentLevel) {
        if (node.isQuantifierNode()) {
          // skip quantifier nodes
          for (final Node<?> child : node.getChildren()) {
            child.accept(this, currentLevel);
          }
        } else {
          if (GTRReducer.this.skipTerminalTrees && node.numberOfTerminals() == 1) {
            return null;
          }

          if (currentLevel == level) {
            nodes.add(node);
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
        if (currentLevel == level && !GTRReducer.this.skipTerminalNodes) {
          nodes.add(node);
        }

        return null;
      }

    }, 0);

    return nodes;
  }

}
