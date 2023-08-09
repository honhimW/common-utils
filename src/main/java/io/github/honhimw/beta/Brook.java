package io.github.honhimw.beta;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 容忍非运行时异常和无返回值操作的类{@link Optional}操作
 * @author hon_him
 * @since 2022-07-28
 */
@SuppressWarnings("unused")
@Slf4j
public final class Brook<O> {

    @FunctionalInterface
    public interface Fun<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface Sup<R, E extends Exception> {
        R get() throws E;
    }

    @FunctionalInterface
    public interface Con<T, E extends Exception> {
        void accept(T t) throws E;
    }


    @FunctionalInterface
    public interface BiFun<T, U, R, E extends Exception> {
        R apply(T t, U u) throws E;
    }

    public static <T> Brook<T> with(T value) {
        return new Brook<>(value, null);
    }

    @SuppressWarnings("all")
    public static <T> Brook<T> fromOptional(Optional<T> optional) {
        Objects.requireNonNull(optional);
        return new Brook<>(optional.orElse(null), null);
    }

    public static <E extends Exception> void runThrow(E e) throws E {
        throw e;
    }
    public static <R, E extends Exception> R callThrow(E e) throws E {
        throw e;
    }

    private final O value;

    private final Exception exception;

    private Brook(O value, Exception exception) {
        this.value = value;
        this.exception = exception;
        if (log.isTraceEnabled()) {
            if (Objects.nonNull(value)) {
                log.trace("Brook with Value: {}", value);
            } else if (Objects.nonNull(exception)) {
                log.trace("Brook with Error: {}", exception.toString());
            }
        }
    }

    private <R> Brook<R> construct(R value, Exception exception) {
        return new Brook<>(value, exception);
    }

    private <R> Brook<R> constructNext(R value) {
        return new Brook<>(value, this.exception);
    }

    private <R> Brook<R> constructError(Exception error) {
        return new Brook<>(null, error);
    }

    private Brook<O> setContext(Object ctx) {
        return new Brook<>(this.value, this.exception);
    }

    public <R, E extends Exception> Brook<R> map(Fun<O, R, E> fun) {
        Objects.requireNonNull(fun);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                return constructNext(fun.apply(value));
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <R, E extends Exception> Brook<R> flatMap(Fun<O, Brook<R>, E> fun) {
        Objects.requireNonNull(fun);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                return fun.apply(value);
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <R, E extends Exception> Brook<R> mapAndThrow(Fun<O, R, Exception> fun, Fun<Exception, E, E> boxErr) throws E {
        Objects.requireNonNull(fun);
        Objects.requireNonNull(boxErr);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                return constructNext(fun.apply(value));
            } catch (Exception e) {
                throw boxErr.apply(e);
            }
        }
    }

    /**
     * 条件不满足会丢失数据，配合{@link #fissionFuns(Fun[])}}
     */
    public <R, E extends Exception> Brook<R> mapWhen(Predicate<O> p, Fun<O, R, Exception> fun) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(fun);
        if (!isPresent()) {
            return empty();
        } else {
            if (p.test(value)) {
                return map(fun);
            } else {
                return empty();
            }
        }
    }

    public Brook<O> filter(Predicate<O> p) {
        Objects.requireNonNull(p);
        if (!isPresent()) {
            return empty();
        } else {
            if (p.test(value)) {
                return this;
            } else {
                return empty();
            }
        }
    }

    public <R, E extends Exception> Brook<R> flatOptional(Fun<O, Optional<R>, E> fun) {
        Objects.requireNonNull(fun);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                return fromOptional(fun.apply(value));
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <R> Brook<R> cast(Class<R> clz) {
        Objects.requireNonNull(clz);
        if (isPresent()) {
            if (clz.isAssignableFrom(value.getClass())) {
                return map(clz::cast);
            }
        }
        return empty();
    }

    public <E extends Exception> Brook<O> exec(Con<O, E> con) {
        Objects.requireNonNull(con);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                con.accept(value);
                return this;
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <E extends Exception> Brook<O> execWhen(Predicate<O> predicate, Con<O, E> con) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(con);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                if (predicate.test(value)) {
                    con.accept(value);
                }
                return this;
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    @SafeVarargs
    public final <E extends Exception> Brook<O> fissionCons(Con<Brook<O>, E>... cons) {
        Objects.requireNonNull(cons);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                for (Con<Brook<O>, E> con : cons) {
                    con.accept(this);
                }
                return this;
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <R, E extends Exception> Brook<R> fission(Fun<Brook<O>, Brook<R>, E> fun) {
        Objects.requireNonNull(fun);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                Brook<R> apply = fun.apply(this);
                return Objects.isNull(apply) ? empty() : apply;
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    /**
     * 可用于类似switch的操作，一旦取得返回值或错误即返回
     */
    @SafeVarargs
    public final <R, E extends Exception> Brook<R> fissionFuns(Fun<Brook<O>, Brook<? extends R>, E>... funs) {
        Objects.requireNonNull(funs);
        if (!isPresent()) {
            return empty();
        } else {
            try {
                Brook<? extends R> apply = null;
                for (Fun<Brook<O>, Brook<? extends R>, E> fun : funs) {
                    apply = fun.apply(this);
                    if (apply.isPresent() || apply.isErr()) {
                        break;
                    }
                }
                return Objects.isNull(apply) ? empty() : apply.map(r -> (R) r);
            } catch (Exception e) {
                return constructError(e);
            }
        }
    }

    public <R extends Exception, E extends Exception> Brook<O> breakPoint(Fun<Exception, R, E> fun) throws R, E {
        if (isPresent()) {
            return this;
        } else {
            if (isErr()) {
                throw fun.apply(exception);
            } else {
                throw fun.apply(new NoSuchElementException("No value present"));
            }
        }
    }

    public <R extends Exception, E extends Exception> Brook<O> breakPoint(Predicate<O> predicate, Fun<O, R, E> fun) throws R, E {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(fun);
        if (isPresent()) {
            if (predicate.test(value)) {
                throw fun.apply(value);
            }
        }
        return this;
    }

    public Brook<O> errExec(Con<Exception, Exception> con) {
        Objects.requireNonNull(con);
        if (isErr()) {
            try {
                con.accept(exception);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public Brook<O> errReset(Fun<Exception, O, Exception> errMap) {
        Objects.requireNonNull(errMap);
        if (isErr()) {
            try {
                return construct(errMap.apply(exception), null);
            } catch (Exception e) {
                return constructError(e);
            }
        }
        return this;
    }

    public <E extends Exception> Brook<O> errReset(Class<E> errType, Fun<E, O, Exception> errMap) {
        Objects.requireNonNull(errType);
        Objects.requireNonNull(errMap);
        if (isErr()) {
            if (errType.isAssignableFrom(exception.getClass())) {
                try {
                    return construct(errMap.apply(errType.cast(exception)), null);
                } catch (Exception e) {
                    return constructError(e);
                }
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #unwrap()} instead to provide a more accurate naming
     * @return exists value or throw {@link NoSuchElementException}
     */
    @Deprecated
    public O get() {
        if (Objects.nonNull(value)) {
            return value;
        } else {
            throw new NoSuchElementException("No value present");
        }
    }

    /**
     * If a value is present, returns the value, otherwise throws NoSuchElementException.
     * @return the value currently held by this Brook
     * @throws NoSuchElementException if no value is present
     */
    public O unwrap() {
        if (Objects.nonNull(value)) {
            return value;
        } else {
            throw new NoSuchElementException("No value present");
        }
    }

    public O orElse(O other) {
        if (Objects.nonNull(value)) {
            return value;
        } else {
            return other;
        }
    }

    public O orElse(Sup<O, RuntimeException> sup) {
        if (Objects.nonNull(value)) {
            return value;
        } else {
            return sup.get();
        }
    }

    public <E extends Exception> O errElse(Fun<Exception, O, E> fun) throws E {
        if (isPresent()) {
            return value;
        } else {
            if (isErr()) {
                return fun.apply(exception);
            } else {
                return fun.apply(new NoSuchElementException("No value present"));
            }
        }
    }

    public <R extends Exception, E extends Exception> O orElseThrow(Sup<R, E> sup) throws E, R {
        if (isPresent()) {
            return value;
        } else {
            throw sup.get();
        }
    }

    public <R extends Exception, E extends Exception> O errElseThrow(Fun<Exception, R, E> fun) throws E, R {
        if (isPresent()) {
            return value;
        } else {
            if (isErr()) {
                throw fun.apply(exception);
            } else {
                throw fun.apply(new NoSuchElementException("No value present"));
            }
        }
    }

    public boolean isPresent() {
        return Objects.nonNull(this.value);
    }

    public boolean isEmpty() {
        return !isPresent();
    }

    public boolean isErr() {
        return Objects.nonNull(this.exception);
    }

    public Optional<O> optional() {
        if (isPresent()) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    public Stream<O> stream() {
        if (isPresent()) {
            return Stream.of(value);
        } else {
            return Stream.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <R> Brook<R> empty() {
        if (Objects.isNull(value)) {
            return (Brook<R>) this;
        } else {
            return construct(null, exception);
        }
    }

}
