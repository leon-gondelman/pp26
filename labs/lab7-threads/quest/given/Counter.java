// The counter of the warm-up: the interface, and the racy implementation.
//
// Contract. add(int worker) never loses an add: after every thread that adds has ended and
// been joined, get() is the number of adds. During the run, get() returns a value at least
// the number of adds that completed before the call began and at most the number that began
// before it returned. Three of the four implementations ignore the argument; the fourth,
// PerWorkerCounter, also offers get(int worker), its own count, and its get() is a sum of
// slots read one after another, not a snapshot.
public interface Counter {
  void add(int worker);
  long get();
}
