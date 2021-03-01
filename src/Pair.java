public class Pair<L, R> {

    private L l;
    private R r;

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public L getLeft() {
        return l;
    }

    public R getRight() {
        return r;
    }

    public void setLeft(L l) {
        this.l = l;
    }

    public void setRight(R r) {
        this.r = r;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "l=" + l +
                ", r=" + r +
                '}';
    }
}
