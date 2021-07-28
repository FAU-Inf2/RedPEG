package i2.act.reduction.lists;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.reduction.Reducer;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.dd.DDMin;
import i2.act.reduction.lists.ListReductionCallback;

import java.util.List;

public class ListReducer<E> implements Reducer {

  public static final ListReducer<Token> tokenBased(
      final ListReductionFactory listReductionFactory) {
    return tokenBased(createListReduction(listReductionFactory));
  }

  public static final ListReducer<Token> tokenBased(
      final ListReduction<Token> listReduction) {
    return new ListReducer<Token>(new TokenSlicer(), listReduction);
  }

  public static final ListReducer<String> lineBased(
      final ListReductionFactory listReductionFactory) {
    return lineBased(createListReduction(listReductionFactory));
  }

  public static final ListReducer<String> lineBased(
      final ListReduction<String> listReduction) {
    return new ListReducer<String>(new LineSlicer(), listReduction);
  }

  public static final ListReducer<String> characterBased(
      final ListReductionFactory listReductionFactory) {
    return characterBased(createListReduction(listReductionFactory));
  }

  public static final ListReducer<String> characterBased(
      final ListReduction<String> listReduction) {
    return new ListReducer<String>(new CharacterSlicer(), listReduction);
  }

  private static final <E> ListReduction<E> createListReduction(
      final ListReductionFactory listReductionFactory) {
    if (listReductionFactory == null) {
      return new DDMin<E>(); // ddMin in its default configuration
    } else {
      return listReductionFactory.<E>createListReduction();
    }
  }

  // ===============================================================================================

  protected final Slicer<E> slicer;

  protected final ListReduction<E> listReduction;

  public ListReducer(final Slicer<E> slicer, final ListReduction<E> listReduction) {
    this.slicer = slicer;
    this.listReduction = listReduction;
  }

  @Override
  public final String reduce(final Node<?> syntaxTree, final ReductionRun run) {
    final List<E> list = this.slicer.slice(syntaxTree);

    final ListReductionCallback<E> callback = (candidate) -> run.test(this.slicer.join(candidate));
    final List<E> reduced = this.listReduction.reduce(list, callback);

    final String joined = this.slicer.join(reduced);

    run.finishIteration();

    return joined;
  }

}
