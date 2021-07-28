package i2.act.reduction.lists;

import java.util.List;

public interface ListReductionCallback<E> {

  public boolean test(final List<E> list);

}
