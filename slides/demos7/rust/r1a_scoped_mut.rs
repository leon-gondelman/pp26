// The counter of example 1 in Rust, the two threads started inside thread::scope.
// Refused by the compiler: error[E0499], cannot borrow `count` as mutable more
// than once at a time (rustc --edition 2024; briefs/refs/records/s7_runs_c1_rows.md, 2.1).
use std::thread;

fn main() {
    let mut count = 0;
    thread::scope(|s| {
        s.spawn(|| for _ in 0..1_000_000 { count += 1 });
        s.spawn(|| for _ in 0..1_000_000 { count += 1 });
    });
    println!("{}", count);
}
