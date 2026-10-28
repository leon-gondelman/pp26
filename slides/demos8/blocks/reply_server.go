// A request-and-reply server: each request carries its own reply channel.
package main

import "fmt"

type req struct {
	n     int
	reply chan<- int // the server may only send on it
}

// counter serves the requests one at a time and keeps a running total.
func counter(reqs <-chan req) {
	total := 0
	for r := range reqs { // one request at a time
		total += r.n
		r.reply <- total // the answer goes back where the request said
	}
}

func main() {
	reqs := make(chan req)
	go counter(reqs)

	// a client, with a fresh reply channel for each request
	for _, n := range []int{5, 7} {
		reply := make(chan int)
		reqs <- req{n, reply}
		fmt.Println(<-reply)
	}
	close(reqs)
}
