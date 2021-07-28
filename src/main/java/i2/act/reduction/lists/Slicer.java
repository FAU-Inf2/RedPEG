package i2.act.reduction.lists;

import i2.act.packrat.cst.Node;

import java.util.List;

public interface Slicer<E> {

  public List<E> slice(final Node<?> syntaxTree);

  public String join(final List<E> list);

}
