package i2.act.reduction;

import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.cst.Node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ReducerPipeline implements Reducer {

  public static final boolean PAUSE_REDUCTION_RUN_FOR_PARSING = true;

  private final Lexer lexer;
  private final Parser parser;

  private final boolean fixpoint;
  private final List<Reducer> reducers;

  public ReducerPipeline(final Lexer lexer, final Parser parser, final boolean fixpoint,
      final Reducer... reducers) {
    this(lexer, parser, fixpoint, Arrays.asList(reducers));
  }

  public ReducerPipeline(final Lexer lexer, final Parser parser, final boolean fixpoint,
      final List<Reducer> reducers) {
    this.lexer = lexer;
    this.parser = parser;
    this.fixpoint = fixpoint;
    this.reducers = reducers;
    assert (!this.reducers.isEmpty());
  }

  @Override
  public final String getName() {
    return this.reducers.stream().map(Reducer::getName).collect(Collectors.joining("|"));
  }

  private final Node<?> constructSyntaxTree(final String program, final ReductionRun run) {
    if (PAUSE_REDUCTION_RUN_FOR_PARSING) {
      run.pause();
    }

    // TODO should we do some error handling here in case the program is not syntactically valid?
    final Node<?> syntaxTree = this.parser.parse(this.lexer.lex(program));

    if (PAUSE_REDUCTION_RUN_FOR_PARSING) {
      run.resume();
    }

    return syntaxTree;
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    Node<?> currentSyntaxTree = syntaxTree;
    String result = null;

    int sizeBeforeIteration;
    int sizeAfterIteration = currentSyntaxTree.print().length();

    do {
      if (result != null) {
        // not the first iteration => re-construct syntax tree
        currentSyntaxTree = constructSyntaxTree(result, run);
      }

      sizeBeforeIteration = sizeAfterIteration;

      result = reductionIteration(currentSyntaxTree, run);

      sizeAfterIteration = result.length();
    } while (this.fixpoint && sizeAfterIteration < sizeBeforeIteration);

    assert (result != null);
    return result;
  }

  private final String reductionIteration(final Node<?> syntaxTree, final ReductionRun run) {
    Node<?> currentSyntaxTree = syntaxTree;
    String result = null;

    for (int reducerIndex = 0; reducerIndex < this.reducers.size(); ++reducerIndex) {
      final Reducer reducer = this.reducers.get(reducerIndex);
      result = reducer.reduce(currentSyntaxTree, run);

      if (reducerIndex != this.reducers.size() - 1) {
        // re-construct syntax tree
        currentSyntaxTree = constructSyntaxTree(result, run);
      }
    }

    assert (result != null);
    return result;
  }

}
