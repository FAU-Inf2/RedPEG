package i2.act.grammargraph.properties.subtree;

import i2.act.packrat.Token;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SubTree {

  private final Symbol<?> symbol;
  private final List<SubTree> children;

  private SubTree(final Symbol<?> symbol, final List<SubTree> children) {
    this.symbol = symbol;
    this.children = children;
  }

  public static final SubTree leafNode(final Symbol<?> symbol) {
    return new SubTree(symbol, new ArrayList<SubTree>());
  }

  public static final SubTree innerNode(final Symbol<?> symbol, final List<SubTree> children) {
    return new SubTree(symbol, children);
  }

  public final Symbol<?> getSymbol() {
    return this.symbol;
  }

  public final SubTree getChild(final int childIndex) {
    return this.children.get(childIndex);
  }

  public final List<SubTree> getChildren() {
    return Collections.unmodifiableList(this.children);
  }

  public final int numberOfChildren() {
    return this.children.size();
  }

  public final int size() {
    int size = 1;

    for (final SubTree child : this.children) {
      size += child.size();
    }

    return size;
  }

  public final int numberOfTerminals() {
    int numberOfTerminals = 0;

    if (this.symbol instanceof LexerSymbol) {
      ++numberOfTerminals;
      assert (this.children.isEmpty());
    } else {
      for (final SubTree child : this.children) {
        numberOfTerminals += child.numberOfTerminals();
      }
    }

    return numberOfTerminals;
  }

  public final List<LexerSymbol> getTerminals() {
    final List<LexerSymbol> terminals = new ArrayList<>();
    return getTerminals(terminals);
  }

  private final List<LexerSymbol> getTerminals(final List<LexerSymbol> terminals) {
    if (this.symbol instanceof LexerSymbol) {
      terminals.add((LexerSymbol) this.symbol);
      assert (this.children.isEmpty());
    } else {
      for (final SubTree child : this.children) {
        child.getTerminals(terminals);
      }
    }

    return terminals;
  }

  protected final void toTokens(final Map<Symbol<?>, List<Token>> replacements,
      final List<Token> tokens) {
    if (replacements.get(this.symbol) != null) {
      final List<Token> replacement = replacements.get(this.symbol);

      for (final Token token : replacement) {
        tokens.add(token.clone());
      }
    } else {
      for (final SubTree child : this.children) {
        child.toTokens(replacements, tokens);
      }
    }
  }

  @Override
  public final int hashCode() {
    return Objects.hash(this.symbol, this.children);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SubTree)) {
      return false;
    }

    final SubTree otherSubTree = (SubTree) other;

    return this.symbol.equals(otherSubTree.symbol)
        && this.children.equals(otherSubTree.children);
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("(");
    builder.append(this.symbol.getName());

    for (final SubTree child : this.children) {
      builder.append(" ");
      builder.append(child.toString());
    }

    builder.append(")");

    return builder.toString();
  }

}
