use std::sync::{Arc, Mutex};
use std::thread;

// The balance is inside its lock: the only way to it is through lock().
fn main() {
    let balance = Arc::new(Mutex::new(100));
    let b = Arc::clone(&balance);
    let t = thread::spawn(move || {
        *b.lock().unwrap() += 20;
    });
    t.join().unwrap();
    println!("{}", *balance.lock().unwrap());
}
