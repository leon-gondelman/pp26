// The same counter with the integer inside a lock: Arc<Mutex<i32>>. Compiles, and
// three runs printed 2000000 (rustc --edition 2024 -O; briefs/refs/records/s7_runs_c1_rows.md, 2.2).
use std::sync::{Arc, Mutex};
use std::thread;

fn main() {
    let count = Arc::new(Mutex::new(0));
    let mut handles = Vec::new();
    for _ in 0..2 {
        let c = Arc::clone(&count);
        handles.push(thread::spawn(move || {
            for _ in 0..1_000_000 {
                *c.lock().unwrap() += 1;
            }
        }));
    }
    for h in handles {
        h.join().unwrap();
    }
    println!("{}", *count.lock().unwrap());
}
