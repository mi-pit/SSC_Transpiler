package cz.mipit.sscc.ssc.compiler.data.var;

public class Typedef<T> extends Variable {
    private final T of;

    public Typedef(String name, int pointer, T of) {
        super(name, pointer);
        this.of = of;
    }

    public T getRepresentedType() {
        return of;
    }
}
