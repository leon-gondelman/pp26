package main

import (
	"fmt"
	"time"
)

// The program of Dijkstra's bounded buffer with a channel of capacity 2.
func produce(k int) string { return fmt.Sprintf("line %d", k) }
func process(p string)     { time.Sleep(15 * time.Millisecond); fmt.Println(p) }

func main() {
	portions := make(chan string, 2)
	go func() {
		for k := 1; k <= 8; k++ {
			portions <- produce(k)
		}
		close(portions)
	}()
	for p := range portions {
		process(p)
	}
}
