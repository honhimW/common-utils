package io.github.honhimw;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author hon_him
 * @since 2021/8/30
 */
@SuppressWarnings("unused")
@Slf4j
public class ReactorUtils {

    public static <T, R> List<R> block(int parallel, Collection<T> tList, Function<T, Stream<R>> mapper) {
        ExecutorService executor = Executors.newFixedThreadPool(parallel);
        try {
            return block(executor, parallel, tList, null, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 并发处理, 获得所有线程结果, 任意线程超时返回空(丢失所有线程结果)
     *
     * @param executor 线程池
     * @param parallel 并发数
     * @param tList    源数据集
     * @param timeout  超时时间
     * @param mapper   源数据与结果映射, 相当于task
     * @param <T>      源数据
     * @param <R>      返回结果
     * @return 返回结果集
     */
    public static <T, R> List<R> block(Executor executor, int parallel, Collection<T> tList,
                                       Duration timeout,
                                       Function<T, Stream<R>> mapper) {
        ParallelFlux<T> parallelFlux = Flux
            .fromIterable(tList)
            .parallel(parallel)
            .runOn(Schedulers.fromExecutor(executor));
        Mono<List<R>> mono = parallelFlux
            .flatMap(convertReactorMapper(mapper))
            .collectSortedList((o1, o2) -> 0)
            .onErrorContinue(
                (throwable, o) -> log.error("执行异常: {}, 源数据: {}", throwable.toString(), o.toString()));
        if (timeout != null && !timeout.equals(Duration.ZERO)) {
            mono = mono.timeout(timeout, Mono.empty());
        }
        return mono.block();
    }

    public static void execute(int parallel, int times, Consumer<Integer> consumer) {
        ExecutorService executor = Executors.newFixedThreadPool(parallel);
        try {
            execute(executor, parallel, times, null, consumer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    public static void execute(Executor executor, int parallel, int times, Duration timeout, Consumer<Integer> consumer) {
        if (times < 1) {
            return;
        }
        ParallelFlux<Integer> parallelFlux = Flux
            .range(0, times)
            .parallel(parallel)
            .runOn(Schedulers.fromExecutor(executor));

        Mono<Void> mono = parallelFlux
            .doOnNext(consumer)
            .then()
            .onErrorContinue(
                (throwable, o) -> log.error("执行异常: {}, 序号: {}", throwable.toString(), o.toString()));
        if (timeout != null && !timeout.equals(Duration.ZERO)) {
            mono = mono.timeout(timeout, Mono.empty());
        }
        mono.block();
    }

    /**
     * Stream mapper转为Reactor mapper
     */
    public static <T, R> Function<T, Publisher<R>> convertReactorMapper(
        Function<T, Stream<R>> mapper) {
        return t -> {
            Stream<? extends R> rStream = mapper.apply(t);
            return Flux.fromStream(rStream);
        };
    }

    public static <T, R> R retry(T param, Fun<? super T, ? extends R> mapper) {
        return retry(3, param, mapper);
    }

    public static <T, R> R retry(int retryCount, T param, Fun<? super T, ? extends R> mapper) {
        return retry(retryCount, param, mapper, r -> true);
    }

    public static <T, R> R retry(int retryCount, T param, Fun<? super T, R> mapper, Predicate<R> predicate) {
        return retry(retryCount, param, mapper, predicate, Stream.of(Throwable.class));
    }

    /**
     * @param retryCount 重试次数
     * @param param      参数
     * @param mapper     抛出{@link RuntimeException(Throwable)}
     * @param predicate  结果断言, false需要重试
     * @param throwables 需要重试的异常, {@link Stream#empty()}表示不捕获异常
     * @param <T>        参数类型
     * @param <R>        返回类型
     * @return 结果
     */
    public static <T, R> R retry(int retryCount, T param, Fun<T, R> mapper, Predicate<R> predicate,
                                 Stream<Class<? extends Throwable>> throwables) {
        AtomicInteger count = new AtomicInteger(0);
        if (throwables == null) {
            throwables = Stream.empty();
        }
        List<Class<? extends Throwable>> causes = throwables.toList();

        if (retryCount < 0) {
            retryCount = 0;
        }
        try {
            Mono<R> mono = Mono.just(param).flatMap(t -> {
                R r;
                try {
                    r = mapper.apply(t);
                } catch (Exception e) {
                    return Mono.error(e);
                }
                boolean result = predicate.test(r);
                if (!result) {
                    return Mono.error(new IllegalArgumentException("结果不符合预期"));
                } else {
                    return Mono.just(r);
                }
            });
            if (retryCount > 0) {
                mono = mono.retryWhen(Retry.max(retryCount).filter(throwable -> {
                    boolean flag = causes
                        .stream().anyMatch(type -> type.isAssignableFrom(throwable.getClass()));
                    if (flag) {
                        count.incrementAndGet();
                    }
                    return flag;
                }));
            }
            return mono.block();
        } catch (Exception e) {
            String msg;
            if (count.get() > 0) {
                msg = String.format("重试次数%d次, 失败原因: %s", count.get(), e);
            } else {
                msg = String.format("未捕获的异常: %s", e);
            }
            throw new RuntimeException(msg);
        }

    }

    /**
     * 同{@link Function#apply(Object)}, 但支持抛出非运行时异常
     *
     * @param <T> 参数类型
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface Fun<T, R> {

        R apply(T t) throws Exception;

    }

}
