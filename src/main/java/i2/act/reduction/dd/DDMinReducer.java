package i2.act.reduction.dd;

import i2.act.packrat.Token;
import i2.act.reduction.lists.CharacterSlicer;
import i2.act.reduction.lists.LineSlicer;
import i2.act.reduction.lists.ListReducer;
import i2.act.reduction.lists.Slicer;
import i2.act.reduction.lists.TokenSlicer;

public final class DDMinReducer<E> extends ListReducer<E> {

  public static final DDMinReducer<Token> tokenBased() {
    return tokenBased(new DDMin<Token>()); // ddMin in its default configuration
  }

  public static final DDMinReducer<Token> tokenBased(final boolean reduceToSubset,
      final boolean reduceToComplement, final boolean reverseSubsets,
      final boolean reverseComplements) {
    return tokenBased(
        new DDMin<Token>(reduceToSubset, reduceToComplement, reverseSubsets, reverseComplements));
  }

  public static final DDMinReducer<Token> tokenBased(final DDMin<Token> ddMin) {
    return new DDMinReducer<Token>(new TokenSlicer(), ddMin);
  }

  public static final DDMinReducer<String> lineBased() {
    return lineBased(new DDMin<String>()); // ddMin in its default configuration
  }

  public static final DDMinReducer<String> lineBased(final boolean reduceToSubset,
      final boolean reduceToComplement, final boolean reverseSubsets,
      final boolean reverseComplements) {
    return lineBased(
        new DDMin<String>(reduceToSubset, reduceToComplement, reverseSubsets, reverseComplements));
  }

  public static final DDMinReducer<String> lineBased(final DDMin<String> ddMin) {
    return new DDMinReducer<String>(new LineSlicer(), ddMin);
  }

  public static final DDMinReducer<String> characterBased() {
    return characterBased(new DDMin<String>()); // ddMin in its default configuration
  }

  public static final DDMinReducer<String> characterBased(final boolean reduceToSubset,
      final boolean reduceToComplement, final boolean reverseSubsets,
      final boolean reverseComplements) {
    return characterBased(
        new DDMin<String>(reduceToSubset, reduceToComplement, reverseSubsets, reverseComplements));
  }

  public static final DDMinReducer<String> characterBased(final DDMin<String> ddMin) {
    return new DDMinReducer<String>(new CharacterSlicer(), ddMin);
  }

  // ===============================================================================================

  private DDMinReducer(final Slicer<E> slicer, final DDMin<E> ddMin) {
    super(slicer, ddMin);
  }

}
