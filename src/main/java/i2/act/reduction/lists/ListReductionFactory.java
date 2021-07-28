package i2.act.reduction.lists;

import i2.act.reduction.dd.DDMin;
import i2.act.reduction.dd.OPDD;

public enum ListReductionFactory {

  DDMIN("DDMin") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>();
    }

  },

  DDMIN_REVERSE("DDMinReverse") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>(
          DDMin.DEFAULT_REDUCE_TO_SUBSET, DDMin.DEFAULT_REDUCE_TO_COMPLEMENT, true, true);
    }

  },

  DDMIN_SUBSETS("DDMinSubsets") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>(true, false,
          DDMin.DEFAULT_REVERSE_SUBSETS, DDMin.DEFAULT_REVERSE_COMPLEMENTS);
    }

  },

  DDMIN_SUBSETS_REVERSE("DDMinSubsetsReverse") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>(true, false, true, false);
    }

  },

  DDMIN_COMPLEMENTS("DDMinComplements") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>(false, true,
          DDMin.DEFAULT_REVERSE_SUBSETS, DDMin.DEFAULT_REVERSE_COMPLEMENTS);
    }

  },

  DDMIN_COMPLEMENTS_REVERSE("DDMinComplementsReverse") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new DDMin<E>(false, true, false, true);
    }

  },

  OPDD("OPDD") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new OPDD<E>();
    }

  },

  OPDD_REVERSE("OPDDReverse") {

    @Override
    public final <E> ListReduction<E> createListReduction() {
      return new OPDD<E>(true, true);
    }

  };

  // ===============================================================================================

  private final String listReductionName;

  private ListReductionFactory(final String listReductionName) {
    this.listReductionName = listReductionName;
  }

  public final String getListReductionName() {
    return this.listReductionName;
  }

  public abstract <E> ListReduction<E> createListReduction();

  public static final ListReductionFactory fromName(final String listReductionName) {
    for (final ListReductionFactory listReductionFactory : values()) {
      if (listReductionFactory.listReductionName.equals(listReductionName)) {
        return listReductionFactory;
      }
    }

    return null;
  }

}
