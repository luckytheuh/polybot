package polybot;

public class TwoValues<T, U> {
    T t;
    U u;

    public TwoValues(T t, U u) {
        this.t = t;
        this.u = u;
    }

    public T getFirst() {
        return t;
    }

    public void setFirst(T t) {
        this.t = t;
    }

    public U getSecond() {
        return u;
    }

    public void setSecond(U u) {
        this.u = u;
    }
}
