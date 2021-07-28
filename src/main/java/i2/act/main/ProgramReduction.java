package i2.act.main;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.properties.MinTreeComputation;
import i2.act.grammargraph.properties.PossibleSubTreesComputation;
import i2.act.grammargraph.properties.SubsumptionComputation;
import i2.act.grammargraph.properties.subtree.SubTreeSequence;
import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.DotGenerator;
import i2.act.packrat.cst.visitors.PrettyPrinter;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.packrat.cst.visitors.TreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.builder.GrammarBuilder;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.Reducer;
import i2.act.reduction.ReducerFactory;
import i2.act.reduction.ReductionRun;
import i2.act.reduction.lists.ListReductionFactory;
import i2.act.reduction.test.ExternalTestFunction;
import i2.act.reduction.test.TestFunction;
import i2.act.reduction.util.TokenJoiner;
import i2.act.util.ArgumentSplitter;
import i2.act.util.FileUtil;
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static i2.act.peg.builder.GrammarBuilder.*;

public final class ProgramReduction {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_INPUT_FILE = "--in";
  private static final String OPTION_OUTPUT_FILE = "--out";
  private static final String OPTION_OUTPUT_DIRECTOTY = "--outDir";
  private static final String OPTION_GRAMMAR = "--grammar";

  private static final String OPTION_REDUCER = "--reduce";
  private static final String OPTION_LIST_REDUCTION = "--listReduction";
  private static final String OPTION_TEST = "--test";
  private static final String OPTION_JOIN = "--join";
  private static final String OPTION_TRY_FORMAT = "--tryFormat";
  private static final String OPTION_SIZE_LIMIT = "--sizeLimit";
  private static final String OPTION_CHECK_LIMIT = "--checkLimit";
  private static final String OPTION_TIME_LIMIT = "--timeLimit";
  private static final String OPTION_KEEP_ALL = "--keepAll";
  private static final String OPTION_KEEP_SUCCESSFUL = "--keepSuccessful";
  private static final String OPTION_KEEP_UNSUCCESSFUL = "--keepUnsuccessful";
  private static final String OPTION_KEEP_ITERATION_RESULTS = "--keepIterationResults";
  private static final String OPTION_STATS_CSV = "--statsCSV";
  private static final String OPTION_STATS_JSON = "--statsJSON";
  private static final String OPTION_CACHE = "--cache";
  private static final String OPTION_COUNT_TOKENS = "--countTokens";

  private static final String OPTION_PRETTY_PRINT = "--prettyPrint";
  private static final String OPTION_DOT = "--toDot";
  private static final String OPTION_PRINT_GRAMMAR_GRAPH = "--printGG";
  private static final String OPTION_TREE_STATS = "--treeStats";

  private static final String OPTION_REPLACEMENTS = "--replacements";
  private static final String OPTION_PRINT_REPLACEMENTS = "--printReplacements";

  private static final String OPTION_PRINT_SUBSUMPTION = "--printSubsumption";
  private static final String OPTION_PRINT_POSSIBLE_TREES = "--printPossibleTrees";

  private static final String OPTION_OMIT_QUANTIFIERS = "--omitQuantifiers";
  private static final String OPTION_NO_COMPACTIFY = "--noCompactify";

  private static final String OPTION_PARSER_STATISTICS = "--parserStats";

  private static final String OPTION_VERBOSITY = "--verbosity";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_INPUT_FILE, true, true, "<path to input file>");
    argumentsParser.addOption(OPTION_OUTPUT_FILE, false, true, "<path to output file>");
    argumentsParser.addOption(OPTION_OUTPUT_DIRECTOTY, false, true, "<output directory>");
    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<path to grammar>");

    argumentsParser.addOption(OPTION_REDUCER, false, true, "<reducer name>");
    argumentsParser.addOption(OPTION_LIST_REDUCTION, false, true, "<list reduction name>");
    argumentsParser.addOption(OPTION_TEST, false, true, "<path to test script>");
    argumentsParser.addOption(OPTION_JOIN, false, true, "<separator for token joining>");
    argumentsParser.addOption(OPTION_TRY_FORMAT, false);
    argumentsParser.addOption(OPTION_SIZE_LIMIT, false, true, "<limit>");
    argumentsParser.addOption(OPTION_CHECK_LIMIT, false, true, "<limit>");
    argumentsParser.addOption(OPTION_TIME_LIMIT, false, true, "<limit (ms)>");
    argumentsParser.addOption(OPTION_KEEP_ALL, false);
    argumentsParser.addOption(OPTION_KEEP_SUCCESSFUL, false);
    argumentsParser.addOption(OPTION_KEEP_UNSUCCESSFUL, false);
    argumentsParser.addOption(OPTION_KEEP_ITERATION_RESULTS, false);
    argumentsParser.addOption(OPTION_STATS_CSV, false, true, "<CSV file name>");
    argumentsParser.addOption(OPTION_STATS_JSON, false, true, "<JSON file name>");
    argumentsParser.addOption(OPTION_CACHE, false);
    argumentsParser.addOption(OPTION_COUNT_TOKENS, false);

    argumentsParser.addOption(OPTION_PRETTY_PRINT, false);
    argumentsParser.addOption(OPTION_DOT, false);
    argumentsParser.addOption(OPTION_PRINT_GRAMMAR_GRAPH, false);
    argumentsParser.addOption(OPTION_TREE_STATS, false);

    argumentsParser.addOption(OPTION_REPLACEMENTS, false, true, "<path to replacement file>");
    argumentsParser.addOption(OPTION_PRINT_REPLACEMENTS, false);

    argumentsParser.addOption(OPTION_PRINT_SUBSUMPTION, false);
    argumentsParser.addOption(OPTION_PRINT_POSSIBLE_TREES, false);

    argumentsParser.addOption(OPTION_OMIT_QUANTIFIERS, false);
    argumentsParser.addOption(OPTION_NO_COMPACTIFY, false);

    argumentsParser.addOption(OPTION_PARSER_STATISTICS, false);

    argumentsParser.addOption(OPTION_VERBOSITY, false, true,
        "<" + ReductionRun.Verbosity.options() + ">");
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", ProgramReduction.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  public static final void main(final String[] args) {
    ProgramArguments arguments = null;

    try {
      arguments = argumentsParser.parseArgs(args);
    } catch (final Exception exception) {
      abort(String.format("[!] %s", exception.getMessage()));
    }

    assert (arguments != null);

    final ReductionRun.Verbosity verbosity;
    {
      if (arguments.hasOption(OPTION_VERBOSITY)) {
        final String verbosityOption = arguments.getOption(OPTION_VERBOSITY);
        verbosity = ReductionRun.Verbosity.fromName(verbosityOption);

        if (verbosity == null) {
          abort(String.format("[!] invalid verbosity level '%s'", verbosityOption));
        }
      } else {
        verbosity = ReductionRun.DEFAULT_VERBOSITY;
      }
    }

    final String inputFileName = arguments.getOption(OPTION_INPUT_FILE);
    final String outputFileName;
    final String iterationResultFileName;
    {
      if (arguments.hasOption(OPTION_OUTPUT_FILE)) {
        outputFileName = arguments.getOption(OPTION_OUTPUT_FILE);
        iterationResultFileName = FileUtil.prependBeforeFileExtension(outputFileName, "iteration");

        FileUtil.createPathIfNotExists(outputFileName);
      } else {
        if (arguments.hasOption(OPTION_OUTPUT_DIRECTOTY)) {
          final String outputDirectory = arguments.getOption(OPTION_OUTPUT_DIRECTOTY);

          final String baseName =
              outputDirectory + File.separator + FileUtil.getBaseName(inputFileName);

          outputFileName = FileUtil.prependBeforeFileExtension(baseName, "reduced");
          iterationResultFileName = FileUtil.prependBeforeFileExtension(baseName, "iteration");

          FileUtil.createPathIfNotExists(outputFileName);
        } else {
          outputFileName = FileUtil.prependBeforeFileExtension(inputFileName, "reduced");
          iterationResultFileName = FileUtil.prependBeforeFileExtension(inputFileName, "iteration");
        }
      }
    }

    final String grammarPath = arguments.getOption(OPTION_GRAMMAR);

    final boolean quantifierNodes = !arguments.hasOption(OPTION_OMIT_QUANTIFIERS);
    final boolean compactifyTree = !arguments.hasOption(OPTION_NO_COMPACTIFY);

    final Grammar grammar = readGrammar(grammarPath);
    final GrammarGraph grammarGraph = GrammarGraph.fromGrammar(grammar);

    if (arguments.hasOption(OPTION_PRINT_SUBSUMPTION)) {
      final Map<Symbol<?>, Set<Symbol<?>>> subsumption =
          SubsumptionComputation.computeSubsumption(grammarGraph);

      System.err.println("===[ SUBSUMPTION ]===");

      for (final Map.Entry<Symbol<?>, Set<Symbol<?>>> entry : subsumption.entrySet()) {
        final Symbol<?> symbol = entry.getKey();
        final Set<Symbol<?>> subsumes = entry.getValue();

        System.err.format("%s => %s\n", symbol, subsumes);
      }

      System.err.println("=====================");
    }

    if (arguments.hasOption(OPTION_PRINT_POSSIBLE_TREES)) {
      final Map<Symbol<?>, List<SubTreeSequence>> possibleSubTrees =
          PossibleSubTreesComputation.computePossibleSubTrees(grammarGraph);

      System.err.println("===[ POSSIBLE TREES ]===");

      for (final Map.Entry<Symbol<?>, List<SubTreeSequence>> entry : possibleSubTrees.entrySet()) {
        final Symbol<?> symbol = entry.getKey();
        final List<SubTreeSequence> possible = entry.getValue();

        System.err.format("%s => %s\n", symbol, possible);
      }

      System.err.println("========================");
    }

    if (arguments.hasOption(OPTION_PRINT_GRAMMAR_GRAPH)) {
      grammarGraph.printAsDot();
    }

    final Lexer lexer = Lexer.forGrammar(grammar);
    final Parser parser = Parser.fromGrammar(grammar, quantifierNodes);

    Node<?> syntaxTree = null;

    long timeBeforeLexer = 0;
    long timeAfterLexer = 0;
    long timeAfterParser = 0;

    try {
      final String input = FileUtil.readFile(inputFileName);

      timeBeforeLexer = System.currentTimeMillis();

      final TokenStream tokens = lexer.lex(input);

      timeAfterLexer = System.currentTimeMillis();

      syntaxTree = parser.parse(tokens);

      timeAfterParser = System.currentTimeMillis();
    } catch (final Exception exception) {
      System.err.format("[!] %s\n", exception.getMessage());
      System.exit(1);
    }

    if (arguments.hasOption(OPTION_PARSER_STATISTICS)) {
      final long lexerTime = timeAfterLexer - timeBeforeLexer;
      final long parserTime = timeAfterParser - timeAfterLexer;

      System.err.format("[i] time in lexer:  %6d ms\n", lexerTime);
      System.err.format("[i] time in parser: %6d ms\n", parserTime);
      System.err.format("[i] total time:     %6d ms\n", lexerTime + parserTime);
    }

    if (compactifyTree) {
      syntaxTree.compactify();
    }

    if (arguments.hasOption(OPTION_PRETTY_PRINT)) {
      PrettyPrinter.print(syntaxTree, SafeWriter.openStdOut());
    }

    if (arguments.hasOption(OPTION_DOT)) {
      DotGenerator.print(syntaxTree, SafeWriter.openStdOut());
    }

    if (arguments.hasOption(OPTION_TREE_STATS)) {
      printStats(syntaxTree);
    }

    final ListReductionFactory listReductionFactory;
    {
      if (arguments.hasOption(OPTION_LIST_REDUCTION)) {
        final String listReductionName = arguments.getOption(OPTION_LIST_REDUCTION);
        listReductionFactory = ListReductionFactory.fromName(listReductionName);

        if (listReductionFactory == null) {
          abort(String.format("[!] invalid list reduction name '%s'", listReductionName));
        }
      } else {
        listReductionFactory = null; // use resp. default list reduction for each reducer
      }
    }

    final TokenJoiner joiner;
    {
      final boolean tryFormat = arguments.hasOption(OPTION_TRY_FORMAT);
      final String separator = arguments.getOptionOr(OPTION_JOIN, " ");

      joiner = new TokenJoiner(tryFormat, grammar, separator);
    }

    final Map<Symbol<?>, List<Token>> replacements;
    {
      if (arguments.hasOption(OPTION_REDUCER) || arguments.hasOption(OPTION_PRINT_REPLACEMENTS)) {
        final Map<Symbol<?>, List<Token>> autoReplacements =
            MinTreeComputation.computeMinTokens(grammarGraph);

        if (arguments.hasOption(OPTION_REPLACEMENTS)) {
          final String replacementFileName = arguments.getOption(OPTION_REPLACEMENTS);
          autoReplacements.putAll(readReplacements(replacementFileName, grammar));
        }

        replacements =
            MinTreeComputation.computeMinTokenSequences(grammarGraph, joiner, autoReplacements);
      } else {
        replacements = null;
      }
    }

    if (arguments.hasOption(OPTION_PRINT_REPLACEMENTS)) {
      final Map<Symbol<?>, String> replacementTexts = joiner.join(replacements);

      for (final Map.Entry<Symbol<?>, String> replacement : replacementTexts.entrySet()) {
        System.err.format("%s => %s\n", replacement.getKey(), replacement.getValue());
      }
    }

    if (arguments.hasOption(OPTION_REDUCER)) {
      if (!arguments.hasOption(OPTION_TEST)) {
        abort(String.format("[!] option '%s' is required", OPTION_TEST));
      }

      final String reducerName = arguments.getOption(OPTION_REDUCER);
      final ReducerFactory reducerFactory = ReducerFactory.fromName(reducerName);

      if (reducerFactory == null) {
        abort(String.format("[!] invalid reducer name '%s'", reducerName));
      }

      assert (reducerFactory != null);

      final Reducer reducer = reducerFactory.createReducer(
          lexer, parser, grammar, listReductionFactory, replacements, joiner);

      assert (arguments.hasOption(OPTION_TEST));
      final String testCommand = arguments.getOption(OPTION_TEST);
      final String[] testCommandLine = ArgumentSplitter.splitArguments(testCommand);

      final boolean keepSuccessful = arguments.hasOption(OPTION_KEEP_SUCCESSFUL)
          || arguments.hasOption(OPTION_KEEP_ALL);
      final boolean keepUnsuccessful = arguments.hasOption(OPTION_KEEP_UNSUCCESSFUL)
          || arguments.hasOption(OPTION_KEEP_ALL);

      final TestFunction testFunction = new ExternalTestFunction(testCommandLine, outputFileName,
          keepSuccessful, keepUnsuccessful);

      // this should make time measurements somewhat more deterministic...
      System.gc();

      final ReductionRun run = new ReductionRun(syntaxTree, reducer, testFunction,
          arguments.hasOption(OPTION_KEEP_ITERATION_RESULTS) ? iterationResultFileName : null,
          arguments.hasOption(OPTION_COUNT_TOKENS) ? grammar : null, verbosity);
      {
        if (arguments.hasOption(OPTION_CACHE)) {
          run.enableCache();
        }

        if (arguments.hasOption(OPTION_SIZE_LIMIT)) {
          final int sizeLimit = arguments.getIntOption(OPTION_SIZE_LIMIT);
          run.setSizeLimit(sizeLimit);
        }

        if (arguments.hasOption(OPTION_CHECK_LIMIT)) {
          final int checkLimit = arguments.getIntOption(OPTION_CHECK_LIMIT);
          run.setCheckLimit(checkLimit);
        }

        if (arguments.hasOption(OPTION_TIME_LIMIT)) {
          final int timeLimit = arguments.getIntOption(OPTION_TIME_LIMIT);
          run.setTimeLimit(timeLimit);
        }
      }

      run.start();

      if (arguments.hasOption(OPTION_STATS_CSV)) {
        final String statsFileName = arguments.getOption(OPTION_STATS_CSV);
        run.writeAsCSV(statsFileName, ReductionRun.ONLY_SUCCESSFUL);
      }

      if (arguments.hasOption(OPTION_STATS_JSON)) {
        final String statsFileName = arguments.getOption(OPTION_STATS_JSON);
        final Map<String, Object> configurationOptions = getConfigurationOptions(arguments);

        run.writeAsJSON(statsFileName, configurationOptions, ReductionRun.ONLY_SUCCESSFUL);
      }
    }
  }

  private static final void abort(final String message) {
    System.err.println(message);
    usage();
    System.exit(1);
  }

  private static final Map<String, Object> getConfigurationOptions(
      final ProgramArguments arguments) {
    final Map<String, Object> configurationOptions = new LinkedHashMap<>();

    setConfigurationOption(OPTION_INPUT_FILE, configurationOptions, arguments);
    setConfigurationOption(OPTION_OUTPUT_FILE, configurationOptions, arguments);
    setConfigurationOption(OPTION_OUTPUT_DIRECTOTY, configurationOptions, arguments);
    setConfigurationOption(OPTION_GRAMMAR, configurationOptions, arguments);
    setConfigurationOption(OPTION_REDUCER, configurationOptions, arguments);
    setConfigurationOption(OPTION_LIST_REDUCTION, configurationOptions, arguments);
    setConfigurationOption(OPTION_TEST, configurationOptions, arguments);
    setConfigurationOption(OPTION_SIZE_LIMIT, configurationOptions, arguments);
    setConfigurationOption(OPTION_CHECK_LIMIT, configurationOptions, arguments);
    setConfigurationOption(OPTION_TIME_LIMIT, configurationOptions, arguments);
    setConfigurationOption(OPTION_CACHE, configurationOptions, arguments);
    setConfigurationOption(OPTION_REPLACEMENTS, configurationOptions, arguments);
    setConfigurationOption(OPTION_OMIT_QUANTIFIERS, configurationOptions, arguments);
    setConfigurationOption(OPTION_NO_COMPACTIFY, configurationOptions, arguments);

    return configurationOptions;
  }

  private static final void setConfigurationOption(final String option,
      final Map<String, Object> configurationOptions, final ProgramArguments arguments) {
    if (arguments.hasOption(option)) {
      final String argument = arguments.getOption(option);

      final String strippedOption = option.replaceFirst("^--", "");

      if (argument == null) {
        configurationOptions.put(strippedOption, true);
      } else {
        configurationOptions.put(strippedOption, argument);
      }
    }
  }

  private static final Grammar readGrammar(final String grammarPath) {
    final String grammarInput = FileUtil.readFile(grammarPath);
    final Grammar grammar = PEGParser.parse(grammarInput);
    NameAnalysis.analyze(grammar);

    return grammar;
  }

  private static final void printStats(final Node<?> syntaxTree) {
    final int[] terminalCounter = { 0 };
    final int[] nonTerminalCounter = { 0 };
    final int[] nonTerminalCounterAll = { 0 };

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      @Override
      public final Void visit(final NonTerminalNode node, final Void parameter) {
        ++nonTerminalCounterAll[0];

        if (!node.isAuxiliaryNode()) {
          ++nonTerminalCounter[0];
        }

        return super.visit(node, parameter);
      }

      @Override
      public final Void visit(final TerminalNode node, final Void parameter) {
        ++terminalCounter[0];
        return super.visit(node, parameter);
      }

    }, null);

    System.err.format("[i] number of     terminals         : %10d\n", terminalCounter[0]);
    System.err.format("[i] number of non-terminals         : %10d\n", nonTerminalCounter[0]);
    System.err.format("[i] number of non-terminals (w/ aux): %10d\n", nonTerminalCounterAll[0]);

    final int[] quantifierCounter = { 0 };
    final int[] itemCounter = { 0 };
    final int[] singleElementCounter = { 0 };

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      @Override
      public final Void visit(final NonTerminalNode node, final Void parameter) {
        if (node.isQuantifierNode()) {
          ++quantifierCounter[0];

          if (node.numberOfChildren() == 1) {
            ++singleElementCounter[0];
          }
        }

        if (node.isListItemNode()) {
          ++itemCounter[0];
        }

        return super.visit(node, parameter);
      }

    }, null);

    System.err.format("[i] number of quantifiers           : %10d\n", quantifierCounter[0]);
    System.err.format("[i] number of quantifiers w/ 1 item : %10d\n", singleElementCounter[0]);
    System.err.format("[i] number of list items            : %10d\n", itemCounter[0]);
  }

  private static final Map<Symbol<?>, List<Token>> readReplacements(
      final String replacementFileName, final Grammar grammar) {
    final GrammarBuilder builder = new GrammarBuilder();

    final LexerSymbol COLON = builder.define("COLON", "':'");
    final LexerSymbol SEMICOLON = builder.define("SEMICOLON", "';'");
    final LexerSymbol SPACE = builder.define("SPACE", "( ' ' | '\\n' | '\\r' | '\\t' )+", true);
    final LexerSymbol STRING = builder.define("STRING", "'\"' [^\"]* '\"'");
    final LexerSymbol IDENTIFIER = builder.define("IDENTIFIER", "[a-zA-Z_] [a-zA-Z0-9_]*");

    final ParserSymbol replacements = builder.declare("replacements");
    final ParserSymbol replacement = builder.declare("replacement");

    builder.define(replacements,
        seq(many(replacement), LexerSymbol.EOF));

    builder.define(replacement,
        seq(IDENTIFIER, COLON, STRING, SEMICOLON));

    final Grammar replacementGrammar = builder.build();

    final String input = FileUtil.readFile(replacementFileName);

    final Lexer lexer = Lexer.forGrammar(replacementGrammar);
    final Parser parser = Parser.fromGrammar(replacementGrammar, replacements);

    final TokenStream tokens = lexer.lex(input);
    final Node<?> syntaxTree = parser.parse(tokens);

    final Map<Symbol<?>, List<Token>> manualReplacements = new HashMap<>();

    final Lexer grammarLexer = Lexer.forGrammar(grammar);

    TreeVisitor.leftToRight(replacement, (node, parameter) -> {
      final String symbolName = node.getChild(IDENTIFIER).getText();
      final String replacementTextRaw = node.getChild(STRING).getText();

      assert (replacementTextRaw.length() >= 2
          && replacementTextRaw.startsWith("\"") && replacementTextRaw.endsWith("\""));

      final String replacementText =
          replacementTextRaw.substring(1, replacementTextRaw.length() - 1);

      final Symbol<?> symbol = grammar.getSymbol(symbolName);
      if (symbol != null) {
        List<Token> replacementTokens;
        {
          try {
            replacementTokens = grammarLexer.lex(replacementText).getTokens();
          } catch (final Throwable throwable) {
            System.err.format("[!] replacement '%s' contains invalid tokens\n", replacementText);
            replacementTokens = Arrays.asList(Token.pseudoToken(replacementText));
          }
        }

        manualReplacements.put(symbol, replacementTokens);
      }

      return null;
    }).visit(syntaxTree);

    return manualReplacements;
  }

}
