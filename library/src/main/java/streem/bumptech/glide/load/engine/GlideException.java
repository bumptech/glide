package com.bumptech.glide.load.engine;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An exception with zero or more causes indicating why a load in Glide failed. */
// Public API.
@SuppressWarnings("WeakerAccess")
public final class GlideException extends Exception {
  private static final long serialVersionUID = 1L;

  private static final StackTraceElement[] EMPTY_ELEMENTS = new StackTraceElement[0];

  private final List<Throwable> causes;
  private Key key;
  private DataSource dataSource;
  private Class<?> dataClass;
  private String detailMessage;
  @Nullable private Exception exception;

  public GlideException(String message) {
    this(message, Collections.<Throwable>emptyList());
  }

  public GlideException(String detailMessage, Throwable cause) {
    this(detailMessage, Collections.singletonList(cause));
  }

  public GlideException(String detailMessage, List<Throwable> causes) {
    this.detailMessage = detailMessage;
    setStackTrace(EMPTY_ELEMENTS);
    this.causes = causes;
  }

  void setLoggingDetails(Key key, DataSource dataSource) {
    setLoggingDetails(key, dataSource, null);
  }

  void setLoggingDetails(Key key, DataSource dataSource, Class<?> dataClass) {
    this.key = key;
    this.dataSource = dataSource;
    this.dataClass = dataClass;
  }

  /**
   * Sets a stack trace that includes where the request originated.
   *
   * <p>This is an experimental API that may be removed in the future.
   */
  public void setOrigin(@Nullable Exception exception) {
    this.exception = exception;
  }

  /**
   * Returns an {@link Exception} with a stack trace that includes where the request originated (if
   * previously set via {@link #setOrigin(Exception)})
   *
   * <p>This is an experimental API that may be removed in the future.
   */
  @Nullable
  public Exception getOrigin() {
    return exception;
  }

  // No need to synchronize when doing nothing whatsoever.
  @SuppressWarnings("UnsynchronizedOverridesSynchronized")
  @Override
  public Throwable fillInStackTrace() {
    // Avoid an expensive allocation by doing nothing here. Causes should contain all relevant
    // stack traces.
    return this;
  }

  /**
   * Returns a list of causes that are immediate children of this exception.
   *
   * <p>Causes may or may not be {@link GlideException GlideExceptions}. Causes may also not be root
   * causes, and in turn my have been caused by other failures.
   *
   * @see #getRootCauses()
   */
  public List<Throwable> getCauses() {
    return causes;
  }

  /**
   * Returns the list of root causes that are the leaf nodes of all children of this exception.
   *
   * <p>Use this method to do things like look for http exceptions that indicate the load may have
   * failed due to an error that can be retried. Keep in mind that because Glide may attempt to load
   * a given model using multiple different pathways, there may be multiple related or unrelated
   * reasons for a load to fail.
   */
  public List<Throwable> getRootCauses() {
    List<Throwable> rootCauses = new ArrayList<>();
    addRootCauses(this, rootCauses);
    return rootCauses;
  }

  /**
   * Logs all root causes using the given tag.
   *
   * <p>Each root cause is logged separately to avoid throttling. {@link #printStackTrace()} will
   * provide a more succinct overview of why the exception occurred, although it does not include
   * complete stack traces.
   */
  public void logRootCauses(String tag) {
    List<Throwable> causes = getRootCauses();
    for (int i = 0, size = causes.size(); i < size; i++) {
      Log.i(tag, "Root cause (" + (i + 1) + " of " + size + ")", causes.get(i));
    }
  }

  private void addRootCauses(Throwable throwable, List<Throwable> rootCauses) {
    if (throwable instanceof GlideException) {
      GlideException glideException = (GlideException) throwable;
      for (Throwable t : glideException.getCauses()) {
        addRootCauses(t, rootCauses);
      }
    } else {
      rootCauses.add(throwable);
    }
  }

  @Override
  public void printStackTrace() {
    printStackTrace(System.err);
  }

  @Override
  public void printStackTrace(PrintStream err) {
    printStackTrace((Appendable) err);
  }

  @Override
  public void printStackTrace(PrintWriter err) {
    printStackTrace((Appendable) err);
  }

  private void printStackTrace(Appendable appendable) {
    appendExceptionMessage(this, appendable);
    appendCauses(getCauses(), new IndentedAppendable(appendable));
  }

  // PMD doesn't seem to notice that we're allocating the builder with the suggested size.
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  @Override
  public String getMessage() {
    StringBuilder result =
        new StringBuilder(71)
            .append(detailMessage)
            .append(dataClass != null ? ", " + dataClass : "")
            .append(dataSource != null ? ", " + dataSource : "")
            .append(key != null ? ", " + key : "");

    List<Throwable> rootCauses = getRootCauses();
    if (rootCauses.isEmpty()) {
      return result.toString();
    } else if (rootCauses.size() == 1) {
      result.append("\nThere was 1 root cause:");
    } else {
      result.append("\nThere were ").append(rootCauses.size()).append(" root causes:");
    }
    for (Throwable cause : rootCauses) {
      result
          .append('\n')
          .append(cause.getClass().getName())
          .append('(')
          .append(cause.getMessage())
          .append(')');
    }
    result.append("\n call GlideException#logRootCauses(String) for more detail");
    return result.toString();
  }

  // Appendable throws, PrintWriter, PrintStream, and IndentedAppendable do not, so this should
  // never happen.
  @SuppressWarnings("PMD.PreserveStackTrace")
  private static void appendExceptionMessage(Throwable t, Appendable appendable) {
    try {
      appendable.append(t.getClass().toString()).append(": ").append(t.getMessage()).append('\n');
    } catch (IOException e1) {
      throw new RuntimeException(t);
    }
  }

  // Appendable throws, PrintWriter, PrintStream, and IndentedAppendable do not, so this should
  // never happen.
  @SuppressWarnings("PMD.PreserveStackTrace")
  private static void appendCauses(List<Throwable> causes, Appendable appendable) {
    try {
      appendCausesWrapped(causes, appendable);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private static void appendCausesWrapped(List<Throwable> causes, Appendable appendable)
      throws IOException {
    int size = causes.size();
    for (int i = 0; i < size; i++) {
      appendable
          .append("Cause (")
          .append(String.valueOf(i + 1))
          .append(" of ")
          .append(String.valueOf(size))
          .append("): ");

      Throwable cause = causes.get(i);
      if (cause instanceof GlideException) {
        GlideException glideCause = (GlideException) cause;
        glideCause.printStackTrace(appendable);
      } else {
        appendExceptionMessage(cause, appendable);
      }
    }
  }

  private static final class IndentedAppendable implements Appendable {
    private static final String EMPTY_SEQUENCE = "";
    private static final String INDENT = "  ";
    private final Appendable appendable;
    private boolean printedNewLine = true;

    IndentedAppendable(Appendable appendable) {
      this.appendable = appendable;
    }

    @Override
    public Appendable append(char c) throws IOException {
      if (printedNewLine) {
        printedNewLine = false;
        appendable.append(INDENT);
      }
      printedNewLine = c == '\n';
      appendable.append(c);
      return this;
    }

    @Override
    public Appendable append(@Nullable CharSequence charSequence) throws IOException {
      charSequence = safeSequence(charSequence);
      return append(charSequence, 0, charSequence.length());
    }

    @Override
    public Appendable append(@Nullable CharSequence charSequence, int start, int end)
        throws IOException {
      charSequence = safeSequence(charSequence);
      if (printedNewLine) {
        printedNewLine = false;
        appendable.append(INDENT);
      }
      printedNewLine = charSequence.length() > 0 && charSequence.charAt(end - 1) == '\n';
      appendable.append(charSequence, start, end);
      return this;
    }

    @NonNull
    private CharSequence safeSequence(@Nullable CharSequence sequence) {
      if (sequence == null) {
        return EMPTY_SEQUENCE;
      } else {
        return sequence;
      }
    }
  }
}
