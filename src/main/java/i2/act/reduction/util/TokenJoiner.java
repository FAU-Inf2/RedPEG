package i2.act.reduction.util;

import i2.act.packrat.Lexer;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TokenJoiner {

  private final boolean tryFormat;

  private final String separator;
  private final Lexer lexer;

  public TokenJoiner(final boolean tryFormat, final Grammar grammar) {
    this(tryFormat, grammar, " ");
  }

  public TokenJoiner(final boolean tryFormat, final Grammar grammar, final String separator) {
    this.tryFormat = tryFormat;
    this.separator = separator;
    this.lexer = Lexer.forGrammar(grammar);
  }

  public final String getSeparator() {
    return this.separator;
  }

  private final boolean isOriginalToken(final Token token) {
    return token.getBegin() != SourcePosition.UNKNOWN;
  }

  private final boolean consecutiveTokens(final Token firstToken, final Token secondToken) {
    if (firstToken == null || secondToken == null) {
      return false;
    }

    if (!isOriginalToken(firstToken) || !isOriginalToken(secondToken)) {
      return false;
    }

    final SourcePosition endFirst = firstToken.getEnd();
    final SourcePosition beginSecond;
    {
      if (secondToken.getSkippedTokensBefore().isEmpty()) {
        beginSecond = secondToken.getBegin();
      } else {
        beginSecond = secondToken.getSkippedTokensBefore().get(0).getBegin();
      }
    }

    return (endFirst.offset == beginSecond.offset);
  }

  public final boolean needsSeparator(final Token firstToken, final Token secondToken) {
    if (firstToken == null) {
      return false;
    }

    if (secondToken.getTokenSymbol() == LexerSymbol.EOF) {
      return false;
    }

    if (firstToken.getTokenSymbol() == null && secondToken.getTokenSymbol() == null) {
      return true;
    }

    if (consecutiveTokens(firstToken, secondToken)) {
      return false;
    }

    final TokenStream tokens;
    {
      final String joined;
      {
        final StringBuilder builder = new StringBuilder();

        builder.append(firstToken.getValue());

        for (final Token skippedToken : secondToken.getSkippedTokensBefore()) {
          builder.append(skippedToken.getValue());
        }

        builder.append(secondToken.getValue());

        joined = builder.toString();
      }

      try {
        tokens = this.lexer.lex(joined, true);
      } catch (final Exception exception) {
        return true;
      }
    }

    if (tokens.numberOfTokens() < 2) {
      return true;
    }

    if (firstToken.getTokenSymbol() == null) {
      assert (secondToken.getTokenSymbol() != null);

      // check if last token in lexed token stream matches 'secondToken'
      final Token lexedLastToken = tokens.at(tokens.numberOfTokens() - 1);
      return lexedLastToken.getTokenSymbol() != secondToken.getTokenSymbol();
    } else if (secondToken.getTokenSymbol() == null) {
      assert (firstToken.getTokenSymbol() != null);

      // check if first token in lexed token stream matches 'firstToken'
      final Token lexedFirstToken = tokens.at(0);
      return lexedFirstToken.getTokenSymbol() != firstToken.getTokenSymbol();
    } else {
      final Token lexedFirstToken = tokens.at(0);
      final Token lexedLastToken = tokens.at(tokens.numberOfTokens() - 1);

      return lexedFirstToken.getTokenSymbol() != firstToken.getTokenSymbol()
          || lexedLastToken.getTokenSymbol() != secondToken.getTokenSymbol();
    }
  }

  public final int format(final Token firstToken, final Token secondToken) {
    if (firstToken == null) {
      return -1;
    }

    if (secondToken.getTokenSymbol() == LexerSymbol.EOF) {
      return -1;
    }

    if (consecutiveTokens(firstToken, secondToken)) {
      return -1;
    }

    final SourcePosition firstPosition = firstToken.getEnd();
    final SourcePosition secondPosition = secondToken.getBegin();

    if (firstPosition == SourcePosition.UNKNOWN || secondPosition == SourcePosition.UNKNOWN
        || firstPosition.line == secondPosition.line) {
      return -1;
    }

    // check if there is already a line break in one of the "skipped" tokens
    for (final Token skippedToken : secondToken.getSkippedTokensBefore()) {
      if (skippedToken.getValue().contains("\n")) {
        // there is already a line break
        return -1;
      }
    }

    return 0; // TODO can we improve this somehow?
  }

  public final String join(final Token firstToken, final Token secondToken) {
    if (firstToken == null) {
      return secondToken.getValue();
    }

    final String separator =
        (needsSeparator(firstToken, secondToken)) ? (this.separator) : ("");

    return String.format("%s%s%s", firstToken.getValue(), separator, secondToken.getValue());
  }

  public final String join(final List<Token> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return "";
    }

    final StringBuilder builder = new StringBuilder();

    Token lastToken = null;
    for (final Token token : tokens) {
      int indentation = -1;

      if (this.tryFormat) {
        indentation = format(lastToken, token);

        if (indentation >= 0) {
          builder.append("\n");

          while (indentation-- > 0) {
            builder.append(" ");
          }
        }
      }

      for (final Token skippedToken : token.getSkippedTokensBefore()) {
        builder.append(skippedToken.getValue());
      }

      if (indentation == -1 && needsSeparator(lastToken, token)) {
        builder.append(this.separator);
      }

      builder.append(token.getValue());

      lastToken = token;
    }

    return builder.toString();
  }

  public final String join(final TokenStream tokens) {
    return join(tokens.getTokens());
  }

  public final String join(final Node<?> syntaxTree) {
    return join(syntaxTree, null, null);
  }

  public final String join(final Node<?> syntaxTree, final Set<Node<?>> removedNodes) {
    return join(syntaxTree, removedNodes, null);
  }

  public final String join(final Node<?> syntaxTree, final Set<Node<?>> removedNodes,
      final Map<Symbol<?>, List<Token>> replacements) {
    if (syntaxTree == null) {
      return "";
    }

    final List<Token> tokens = new ArrayList<>();

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      private final boolean keep(final Node<?> node) {
        return removedNodes == null || !removedNodes.contains(node);
      }

      @Override
      public final Void visit(final NonTerminalNode node, final Void parameter) {
        if (keep(node)) {
          for (final Node<?> child : node.getChildren()) {
            child.accept(this, parameter);
          }
        } else {
          addTokensForReplacement(node);
        }

        return null;
      }

      @Override
      public final Void visit(final TerminalNode node, final Void parameter) {
        if (keep(node)) {
          tokens.add(node.getToken()); // NOTE: this includes all skipped tokens
        } else {
          addTokensForReplacement(node);
        }

        return null;
      }

      private final SourcePosition getBegin(final Node<?> node) {
        if (node instanceof TerminalNode) {
          final Token token = ((TerminalNode) node).getToken();
          return token.getBegin();
        }

        for (final Node<?> child : node.getChildren()) {
          final SourcePosition begin = getBegin(child);
          if (begin != SourcePosition.UNKNOWN) {
            return begin;
          }
        }

        return SourcePosition.UNKNOWN;
      }

      private final SourcePosition getEnd(final Node<?> node) {
        if (node instanceof TerminalNode) {
          final Token token = ((TerminalNode) node).getToken();
          return token.getEnd();
        }

        for (int childIndex = node.numberOfChildren() - 1; childIndex >= 0; --childIndex) {
          final Node<?> child = node.getChild(childIndex);

          final SourcePosition end = getEnd(child);
          if (end != SourcePosition.UNKNOWN) {
            return end;
          }
        }

        return SourcePosition.UNKNOWN;
      }

      private final void addTokensForReplacement(final Node<?> node) {
        if (node instanceof NonTerminalNode) {
          addTokensForReplacement((NonTerminalNode) node);
        } else {
          assert (node instanceof TerminalNode);
          addTokensForReplacement((TerminalNode) node);
        }
      }

      private final void addTokensForReplacement(final NonTerminalNode node) {
        if (replacements == null) {
          return;
        }

        if (isNullableListItem(node, removedNodes)) {
          return;
        }

        addTokensForReplacement(node, getReplacement(node));
      }

      private final void addTokensForReplacement(final TerminalNode node) {
        if (replacements == null) {
          return;
        }

        addTokensForReplacement(node, getReplacement(node));
      }

      private final void addTokensForReplacement(final Node<?> node,
          final List<Token> replacement) {
        if (replacement == null || replacement.isEmpty()) {
          return;
        }

        final SourcePosition begin;
        final SourcePosition end;
        {
          if (TokenJoiner.this.tryFormat) {
            begin = getBegin(node);
            end = getEnd(node);
          } else {
            begin = SourcePosition.UNKNOWN;
            end = SourcePosition.UNKNOWN;
          }
        }

        for (final Token token : replacement) {
          final Token clonedToken = token.clone(begin, end);
          tokens.add(clonedToken);
        }
      }

      private final List<Token> getReplacement(final Node<?> node) {
        if (replacements == null) {
          return null;
        }

        assert (node.getExpectedSymbol() != null);
        return replacements.getOrDefault(node.getExpectedSymbol(), null);
      }

      private final String getReplacementText(final Node<?> node) {
        final List<Token> replacement = getReplacement(node);
        return join(replacement);
      }

      private final boolean isNullableListItem(final Node<?> node,
          final Set<Node<?>> removedNodes) {
        final Symbol<?> symbol = node.getSymbol();

        if (symbol != ParserSymbol.LIST_ITEM) {
          return false;
        }

        assert (node.getParent() != null);
        final Symbol<?> parentSymbol = node.getParent().getSymbol();

        if (parentSymbol == ParserSymbol.OPTIONAL || parentSymbol == ParserSymbol.STAR) {
          return true;
        } else {
          if (parentSymbol == ParserSymbol.PLUS) {
            assert (node.getParent() instanceof NonTerminalNode);
            for (final Node<?> sibling : ((NonTerminalNode) node.getParent()).getChildren()) {
              if (removedNodes == null || !removedNodes.contains(sibling)) {
                return true;
              }
            }
          }

          // all items of the '+' quantified list have been removed
          // but: in order to be syntactically correct, we have to add the replacement for one child
          // (it doesn't matter which item we "keep", since it is replaced anyway)
          return (node.getParent().getChild(0) != node);
        }
      }

      }, null);

    final TokenStream tokenStream = new TokenStream(tokens);
    return join(tokenStream);
  }

  public final Map<Symbol<?>, String> join(final Map<Symbol<?>, List<Token>> tokenSequences) {
    final Map<Symbol<?>, String> joined = new HashMap<>();

    for (final Map.Entry<Symbol<?>, List<Token>> entry : tokenSequences.entrySet()) {
      final Symbol<?> symbol = entry.getKey();
      final List<Token> tokenSequence = entry.getValue();

      joined.put(symbol, join(tokenSequence));
    }

    return joined;
  }

}
