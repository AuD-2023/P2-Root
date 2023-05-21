package p2;

@FunctionalInterface
interface ThrowingFunction<T, R> {
    R apply(T t) throws Throwable;
}
