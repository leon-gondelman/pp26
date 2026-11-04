// two_senders.go: two goroutines send on one channel c, and main receives twice. The order of
// the two values is the runtime's; blocks/hello_loop.go counts it over 3,000 runs.
package main

import "fmt"

func main() {
	c := make(chan string)
	go func() { c <- "hello" }()
	go func() { c <- "world" }()
	fmt.Println(<-c, <-c)
}
