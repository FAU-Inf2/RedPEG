package i2.act.reduction.lists;

import java.util.List;

public interface ListReduction<E> {

  default List<E> reduce(final List<E> list, final ListReductionCallback<E> callback) {
    return reduce(list, callback, false);
  }

  public List<E> reduce(final List<E> list, final ListReductionCallback<E> callback,
      final boolean testEmptyList);

}
