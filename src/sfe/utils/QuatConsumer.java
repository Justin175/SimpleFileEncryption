package sfe.utils;

@FunctionalInterface
public interface QuatConsumer <A, B, C, D> {

	public void accept(A a, B b, C c, D d);
}
