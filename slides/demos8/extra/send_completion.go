// send_completion.go: when a send completes, and the state in which every goroutine waits.
// Written for the lecturer's own run (briefs/s8_research_axes.md, section 6); shown nowhere.
// Run: go run send_completion.go
package main

import (
	"fmt"
	"time"
)

func main() {
	// 1. An unbuffered channel: the send completes only when a receiver takes the value
	//    (Go specification, "Send statements"; Hoare 1978, 7.4).
	unbuf := make(chan int)
	go func() {
		unbuf <- 1
		fmt.Println("unbuffered: the send completed (a receiver arrived)")
	}()
	time.Sleep(200 * time.Millisecond)
	fmt.Println("unbuffered: the receiver starts now")
	fmt.Println("unbuffered: received", <-unbuf)
	time.Sleep(50 * time.Millisecond)

	// 2. A buffered channel of capacity 2: two sends complete while there is room, the third waits.
	buf := make(chan int, 2)
	go func() {
		for i := 1; i <= 3; i++ {
			buf <- i
			fmt.Printf("buffered(2): send %d completed, %d in the buffer\n", i, len(buf))
		}
	}()
	time.Sleep(200 * time.Millisecond)
	fmt.Println("buffered(2): the receiver starts now")
	for i := 0; i < 3; i++ {
		fmt.Println("buffered(2): received", <-buf)
	}
	time.Sleep(50 * time.Millisecond)

	// 3. Two goroutines that each wait for a send nobody makes. The runtime reports the state
	//    in which every goroutine is asleep and ends the program (Tu et al. 2019, 5.3).
	a, b := make(chan int), make(chan int)
	go func() { a <- <-b }()
	fmt.Println("both wait: main receives on a; the other goroutine waits to receive on b")
	fmt.Println(<-a)
}
