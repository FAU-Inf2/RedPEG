package i2.act.reduction.test;

import i2.act.util.ArgumentSplitter;
import i2.act.util.FileUtil;
import i2.act.util.ProcessExecutor;

public final class ExternalTestFunction implements TestFunction {

  private final String[] commandLine;

  private final String resultFileName;

  private final boolean keepSuccessfulPrograms;
  private final boolean keepUnsuccessfulPrograms;

  private final String intermediateFileName;
  private int counter;

  public ExternalTestFunction(final String[] commandLine, final String resultFileName,
      final boolean keepSuccessfulPrograms, final boolean keepUnsuccessfulPrograms) {
    this.commandLine = commandLine;
    this.resultFileName = resultFileName;
    this.keepSuccessfulPrograms = keepSuccessfulPrograms;
    this.keepUnsuccessfulPrograms = keepUnsuccessfulPrograms;

    this.intermediateFileName = FileUtil.prependBeforeFileExtension(this.resultFileName, "test");
    this.counter = 0;
  }

  @Override
  public final boolean test(final String program) {
    FileUtil.writeToFile(program, this.intermediateFileName);

    // execute external command
    final String[] commandLine =
        ArgumentSplitter.appendArgument(this.commandLine, this.intermediateFileName);
    final boolean containsBug = !ProcessExecutor.executeAndCheck(commandLine);

    if (containsBug) {
      // copy to final location if program contains bug (may be overridden again)
      FileUtil.writeToFile(program, this.resultFileName);
    }

    if ((containsBug && this.keepSuccessfulPrograms)
        || (!containsBug && this.keepUnsuccessfulPrograms)) {
      final String keptFileName = FileUtil.prependBeforeFileExtension(
          this.resultFileName, String.format("%04d", this.counter));

      FileUtil.writeToFile(program, keptFileName);
    }

    ++this.counter;

    return containsBug;
  }

  @Override
  public final void cleanup() {
    if (FileUtil.fileExists(this.intermediateFileName)) {
      FileUtil.deleteFile(this.intermediateFileName);
    }
  }

}
