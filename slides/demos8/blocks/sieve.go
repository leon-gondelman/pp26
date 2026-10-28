// The prime sieve as a pipeline of goroutines, unbuffered channels only.
package main

import "fmt"

// generate sends 2, 3, 4, ... forever.
func generate(out chan<- int) {
	for i := 2; ; i++ {
		out <- i
	}
}

// filter forwards every value not divisible by p.
func filter(in <-chan int, out chan<- int, p int) {
	for {
		n := <-in
		if n%p != 0 {
			out <- n
		}
	}
}

func main() {
	ch := make(chan int)
	go generate(ch)
	for i := 0; i < 10; i++ {
		p := <-ch // the first value to pass every filter is prime
		fmt.Println(p)
		next := make(chan int)
		go filter(ch, next, p)
		ch = next
	}
}
