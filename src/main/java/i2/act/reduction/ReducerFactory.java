package i2.act.reduction;

import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.Token;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.dd.DDMin;
import i2.act.reduction.dd.DDMinReducer;
import i2.act.reduction.dd.OPDDReducer;
import i2.act.reduction.gtr.GTRReducer;
import i2.act.reduction.hdd.HDDReducer;
import i2.act.reduction.hdd.HDDrReducer;
import i2.act.reduction.lists.ListReducer;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.pardis.PardisReducer;
import i2.act.reduction.perses.PersesReducer;
import i2.act.reduction.util.TokenJoiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ReducerFactory {

  public static enum BaseReducerFactory implements ReducerFactory {

    LIST_TOKENS("ListReducerTokens") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return ListReducer.tokenBased(listReductionFactory);
      }

    },

    LIST_LINES("ListReducerLines") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return ListReducer.lineBased(listReductionFactory);
      }

    },

    LIST_CHARACTERS("ListReducerChars") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return ListReducer.characterBased(listReductionFactory);
      }

    },

    DDMIN_TOKENS("DDMinTokens") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.tokenBased();
      }

    },

    DDMIN_TOKENS_REVERSE("DDMinTokensReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.tokenBased(
            DDMin.DEFAULT_REDUCE_TO_SUBSET, DDMin.DEFAULT_REDUCE_TO_COMPLEMENT, true, true);
      }

    },

    DDMIN_LINES("DDMinLines") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.lineBased();
      }

    },

    DDMIN_LINES_REVERSE("DDMinLinesReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.lineBased(
            DDMin.DEFAULT_REDUCE_TO_SUBSET, DDMin.DEFAULT_REDUCE_TO_COMPLEMENT, true, true);
      }

    },

    DDMIN_CHARACTERS("DDMinChars") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.characterBased();
      }

    },

    DDMIN_CHARACTERS_REVERSE("DDMinCharsReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return DDMinReducer.characterBased(
            DDMin.DEFAULT_REDUCE_TO_SUBSET, DDMin.DEFAULT_REDUCE_TO_COMPLEMENT, true, true);
      }

    },

    OPDD_TOKENS("OPDDTokens") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.tokenBased();
      }

    },

    OPDD_TOKENS_REVERSE("OPDDTokensReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.tokenBased(true, true);
      }

    },

    OPDD_LINES("OPDDLines") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.lineBased();
      }

    },

    OPDD_LINES_REVERSE("OPDDLinesReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.lineBased(true, true);
      }

    },

    OPDD_CHARS("OPDDChars") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.characterBased();
      }

    },

    OPDD_CHARS_REVERSE("OPDDCharsReverse") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return OPDDReducer.characterBased(true, true);
      }

    },

    HDD("HDD") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createHDDReducer(listReductionFactory, joiner, replacements);
      }

    },

    HDD_BASE("BaseHDD") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createBaseHDDReducer(listReductionFactory, joiner);
      }

    },

    HDD_FIXPOINT("HDD*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createHDDFixpointReducer(listReductionFactory, joiner, replacements);
      }

    },

    HDD_BASE_FIXPOINT("BaseHDD*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createBaseHDDFixpointReducer(listReductionFactory, joiner);
      }

    },

    COARSE_HDD("CoarseHDD") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createCoarseHDDReducer(listReductionFactory, joiner, replacements);
      }

    },

    COARSE_HDD_FIXPOINT("CoarseHDD*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDReducer.createCoarseHDDFixpointReducer(
            listReductionFactory, joiner, replacements);
      }

    },

    HDD_R("HDDr") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrReducer(listReductionFactory, joiner, replacements);
      }

    },

    HDD_R_FIXPOINT("HDDr*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrFixpointReducer(listReductionFactory, joiner, replacements);
      }

    },

    HDD_R_DF_FW("HDDr_DF_FW") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.DEPTH_FIRST, HDDrReducer.Append.FORWARD);
      }

    },

    HDD_R_DF_FW_FIXPOINT("HDDr_DF_FW*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrFixpointReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.DEPTH_FIRST, HDDrReducer.Append.FORWARD);
      }

    },

    HDD_R_DF_BW("HDDr_DF_BW") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.DEPTH_FIRST, HDDrReducer.Append.BACKWARD);
      }

    },

    HDD_R_DF_BW_FIXPOINT("HDDr_DF_BW*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrFixpointReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.DEPTH_FIRST, HDDrReducer.Append.BACKWARD);
      }

    },

    HDD_BF_FW_R("HDDr_BF_FW") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.BREADTH_FIRST, HDDrReducer.Append.FORWARD);
      }

    },

    HDD_R_BF_FW_FIXPOINT("HDDr_BF_FW*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrFixpointReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.BREADTH_FIRST, HDDrReducer.Append.FORWARD);
      }

    },

    HDD_R_BF_BW("HDDr_BF_BW") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.BREADTH_FIRST, HDDrReducer.Append.BACKWARD);
      }

    },

    HDD_R_BF_BW_FIXPOINT("HDDr_BF_BW*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return HDDrReducer.createHDDrFixpointReducer(listReductionFactory, joiner, replacements,
            HDDrReducer.Traversal.BREADTH_FIRST, HDDrReducer.Append.BACKWARD);
      }

    },

    PERSES("Perses") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createPersesReducer(grammar, listReductionFactory, joiner);
      }

    },

    PERSES_FIXPOINT("Perses*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createPersesFixpointReducer(grammar, listReductionFactory, joiner);
      }

    },

    PERSES_UNBOUNDED("PersesUnbounded") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createUnboundedPersesReducer(grammar, listReductionFactory, joiner);
      }

    },

    PERSES_UNBOUNDED_FIXPOINT("PersesUnbounded*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createUnboundedPersesFixpointReducer(
            grammar, listReductionFactory, joiner);
      }

    },

    PERSES_NO_REPLACEMENTS("PersesNoReplacements") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createPersesNoReplacementsReducer(
            grammar, listReductionFactory, joiner);
      }

    },

    PERSES_NO_REPLACEMENTS_FIXPOINT("PersesNoReplacements*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createPersesNoReplacementsFixpointReducer(
            grammar, listReductionFactory, joiner);
      }

    },

    PERSES_PRE_REDUCER("PersesPreReducer") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PersesReducer.createPersesPreReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS("Pardis") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPardisReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_FIXPOINT("Pardis*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPardisFixpointReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_HYBRID("PardisHybrid") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPardisHybridReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_HYBRID_FIXPOINT("PardisHybrid*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPardisHybridFixpointReducer(
            grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_PERDES("PardisPerses") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPersesReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_PERSES_FIXPOINT("PardisPerses*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPersesFixpointReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_PERDES_N("PardisPersesN") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPersesNReducer(grammar, listReductionFactory, joiner);
      }

    },

    PARDIS_PERSES_N_FIXPOINT("PardisPersesN*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return PardisReducer.createPersesNFixpointReducer(grammar, listReductionFactory, joiner);
      }

    },

    GTR("GTR") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return GTRReducer.createGTRReducer(grammar, listReductionFactory, joiner);
      }

    },

    GTR_FIXPOINT("GTR*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return GTRReducer.createGTRFixpointReducer(grammar, listReductionFactory, joiner);
      }

    },

    GTR_NO_FILTERING("GTRNoFiltering") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return GTRReducer.createGTRNoFilteringReducer(grammar, listReductionFactory, joiner);
      }

    },

    GTR_NO_FILTERING_FIXPOINT("GTRNoFiltering*") {

      @Override
      public final Reducer createReducer(final Lexer lexer, final Parser parser,
          final Grammar grammar, final ListReductionFactory listReductionFactory,
          final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
        return GTRReducer.createGTRNoFilteringFixpointReducer(
            grammar, listReductionFactory, joiner);
      }

    };

    // =============================================================================================

    public final String reducerName;

    private BaseReducerFactory(final String reducerName) {
      this.reducerName = reducerName;
    }

  }

  public Reducer createReducer(final Lexer lexer, final Parser parser, final Grammar grammar,
      final ListReductionFactory listReductionFactory,
      final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner);

  public static ReducerFactory fromName(final String reducerName) {
    if (reducerName.indexOf("|") == -1) {
      return fromSingleName(reducerName);
    } else {
      // reducer pipeline
      final String[] baseReducerNames = reducerName.split("\\|");

      final List<ReducerFactory> reducerFactories = new ArrayList<>();
      final boolean[] fixpoint = { false };

      for (int index = 0; index < baseReducerNames.length; ++index) {
        final String baseReducerName = baseReducerNames[index];

        if (index == baseReducerNames.length - 1 && baseReducerName.equals("*")) {
          fixpoint[0] = true;
        } else {
          final ReducerFactory reducerFactory = fromSingleName(baseReducerName);

          if (reducerFactory == null) {
            return null;
          }

          reducerFactories.add(reducerFactory);
        }
      }

      return new ReducerFactory() {

        @Override
        public final Reducer createReducer(final Lexer lexer, final Parser parser,
            final Grammar grammar, final ListReductionFactory listReductionFactory,
            final Map<Symbol<?>, List<Token>> replacements, final TokenJoiner joiner) {
          final List<Reducer> reducers = new ArrayList<>();

          for (final ReducerFactory reducerFactory : reducerFactories) {
            final Reducer reducer = reducerFactory.createReducer(
                lexer, parser, grammar, listReductionFactory, replacements, joiner);

            reducers.add(reducer);
          }

          return new ReducerPipeline(lexer, parser, fixpoint[0], reducers);
        }

      };
    }
  }

  public static ReducerFactory fromSingleName(final String reducerName) {
    for (final BaseReducerFactory reducerFactory : BaseReducerFactory.values()) {
      if (reducerFactory.reducerName.equals(reducerName)) {
        return reducerFactory;
      }
    }

    return null;
  }

}
