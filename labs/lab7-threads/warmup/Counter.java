// The counter: one interface, four implementations (RacyCounter is given; the other three
// are exercise 1 of the warm-up). The check: java -cp out CounterCheck racy|locked|atomic|perworker
//
// The contract. add(int worker) never loses an add: after every thread that adds has ended and
// been joined, get() is the number of adds. During the run, get() returns a value at least the
// number of adds that completed before the call began and at most the number that began before
// it returned; for the count per worker, the value is a sum of slots read one after another,
// not a snapshot. Three of the four implementations ignore the argument; the fourth,
// PerWorkerCounter, also offers get(int worker), that worker's own count. The racy
// implementation breaks both sentences: a reader may never see a write, and a plain long may
// be read half-written.
public interface Counter {
  void add(int worker);
  long get();
}
