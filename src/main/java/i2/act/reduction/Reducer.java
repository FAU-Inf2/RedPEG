package i2.act.reduction;

import i2.act.packrat.cst.Node;

public interface Reducer {

  default String getName() {
    return this.getClass().getSimpleName().replace("Reducer", "");
  }

  public String reduce(final Node<?> syntaxTree, final ReductionRun run);

}
