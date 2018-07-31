package datadog.trace.instrumentation.lettuce;

import static datadog.trace.agent.tooling.ClassLoaderMatcher.classLoaderHasClasses;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class LettuceClientInstrumentation extends Instrumenter.Default {

  public static final String PACKAGE = LettuceClientInstrumentation.class.getPackage().getName();

  private static final String[] HELPER_CLASS_NAMES =
      new String[] {
        LettuceReactiveCommandsInstrumentation.class.getPackage().getName()
            + ".LettuceInstrumentationUtil",
        PACKAGE + ".LettuceAsyncBiFunction"
      };

  public LettuceClientInstrumentation() {
    super("lettuce");
  }

  @Override
  public ElementMatcher typeMatcher() {
    return named("io.lettuce.core.RedisClient");
  }

  @Override
  public ElementMatcher<? super ClassLoader> classLoaderMatcher() {
    return classLoaderHasClasses("io.lettuce.core.RedisClient");
  }

  @Override
  public String[] helperClassNames() {
    return HELPER_CLASS_NAMES;
  }

  @Override
  public Map<ElementMatcher, String> transformers() {
    final Map<ElementMatcher, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(isPrivate())
            .and(returns(named("io.lettuce.core.ConnectionFuture")))
            .and(nameStartsWith("connect"))
            .and(nameEndsWith("Async"))
            .and(takesArgument(1, named("io.lettuce.core.RedisURI"))),
        // Cannot reference class directly here because it would lead to class load failure on Java7
        PACKAGE + ".ConnectionFutureAdvice");
    return transformers;
  }
}
