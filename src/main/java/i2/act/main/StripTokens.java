package i2.act.main;

import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.visitors.TreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.builder.GrammarBuilder;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.reduction.util.TokenJoiner;
import i2.act.util.FileUtil;
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.util.ArrayList;
import java.util.List;

import static i2.act.peg.builder.GrammarBuilder.*;

public final class StripTokens {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_INPUT_FILE = "--in";
  private static final String OPTION_OUTPUT_FILE = "--out";
  private static final String OPTION_GRAMMAR = "--grammar";
  private static final String OPTION_STRIP = "--strip";

  private static final String OPTION_JOIN = "--join";
  private static final String OPTION_TRY_FORMAT = "--tryFormat";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_INPUT_FILE, true, true, "<path to input file>");
    argumentsParser.addOption(OPTION_OUTPUT_FILE, true, true, "<path to output file>");
    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<path to grammar>");
    argumentsParser.addOption(OPTION_STRIP, true, true, "<comma separated list of terminals>");

    argumentsParser.addOption(OPTION_JOIN, false, true, "<separator for token joining>");
    argumentsParser.addOption(OPTION_TRY_FORMAT, false);
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", StripTokens.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  private static final void abort(final String message) {
    System.err.format("[!] %s\n", message);
    usage();
    System.exit(1);
  }

  public static final void main(final String[] args) {
    ProgramArguments arguments = null;

    try {
      arguments = argumentsParser.parseArgs(args);
    } catch (final Exception exception) {
      abort(exception.getMessage());
    }

    final String inputFileName = arguments.getOption(OPTION_INPUT_FILE);
    final String outputFileName = arguments.getOption(OPTION_OUTPUT_FILE);

    final String grammarPath = arguments.getOption(OPTION_GRAMMAR);

    final Grammar grammar = readGrammar(grammarPath);

    List<LexerSymbol> terminals = null;
    {
      try {
        final String terminalString = arguments.getOption(OPTION_STRIP);
        terminals = readTerminals(terminalString, grammar);
      } catch (final Throwable throwable) {
        abort(throwable.getMessage());
      }
    }

    assert (terminals != null);

    final List<Token> strippedTokens = new ArrayList<>();
    {
      final Lexer lexer = Lexer.forGrammar(grammar);
      TokenStream tokens = null;

      try {
        final String input = FileUtil.readFile(inputFileName);
        tokens = lexer.lex(input, true);
      } catch (final Throwable throwable) {
        abort(throwable.getMessage());
      }

      assert (tokens != null);

      for (final Token token : tokens) {
        assert (token.getSkippedTokensBefore().isEmpty());

        if (!terminals.contains(token.getTokenSymbol())) {
          strippedTokens.add(token);
        }
      }
    }

    final TokenJoiner joiner;
    {
      final boolean tryFormat = arguments.hasOption(OPTION_TRY_FORMAT);
      final String separator = arguments.getOptionOr(OPTION_JOIN, " ");

      joiner = new TokenJoiner(tryFormat, grammar, separator);
    }

    final SafeWriter outputWriter = SafeWriter.openFile(outputFileName);
    outputWriter.write(joiner.join(strippedTokens));
    outputWriter.close();
  }

  private static final Grammar readGrammar(final String grammarPath) {
    final String grammarInput = FileUtil.readFile(grammarPath);
    final Grammar grammar = PEGParser.parse(grammarInput);
    NameAnalysis.analyze(grammar);

    return grammar;
  }

  private static final List<LexerSymbol> readTerminals(
      final String terminalString, final Grammar grammar) {
    final GrammarBuilder builder = new GrammarBuilder();

    final LexerSymbol COMMA = builder.define("COMMA", "','");
    final LexerSymbol SPACE = builder.define("SPACE", "( ' ' | '\\n' | '\\r' | '\\t' )+", true);
    final LexerSymbol TERMINAL = builder.define("TERMINAL", "[A-Z][A-Z0-9_]*");

    final ParserSymbol terminalList = builder.define("terminalList",
        seq(opt(seq(TERMINAL, many(seq(COMMA, TERMINAL)))), LexerSymbol.EOF));

    final Grammar terminalsGrammar = builder.build();

    final Lexer lexer = Lexer.forGrammar(terminalsGrammar);
    final Parser parser = Parser.fromGrammar(terminalsGrammar);

    final TokenStream tokens = lexer.lex(terminalString);
    final Node<?> syntaxTree = parser.parse(tokens);

    final List<LexerSymbol> terminals = new ArrayList<>();

    TreeVisitor.leftToRight(TERMINAL, (node, parameter) -> {
      final String terminalName = node.getText();
      final Symbol<?> terminalSymbol = grammar.getSymbol(terminalName);

      if (terminalSymbol == null) {
        throw new RuntimeException(String.format("undefined terminal '%s'", terminalName));
      }

      assert (terminalSymbol instanceof LexerSymbol);
      terminals.add((LexerSymbol) terminalSymbol);

      return null;
    }).visit(syntaxTree);

    return terminals;
  }

}
