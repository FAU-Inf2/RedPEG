# RedPEG: A Framework for Test Case Reduction

A program that triggers a bug in a compiler, interpreter, or other language processor often contains
code that is irrelevant for the bug. Such irrelevant code often complicates the debugging, because
it can conceal the root cause of the bug. Automated test case reduction techniques try to remove as
much of this irrelevant code as possible to help the developers in debugging.

This repository contains efficient implementations of the most important (language-agnostic,
syntactical) test case reduction techniques from the scientific literature and is also meant to
serve as a basis for the development of future techniques. It accompanies the research paper "P.
Kreutzer, T. Kunze, M. Philippsen: Test Case Reduction: A Framework, Benchmark, and Comparative
Study" published at ICSME'21, but is also meant to be used for practical purposes.

The input format of a test case is specified by means of a *Parsing Expression Grammar* (PEG). The
respective parser is constructed "on the fly" during runtime (i.e., there is no need to translate
the grammar beforehand). This reduces dependencies and should simplify the use of *RedPEG* in
practice.


## Citations

The *RedPEG* framework contains implementations of the following test case reduction techniques
published in the scientific literature:

- Zeller, A., Hildebrandt, R.: Simplifying and Isolating Failure-Inducing Input. In: IEEE
  Transactions on Software Engineering. 28(2) (Feb. 2002) 183–200.
- Misherghi, G., Su, Z.: HDD: Hierarchical Delta Debugging. In: ICSE’06: International Conference on
  Software Engineering (Shanghai, China, May 2006), 142–151.
- Hodován, R., Kiss, A., Gyimóthy, T.: Coarse Hierarchical Delta Debugging. In: ICSME’17:
  International Conference on Software Maintenance and Evolution (Shanghai, China, Sep. 2017),
  194–203.
- Herfert, S., Patra, J., Pradel, M.: Automatically Reducing Tree-Structured Test Inputs. In:
  ASE’17: International Conference on Automated Software Engineering (Urbana-Champaign, IL, Oct.
  2017), 861–871.
- Sun, C., Li, Y., Zhang, Q., Gu, T., Su, Z.: Perses: Syntax-Guided Program Reduction. In: ICSE’18:
  International Conference on Software Engineering (Gothenburg, Sweden, May 2018), 361–371.
- Gharachorlu, G., Sumner, N.: Avoiding the Familiar to Speed Up Test Case Reduction. In: QRS’18:
  International Conference on Software Quality, Reliability and Security (Lisbon, Portugal, Jul.
  2018), 426–437.
- Kiss, Á., Hodován, R., Gyimóthy, T.: HDDr: A Recursive Variant of the Hierarchical Delta Debugging
  Algorithm. In: A-TEST’18: Automating Test Case Design, Selection, and Evaluation (Lake Buena
  Vista, FL, Nov. 2018), 16–22.
- Gharachorlu, G., Sumner, N.: Pardis: Priority Aware Test Case Reduction. In: FASE’19: Fundamental
  Approaches to Software Engineering (Prague, Czech Republic, Apr. 2019), 409–426.

If you want to cite *RedPEG*, please cite our ICSME'21 research paper:

- *to appear*


## Building RedPEG

We use our own parser implementation to convert an input program to its syntax tree (this allowed us
to design the syntax trees in a way that supports all examined reducers). Since this parser
implementation might also be valuable in other contexts, we moved all code related to parsing to its
own library called [j-PEG](https://github.com/FAU-Inf2/j-PEG), which *RedPEG* includes as a Git
submodule. Use one of the following ways to correctly set up this submodule:

- When cloning the *RedPEG* repository, add the command line option `--recurse-submodules`.
- If you already cloned the *RedPEG* repository without the aforementioned command line option,
  simply type `git submodule update --init` to set up the submodule.

When the submodule is correctly set up, simply type `./gradlew build` in the root directory of the
*RedPEG* repository to build the *RedPEG* framework. After a successful build, there should be a
file `build/libs/RedPEG.jar`. The instructions below assume that this file exists.

**Note**:

- You need a working JDK installation to build and run *RedPEG* (tested with OpenJDK 8 and 11).
- Building *RedPEG* requires an internet connection to resolve external dependencies.


## Example Reduction

Once *RedPEG* has been built successfully (see above), you can run the following example to check if
everything works correctly.

Assume that the C program in `example/program.c` provokes a bug in a C compiler, which is triggered
when a single statement uses the two literal numbers `13` and `3` (a really contrived example).
Further assume that the bug is only triggered if the input program is statically valid (i.e., if it
passes all analyses in the front end of the compiler). (Note: this program has been generated with
the [StarSmith](https://github.com/FAU-Inf2/StarSmith) compiler fuzzer.)

The test script `example/test.sh` simulates this "bug" and tells *RedPEG* whether a reduction
candidate still triggers the bug. It first calls `gcc` to check if the given reduction candidate is
compilable, i.e., if it is still statically valid (note: this requires a (more or less arbitrary)
version of `gcc` in the search path). If the candidate is *not* compilable, the test script returns
with the exit code `0` to signal that the bug is not triggered. If, however, the candidate is
compilable, the test script uses `grep` to check if there is still a statement that uses the
literals `13` and `3`. If there is such a statement, the test script returns with exit code `1` to
signal that the bug has been triggered.

To check that the original program in `example/program.c` really triggers the "bug", you can run the
following command and check that it returns with exit code `1`:


    ./example/test.sh ./example/program.c
    echo $?


To reduce the example program to a smaller one that still triggers the "bug", run the following:


    ./run.sh \
      --grammar grammars/c.txt \
      --in example/program.c \
      --test example/test.sh \
      --reduce 'Perses*' \
      --outDir example/reduced \
      --cache


This example reduction should only take few seconds, after which the final reduction result can be
found in `example/reduced/program.reduced.c`:


     main(void) {13 |3;
    }


In short, the following command line options are used in this example:

- `--grammar`: Path to a PEG that describes the input format of the program that should be reduced.
  This option is required.
- `--in`: Path to the program that should be reduced. This option is required.
- `--test`: Path to the test function that checks whether a reduction candidate still triggers the
  bug. This option is required.
- `--reduce`: The reducer that should be used. `Perses*` is the fixpoint variant of the highly
  effective *Perses* reducer by Sun et al. This option is required.
- `--outDir`: The directory to which the reduction result (and all temporary reduction candidates)
  should be written to.
- `--cache`: Enables test outcome caching to avoid unnecessary evaluations of the test script.
  Caching usually accelerates the reduction, but might require a lot of memory.

The following sections give more details on how to use *RedPEG* and what command line options it
offers.


## Specifying the Input Format

The input format of a test case is specified by means of a *Parsing Expression Grammar* (PEG). For
example, consider the following grammar:


    calculation: statement* EOF ;

    statement: ( VAR_NAME ASSIGN )? expression SEMICOLON ;

    expression: add_expression ;

    add_expression: mul_expression ( ( ADD | SUB ) mul_expression )* ;

    mul_expression: factor ( ( MUL | DIV ) factor )* ;

    factor
      : NUM
      | VAR_NAME
      | LPAREN expression RPAREN
      ;

    ASSIGN: ':=';
    SEMICOLON: ';';

    ADD: '+';
    SUB: '-';
    MUL: '*';
    DIV: '/';

    LPAREN: '(';
    RPAREN: ')';

    @skip
    SPACE: ( ' ' | '\n' | '\r' | '\t' )+ ;

    NUM: '0' | ( [1-9][0-9]* ) ;
    VAR_NAME: [a-zA-Z] [a-zA-Z0-9]* ;


This grammar matches programs of the following form:


    foo := (13 + 3) * 12;
    11 + foo;


### Lexer Rules

The names of lexer rules consist of upper case letters (e.g., `ASSIGN` or `SPACE`).

The right hand side of a lexer rule consists of a single regular expression (please refer to the
example above for the exact notation).

If a lexer rule is annotated with `@skip` (e.g., the `SPACE` rule from above), its matches are
discarded (i.e., its matches do not appear as tokens in the token stream).

The implicitly defined `EOF` rule matches the end of the input.

### Parser Rules

The names of parser rules consist of lower case letters (e.g., `calculation` or `statement`). The
first parser rule becomes the parser's start rule.

The right hand side of a parser rule uses a notation similar to that of EBNF:

- `*` means zero or more repetitions
- `+` means one or more repetitions
- `?` means zero or one occurrences

Use the choice operator `|` to specify alternatives. **Note**: In contrast to "traditional"
context-free grammars in EBNF, the choice operator of PEGs is *ordered*. Thus, if the first
alternative matches, the second one is ignored. For example, a rule `foo: A | A B;` only ever
matches `A` tokens (but you can swap the alternatives so that the parser first tries to match `A
B`).

### Left Recursion

Since the input PEG is converted to a recursive top-down parser, left recursive rules may lead to an
infinite recursion. We provide **experimental** support for *directly* left recursive rules, but
this has not been fully tested yet. *Indirectly* left recursive rules are currently not supported.

### Annotations

It is possible to annotate the rules for terminals and non-terminals in a PEG with one or more
*annotations*, and these annotations might optionally carry an (integer or string) argument:

    @some_annotation
    foo: ... ;

    @int_annotation(13)
    @string_annotation("value")
    bar: ... ;

Nodes in the syntax tree (see below) that belong to such an annotated definition carry these
annotations (and their optional values) for further processing.

### Grammar Graphs

An input grammar induces a bipartite graph that we call "grammar graph". Its nodes either represent
alternatives (so-called *alternative nodes*) or sequences (so-called *sequence nodes*). Each
alternative node has outgoing edges to all its alternative sequences; each sequence node has
outgoing edges to all alternatives that the sequence consists of.

To show a graphical representation of a grammar graph, use the following:

    ./run.sh --in path/to/test_case --grammar path/to/grammar --printGG | dot -Tx11

This requires the `dot` tool from the Graphviz package.

**Note**: The grammar graph is independent from a concrete input test case. However, the command
line interface currently requires the specification of an input test case (this may be fixed in a
later version).


## Syntax Trees and Parser Options

Most reduction algorithms work on the syntax tree of the input test case (which is induced by the
input grammar). To control the shape of the syntax tree, the following command line options are
supported:

- `--omitQuantifiers`: By default, quantifiers in the grammar rules (i.e., `*`, `+`, and `?`) lead
  to quantifier nodes in the syntax tree. For example, if a `*`-ed sub-rule is matched, a `*`-node
  is added to the syntax tree. Its children are `ITEM` nodes, one for each repetition. Most of the
  provided reduction algorithms use these quantifier nodes to detect list structures in the syntax
  tree. By specifying the `--omitQuantifiers` option, quantifier and `ITEM` nodes are omitted.
- `--noCompactify`: For certain grammars and inputs, the respective syntax trees may contain long
  "chains" of nodes (i.e., nodes that only have a single child node). By default, these chains are
  "squeezed". This reduces the number of nodes in the syntax tree and, in general, improves the
  efficiency of the reduction algorithms. Specifying `--noCompactify` disables this behavior (this
  is *not* recommended for typical use cases!).

To show a graphical representation of the input test case's syntax tree, use the following:

    ./run.sh --in path/to/test_case --grammar path/to/grammar --toDot | dot -Tx11

This requires the `dot` tool from the Graphviz package.

Use the `--treeStats` command line option to print statistics for the input test case's syntax tree.


## Replacements

Some of the reduction algorithms (e.g., HDD) delete nodes from the syntax tree and replace them with
minimal textual replacements. By default, these replacements are automatically computed from the
input grammar. Use the `--printReplacements` option to print these replacements.

In certain cases, it might be favorable to specify the replacements for some grammar rules manually.
For example, the automatically computed minimal replacement for the `expression` rule from our
example grammar may be the string `a` (a variable name). If there is no variable named `a` in the
input test case, this replacement may lead to many semantically invalid reduction candidates and
hence hamper the reduction. Instead, replacing `expression`s with `0`s may lead to more efficient
and effective reductions. To manually specify this replacement, create a file with the following
content:

    expression: "0";

Use the `--replacements` option and provide the path to this file as argument to override the
automatically computed replacement for the `expression` rule. Note that the new replacement for
`expression` is also used for the automatic computation of minimal replacements for all other rules
that make use of the `expression` rule. For example, using `0` as replacement for the `expression`
rule, the minimal replacement for the `statement` rule becomes `0;` (instead of `a;`).


## Input Reduction

To actually reduce the input test case, at least provide the `--reduce` and `--test` options:

    ./run.sh --in path/to/test_case --grammar path/to/grammar --reduce ALGORITHM --test TEST_FUNCTION

The `--reduce` option specifies which reduction algorithm should be used. The most important
reducers that *RedPEG* offers are: `HDD`, `CoarseHDD`, `HDDr`, `GTR`, `Perses`, and `Pardis` (see
above for references to their respective publications). The source file
`src/main/java/i2/act/reduction/ReducerFactory.java` contains a list of all provided algorithms and
their respective names.

Note that most reducers (including the ones listed above) also offer a fixpoint mode, in which the
reduction algorithm is executed multiple times until the result no longer changes. Append a `*` to
the name of a reducer to run it in fixpoint mode.

You can also specify a "reduction pipeline", i.e., a list of reducers that should be applied one
after another. For this purpose, simply separate the reducer names with a `|` character. For
example, to run `CoarseHDD*` followed by `HDD`, pass `--reduce 'CoarseHDD*|HDD'` to the `run.sh`
script. If all reducers should be repeatedly run until a fixpoint is reached, simply append a `|*`
to the list of reducer names (e.g., `--reduce 'CoarseHDD*|HDD|*'`).

The `--test` option takes as argument a command line that should be executed to check if a reduction
candidate triggers a bug. For this purpose, the path to a file containing the current reduction
candidate is appended as additional command line option to the specified command line. In most
cases, the command line simply consists of the path to a shell script that implements the test
function. The test function should return `1` if the reduction candidate triggers the bug (and `0`
otherwise).

In one way or another, most reducers remove tokens from the input program and join the remaining
tokens to a new program. In some cases, naively joining the tokens may lead to syntactically invalid
reduction candidates which are often rejected by the test function (e.g., two neighboring
identifiers may be incorrectly merged to a single one). Our serialization mechanism therefore tries
to find out if neighboring tokens have to be separated. By default, this mechanism uses a single
space character as separator (this is a valid token separator in most programming languages); the
command line option `--join` can be used to specify a different separator string.

If the `--tryFormat` command line option is given, our serialization mechanism tries to improve the
formatting of the reduced programs by introducing additional line breaks. Note that this feature is
**experimental** and may even lead to syntactically invalid reduction candidates in some cases.

### Specifying the Output Path

By default, the reduction result is written to the same directory as the input program, i.e., if the
input file name is `path/to/input.txt`, the result is written to `path/to/input.reduced.txt`. Note
that "on-the-fly" minimization is supported, i.e., as soon as a reduction step is successful, the
reduced test case is available in the output file (but may be overridden in a later step). This
allows you to cancel the reduction at any time, i.e., you do not have to wait until the reduction is
finished.

Use the `--out` option to manually specify the file name of the output file.

Alternatively, use the `--outDir` option to specify the directory that the result should be written
to. The file name inside the directory matches that of the input file, but the string `reduced` is
prepended before its file extension.

By default, only a single reduction candidate is kept on disk (the last successful reduction
candidate). Use `--keepSuccessful` to keep all successful reduction candidates, `--keepUnsuccessful`
to keep all unsuccessful reduction candidates, and `--keepAll` to keep all reduction candidates.

By providing the `--statsCSV` option and a file name as argument, a CSV file containing statistics
about the reduction is written to disk.

By providing the `--statsJSON` option and a file name as argument, a JSON file containing statistics
about the reduction is written to disk.

Add the `--countTokens` option to include the number of tokens of each reduction candidate in the
statistics. Note that counting the tokens takes some time (depending on the size of the reduction
candidate, this may range from few milliseconds to multiple seconds). To not distort the time
measurements, we do not include the time for counting the tokens in the measurements.

### Specifying a List Reduction

Most of the provided reduction algorithms handle list structures specially and apply a list
reduction algorithm to such structures. By default, these algorithms use their respective original
list reduction algorithm for this purpose, but you can manually specify the list reduction algorithm
with the `--listReduction` option. The two most important list reduction algorithms that *RedPEG*
offers are `DDMin` and `OPDD` (see above for references to their respective publications), but
*RedPEG* also offers some variants of them. See the file
`src/main/java/i2/act/reduction/lists/ListReductionFactory.java` for all possible list reduction
algorithms and their names.

### Specifying Limits

You can specify limits to abort the reduction prematurely:

- `--sizeLimit`: Abort the reduction once the size of the reduced input (in bytes) is smaller than
  the provided value.
- `--checkLimit`: Abort the reduction after the provided number of checks.
- `--timeLimit`: Abort the reduction after the provided time (in ms).

You can also specify more than one limit.

### Caching Test Results

In some cases, reduction algorithms generate the exact same reduction candidates multiple times. By
default, the test function is executed once per candidate, but you can specify the `--cache` option
to cache the result of a call of the test function. Note that this may require a lot of memory in
certain cases.

### Verbosity

By default, each successful reduction step is logged to stderr. To increase or decrease the level of
output, specify the `--verbosity` command line option.


## Stripping Tokens from a Program

In certain cases, it may be useful to first strip some tokens from the input program before starting
the actual reduction, e.g., to remove unnecessary comments. For this purpose, the framework includes
a helper script `strip.sh` that is invoked as follows:

    ./strip.sh --in path/to/input --grammar path/to/grammar --out path/to/output --strip TOKENS

Here, `TOKENS` is a comma separated list of token names (as defined in the input grammar). All
tokens that match one of the specified ones are removed from the input.

The helper script also accepts the command line options `--join` and `--tryFormat` (see above).


## Extending RedPEG with a Reduction Algorithm

The *RedPEG* framework can easily be extended with new reduction algorithms. To do so, simply add a
new class for the reducer and let it implement the interface `i2.act.reduction.Reducer`. Afterwards,
register the new reducer under the desired name(s) in `i2.act.reduction.ReducerFactory`. You can
then choose the new reducer with the `--reduce` command line option (see above).


## Extending RedPEG with a List Reduction Algorithm

As explained above, most reducers use a list reduction algorithm under the hood to reduce list
structures in the syntax tree and *RedPEG* allows the combination of any reducer with any list
reduction algorithm. To extend *RedPEG* with a new list reduction algorithm, simply implement the
interface `i2.act.reduction.lists.ListReduction` and register the new list reduction algorithm under
the desired name in `i2.act.reduction.lists.ListReductionFactory`. The new list reduction algorithm
can then be chosen with the `--listReduction` command line option (see above).


## License

*RedPEG* is licensed under the terms of the MIT license (see [LICENSE.mit](LICENSE.mit)).

*RedPEG* makes use of the following open-source projects:

- Gradle (licensed under the terms of Apache License 2.0)
