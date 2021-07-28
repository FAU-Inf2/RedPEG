package i2.act.reduction.dd;

import i2.act.packrat.Token;
import i2.act.reduction.lists.CharacterSlicer;
import i2.act.reduction.lists.LineSlicer;
import i2.act.reduction.lists.ListReducer;
import i2.act.reduction.lists.Slicer;
import i2.act.reduction.lists.TokenSlicer;

public final class OPDDReducer<E> extends ListReducer<E> {

  public static final OPDDReducer<Token> tokenBased() {
    return tokenBased(new OPDD<Token>()); // OPDD in its default configuration
  }

  public static final OPDDReducer<Token> tokenBased(final boolean reverseSubsets,
      final boolean reverseComplements) {
    return tokenBased(new OPDD<Token>(reverseSubsets, reverseComplements));
  }

  public static final OPDDReducer<Token> tokenBased(final OPDD<Token> opdd) {
    return new OPDDReducer<Token>(new TokenSlicer(), opdd);
  }

  public static final OPDDReducer<String> lineBased() {
    return lineBased(new OPDD<String>()); // OPDD in its default configuration
  }

  public static final OPDDReducer<String> lineBased(final boolean reverseSubsets,
      final boolean reverseComplements) {
    return lineBased(new OPDD<String>(reverseSubsets, reverseComplements));
  }

  public static final OPDDReducer<String> lineBased(final OPDD<String> opdd) {
    return new OPDDReducer<String>(new LineSlicer(), opdd);
  }

  public static final OPDDReducer<String> characterBased() {
    return characterBased(new OPDD<String>()); // OPDD in its default configuration
  }

  public static final OPDDReducer<String> characterBased(final boolean reverseSubsets,
      final boolean reverseComplements) {
    return characterBased(new OPDD<String>(reverseSubsets, reverseComplements));
  }

  public static final OPDDReducer<String> characterBased(final OPDD<String> opdd) {
    return new OPDDReducer<String>(new CharacterSlicer(), opdd);
  }

  // ===============================================================================================

  private OPDDReducer(final Slicer<E> slicer, final OPDD<E> opdd) {
    super(slicer, opdd);
  }

}
