package i2.act.reduction;

import i2.act.packrat.Lexer;
import i2.act.packrat.cst.Node;
import i2.act.peg.ast.Grammar;
import i2.act.peg.error.InvalidInputException;
import i2.act.reduction.Reducer;
import i2.act.reduction.test.TestFunction;
import i2.act.util.FileUtil;
import i2.act.util.LRUCache;
import i2.act.util.LRUCache.EvictionFixedSize;
import i2.act.util.LRUCache.NoEviction;
import i2.act.util.SafeWriter;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReductionRun {

  public static final boolean INCLUDE_LEXER_TIME = false;
  public static final boolean SKIP_TOKEN_COUNTING_IN_UNSUCCESSFUL = true;

  public static enum Result {

    SUCCESSFUL, NOT_SUCCESSFUL;

    @Override
    public final String toString() {
      return this.name();
    }

  }

  public static final class ReductionStep {

    protected final int index;
    protected final long timestamp;
    protected final long verificationTime;
    protected final int size;
    protected final int tokens;
    protected final Result result;

    public ReductionStep(final int index, final long timestamp, final long verificationTime,
        final int size, final int tokens, final Result result) {
      this.index = index;
      this.timestamp = timestamp;
      this.verificationTime = verificationTime;
      this.size = size;
      this.tokens = tokens;
      this.result = result;
    }

    @Override
    public final String toString() {
      return String.format("%d, %d, %d, %d, %d, %s", this.index, this.timestamp,
          this.verificationTime, this.size, this.tokens, this.result);
    }

  }

  public static final class ReductionIteration {

    protected final int checks;
    protected final long timestamp;
    protected final long timeInTestFunction;

    public ReductionIteration(final int checks, final long timestamp,
        final long timeInTestFunction) {
      this.checks = checks;
      this.timestamp = timestamp;
      this.timeInTestFunction = timeInTestFunction;
    }

  }

  public static interface Filter {

    public boolean keep(final ReductionStep reductionStep);

  }

  public static final Filter ONLY_SUCCESSFUL = new Filter() {

      @Override
      public final boolean keep(final ReductionStep reductionStep) {
        return reductionStep.result == Result.SUCCESSFUL;
      }

  };

  public static enum Verbosity {
    QUIET, MINIMAL, MORE, ALL;

    public final boolean atLeast(final Verbosity verbosity) {
      return ordinal() >= verbosity.ordinal();
    }

    public static Verbosity fromName(final String name) {
      for (final Verbosity verbosity : Verbosity.values()) {
        if (verbosity.name().equals(name)) {
          return verbosity;
        }
      }

      return null;
    }

    public static final String options() {
      return Arrays.stream(Verbosity.values())
          .map(Verbosity::name)
          .collect(Collectors.joining(" | "));
    }

  }

  public static final Verbosity DEFAULT_VERBOSITY = Verbosity.MORE;

  private static final Verbosity VERBOSITY_START_STOP = Verbosity.MINIMAL;
  private static final Verbosity VERBOSITY_SUCCESSFUL = Verbosity.MORE;
  private static final Verbosity VERBOSITY_NOT_SUCCESSFUL = Verbosity.ALL;


  // -----------------------------------------------------------------------------------------------


  private final Node<?> syntaxTree;
  private final Reducer reducer;
  private final TestFunction testFunction;
  private final Lexer lexer;

  private final String iterationResultFileName;

  private Instant timestamp;

  private long startTime;
  private long endTime;
  private long pauseStartTime;

  private long pausedTime;
  private long timeInLexer;
  private long timeInTestFunction;

  private int numberOfChecks;

  private final List<ReductionStep> reductionSteps;
  private final List<ReductionIteration> iterations;

  private LRUCache<String, Boolean> cache;

  private final Verbosity verbosity;

  private int sizeLimit = -1;
  private int checkLimit = -1;
  private int timeLimit = -1;

  private String lastReduction;

  private String abortion = null;

  public ReductionRun(final Node<?> syntaxTree, final Reducer reducer,
      final TestFunction testFunction) {
    this(syntaxTree, reducer, testFunction, null, null, DEFAULT_VERBOSITY);
  }

  public ReductionRun(final Node<?> syntaxTree, final Reducer reducer,
      final TestFunction testFunction, final String iterationResultFileName) {
    this(syntaxTree, reducer, testFunction, iterationResultFileName, null, DEFAULT_VERBOSITY);
  }

  public ReductionRun(final Node<?> syntaxTree, final Reducer reducer,
      final TestFunction testFunction, final String iterationResultFileName,
      final Grammar grammar) {
    this(syntaxTree, reducer, testFunction, iterationResultFileName, grammar, DEFAULT_VERBOSITY);
  }

  public ReductionRun(final Node<?> syntaxTree, final Reducer reducer,
      final TestFunction testFunction,
      final String iterationResultFileName, final Grammar grammar, final Verbosity verbosity) {
    this.syntaxTree = syntaxTree;
    this.reducer = reducer;
    this.testFunction = testFunction;
    this.iterationResultFileName = iterationResultFileName;
    this.verbosity = verbosity;

    this.timeInTestFunction = 0;

    this.reductionSteps = new ArrayList<ReductionStep>();
    this.numberOfChecks = 0;

    this.iterations = new ArrayList<ReductionIteration>();

    if (grammar == null) {
      this.lexer = null;
    } else {
      // TODO "fail-save mode"?
      this.lexer = Lexer.forGrammar(grammar);
    }
  }

  public final void enableCache() {
    this.cache = new LRUCache<String, Boolean>(new NoEviction());
  }

  public final void enableCache(final int maxCacheSize) {
    this.cache = new LRUCache<String, Boolean>(new EvictionFixedSize(maxCacheSize));
  }

  public final void setSizeLimit(final int sizeLimit) {
    this.sizeLimit = sizeLimit;
  }

  public final void setCheckLimit(final int checkLimit) {
    this.checkLimit = checkLimit;
  }

  public final void setTimeLimit(final int timeLimit) {
    this.timeLimit = timeLimit;
  }

  public final boolean test(final String program) {
    assertRunning();

    if (resultInCache(program)) {
      return this.cache.get(program);
    }

    if (this.checkLimit > -1 && this.numberOfChecks >= this.checkLimit) {
      throw new ReductionAborted(String.format("reached check limit (%d)", this.checkLimit));
    }

    final long startTime = System.currentTimeMillis();
    final boolean triggersBug = this.testFunction.test(program);
    final long endTime = System.currentTimeMillis();

    // add to cache and remove obsolete elements
    if (this.cache != null) {
      this.cache.put(program, triggersBug);

      if (triggersBug) {
        this.cache.clear(cachedProgram -> cachedProgram.length() > program.length());
      }
    }

    if (this.timeLimit > -1 && (endTime - this.startTime) >= this.timeLimit) {
      throw new ReductionAborted(String.format("reached time limit (%d)", this.timeLimit));
    }

    final long verificationTime = endTime - startTime;

    this.timeInTestFunction += verificationTime;

    ++this.numberOfChecks;

    final Result result = (triggersBug) ? Result.SUCCESSFUL : Result.NOT_SUCCESSFUL;

    final int size = program.length();
    final int tokens = numberOfTokens(program, result);

    addStep(size, tokens, verificationTime, result);

    if (triggersBug) {
      this.lastReduction = program;

      if (this.sizeLimit > -1 && size <= this.sizeLimit) {
        throw new ReductionAborted(String.format("reached size limit (%d)", this.sizeLimit));
      }
    }

    return triggersBug;
  }

  public final String start() {
    this.lastReduction = this.syntaxTree.print();
    final int originalSize = this.lastReduction.length();

    this.timestamp = Instant.now();
    this.startTime = System.currentTimeMillis();

    final int originalTokens = numberOfTokens(this.lastReduction, Result.SUCCESSFUL);

    addStep(originalSize, originalTokens, 0, Result.SUCCESSFUL);

    if (this.verbosity.atLeast(VERBOSITY_START_STOP)) {
      printMessage(0, "start reduction with '%s' reducer (%d)",
          this.reducer.getName(), originalSize);
    }

    String result;

    try {
      result = this.reducer.reduce(this.syntaxTree, this);
    } catch (final ReductionAborted aborted) {
      result = this.lastReduction;

      this.abortion = aborted.getMessage();

      if (this.verbosity.atLeast(VERBOSITY_START_STOP)) {
        final long timestamp = getTimestamp();
        printMessage(timestamp, "ABORTED: %s", aborted.getMessage());
      }
    } finally {
      stop();
    }

    return result;
  }

  public final void pause() {
    assertRunning();
    assert (this.pauseStartTime == 0) : "reduction run already paused";

    this.pauseStartTime = System.currentTimeMillis();
  }

  public final void resume() {
    assert (this.endTime == 0) : "reduction run already stopped";
    assert (this.pauseStartTime != 0) : "reduction run not paused";

    final long pauseEndTime = System.currentTimeMillis();
    this.pausedTime += (pauseEndTime - this.pauseStartTime);

    this.pauseStartTime = 0;
  }

  private final void stop() {
    if (this.pauseStartTime != 0) {
      resume();
    }

    this.endTime = System.currentTimeMillis();
    this.testFunction.cleanup();

    if (this.verbosity.atLeast(VERBOSITY_START_STOP)) {
      final long timestamp = getDuration();
      printMessage(timestamp, "reduction finished:");

      final int originalSize = getOriginalSize();
      final int reducedSize = getReducedSize();

      printMessage(timestamp, "~~ %d => %d characters (%.2f %%)",
          originalSize, reducedSize,
          ((double) originalSize - reducedSize) / originalSize * 100);

      final int numberOfChecks = getNumberOfChecks();
      final int numberOfReductions = getNumberOfReductions();

      printMessage(timestamp, "~~ %d checks, %d successful reductions (%.2f %%)",
          numberOfChecks, numberOfReductions,
          ((double) numberOfReductions) / numberOfChecks * 100);

      printMessage(timestamp, "~~ %d ms (%.2f %%) in test function",
          this.timeInTestFunction,
          ((double) this.timeInTestFunction) / timestamp * 100);
    }
  }

  public final void finishIteration() {
    assertStarted();

    final int checks = this.numberOfChecks;
    final long timestamp = getTimestamp();
    final long timeInTestFunction = this.timeInTestFunction;

    final ReductionIteration iteration =
        new ReductionIteration(checks, timestamp, timeInTestFunction);
    this.iterations.add(iteration);

    if (this.verbosity.atLeast(VERBOSITY_START_STOP)) {
      final int iterationChecks;
      final long iterationTime;
      final long iterationTimeInTestFunction;
      {
        final int numberOfIterations = this.iterations.size();
        assert (numberOfIterations > 0);

        if (numberOfIterations == 1) {
          iterationChecks = checks;
          iterationTime = timestamp;
          iterationTimeInTestFunction = timeInTestFunction;
        } else {
          final ReductionIteration previousIteration = this.iterations.get(numberOfIterations - 2);

          iterationChecks = checks - previousIteration.checks;
          iterationTime = timestamp - previousIteration.timestamp;
          iterationTimeInTestFunction = timeInTestFunction - previousIteration.timeInTestFunction;
        }
      }

      printMessage(timestamp, "iteration finished (%5d checks, %6d ms, %6d ms in test function)",
          iterationChecks, iterationTime, iterationTimeInTestFunction);
    }

    if (this.iterationResultFileName != null) {
      // write result to disk
      assert (this.lastReduction != null);

      final int numberOfIterations = this.iterations.size();

      final String keptFileName = FileUtil.prependBeforeFileExtension(
          this.iterationResultFileName, String.format("%04d", numberOfIterations));

      FileUtil.writeToFile(this.lastReduction, keptFileName);
    }
  }

  private final int numberOfTokens(final String program, final Result result) {
    if (this.lexer == null
        || (result == Result.NOT_SUCCESSFUL && SKIP_TOKEN_COUNTING_IN_UNSUCCESSFUL)) {
      return -1;
    }

    final long timeBeforeLexer = System.currentTimeMillis();

    try {
      return this.lexer.lex(program).numberOfTokens();
    } catch (final InvalidInputException exception) {
      // unable to lex program
      return -1;
    } finally {
      if (!INCLUDE_LEXER_TIME) {
        final long timeAfterLexer = System.currentTimeMillis();
        this.timeInLexer += (timeAfterLexer - timeBeforeLexer);
      }
    }
  }

  private final void printMessage(final String formatString,
      final Object... formatArguments) {
    printMessage(-1, formatString, formatArguments);
  }

  private final void printMessage(final long timestamp, final String formatString,
      final Object... formatArguments) {
    if (timestamp == -1) {
      System.err.format(Locale.US, "                ");
    } else {
      System.err.format(Locale.US, "[i] [%5d.%03d] ", timestamp / 1000, timestamp % 1000);
    }

    System.err.format(Locale.US, formatString + "\n", formatArguments);
  }

  private final void assertStarted() {
    assert (this.startTime != 0) : "reduction run not started";
  }

  private final void assertRunning() {
    assertStarted();
    assert (this.endTime == 0) : "reduction run already stopped";
    assert (this.pauseStartTime == 0) : "reduction run paused";
  }

  private final void assertStopped() {
    assertStarted();
    assert (this.endTime != 0) : "reduction run not stopped yet";
  }

  public final int getOriginalSize() {
    assertStarted();
    return this.reductionSteps.get(0).size;
  }

  public final int getReducedSize() {
    assertStopped();

    final ListIterator<ReductionStep> listIterator =
        this.reductionSteps.listIterator(this.reductionSteps.size());
    while (listIterator.hasPrevious()) {
      final ReductionStep reductionStep = listIterator.previous();

      if (reductionStep.result == Result.SUCCESSFUL) {
        return reductionStep.size;
      }
    }

    return getOriginalSize();
  }

  public final long getDuration() {
    assertStopped();
    return this.endTime - this.startTime - this.timeInLexer - this.pausedTime;
  }

  public final int getNumberOfChecks() {
    return this.numberOfChecks;
  }

  public final int getNumberOfReductions() {
    assertStarted();

    int numberOfReductions = 0;

    for (final ReductionStep reductionStep : this.reductionSteps) {
      if (reductionStep != this.reductionSteps.get(0)
          && reductionStep.result == Result.SUCCESSFUL) {
        ++numberOfReductions;
      }
    }

    return numberOfReductions;
  }

  public final long getTimeInTestFunction() {
    return this.timeInTestFunction;
  }

  private final long getTimestamp() {
    assertStarted();
    return System.currentTimeMillis() - this.startTime - this.timeInLexer - this.pausedTime;
  }

  private final void addStep(final int size, final int tokens, final long verificationTime,
      final Result result) {
    final long timestamp = getTimestamp();

    final int index = this.reductionSteps.size();
    final ReductionStep reductionStep =
        new ReductionStep(index, timestamp, verificationTime, size, tokens, result);

    this.reductionSteps.add(reductionStep);

    if (this.reductionSteps.size() > 1
        && result == Result.SUCCESSFUL && this.verbosity.atLeast(VERBOSITY_SUCCESSFUL)) {
      if (tokens > -1) {
        printMessage(timestamp, "successful reduction (%8d bytes, %8d tokens)", size, tokens);
      } else {
        printMessage(timestamp, "successful reduction (%8d bytes)", size);
      }
    }

    if (result == Result.NOT_SUCCESSFUL && this.verbosity.atLeast(VERBOSITY_NOT_SUCCESSFUL)) {
      printMessage(timestamp, "unsuccessful reduction");
    }
  }

  public final void writeAsCSV(final String fileName) {
    writeAsCSV(fileName, null);
  }

  public final void writeAsCSV(final String fileName, final Filter filter) {
    writeAsCSV(new File(fileName), filter);
  }

  public final void writeAsCSV(final File file) {
    writeAsCSV(file, null);
  }

  public final void writeAsCSV(final File file, final Filter filter) {
    final SafeWriter writer = SafeWriter.openFile(file);

    for (final ReductionStep reductionStep : this.reductionSteps) {
      if (filter == null || filter.keep(reductionStep)) {
        writer.write("%s\n", reductionStep);
      }
    }

    writer.close();
  }

  public final void writeAsJSON(final String fileName,
      final Map<String, Object> configurationOptions) {
    writeAsJSON(fileName, configurationOptions, null);
  }

  public final void writeAsJSON(final String fileName,
      final Map<String, Object> configurationOptions, final Filter filter) {
    writeAsJSON(new File(fileName), configurationOptions, filter);
  }

  public final void writeAsJSON(final File file, final Map<String, Object> configurationOptions) {
    writeAsJSON(file, configurationOptions, null);
  }

  public final void writeAsJSON(final File file, final Map<String, Object> configurationOptions,
      final Filter filter) {
    final SafeWriter writer = SafeWriter.openFile(file);

    writer.write("{\n");

    writer.write("\t\"timestamp\": \"%s\",\n", this.timestamp);
    writer.write("\t\"reducer\": \"%s\",\n", this.reducer.getName());

    // configuration
    if (configurationOptions != null) {
      writer.write("\t\"configuration\": {");

      boolean first = true;

      for (final Map.Entry<String, Object> configurationOption : configurationOptions.entrySet()) {
        if (first) {
          writer.write("\n");
          first = false;
        } else {
          writer.write(",\n");
        }

        final String option = configurationOption.getKey();
        final Object argument = configurationOption.getValue();

        if (argument instanceof Boolean) {
          writer.write("\t\t\"%s\": %s", option, argument);
        } else {
          writer.write("\t\t\"%s\": \"%s\"", option, argument);
        }
      }

      writer.write("\n\t},\n");
    }

    // steps
    {
      writer.write("\t\"steps\": [");

      boolean first = true;

      for (final ReductionStep reductionStep : this.reductionSteps) {
        if (filter == null || filter.keep(reductionStep)) {
          if (first) {
            writer.write("\n");
            first = false;
          } else {
            writer.write(",\n");
          }

          writer.write("\t\t{\n");

          writer.write("\t\t\t\"index\": %d,\n", reductionStep.index);
          writer.write("\t\t\t\"timestamp\": %d,\n", reductionStep.timestamp);
          writer.write("\t\t\t\"verificationTime\": %d,\n", reductionStep.verificationTime);
          writer.write("\t\t\t\"size\": %d,\n", reductionStep.size);
          writer.write("\t\t\t\"tokens\": %d,\n", reductionStep.tokens);
          writer.write("\t\t\t\"result\": \"%s\"\n", reductionStep.result);

          writer.write("\t\t}");
        }
      }

      writer.write("\n\t],\n");
    }

    // iterations
    {
      writer.write("\t\"iterations\": [");

      boolean first = true;

      for (final ReductionIteration iteration : this.iterations) {
        if (first) {
          writer.write("\n");
          first = false;
        } else {
          writer.write(",\n");
        }

        writer.write("\t\t{\n");

        writer.write("\t\t\t\"checks\": %d,\n", iteration.checks);
        writer.write("\t\t\t\"timestamp\": %d,\n", iteration.timestamp);
        writer.write("\t\t\t\"timeInTestFunction\": %d\n", iteration.timeInTestFunction);

        writer.write("\t\t}");
      }

      writer.write("\n\t],\n");
    }

    if (this.abortion != null) {
      writer.write("\t\"aborted\": \"%s\",\n", this.abortion);
    }

    writer.write("\t\"totalTime\": %d,\n", getDuration());

    final long wallTime = this.endTime - this.startTime;
    writer.write("\t\"wallTime\": %d,\n", wallTime);

    writer.write("\t\"timeInTestFunction\": %d,\n", this.timeInTestFunction);

    writer.write("\t\"numberOfChecks\": %d\n", this.numberOfChecks);

    writer.write("}\n");

    writer.close();
  }

  private final boolean resultInCache(final String program) {
    return this.cache != null && this.cache.containsKey(program);
  }

}
