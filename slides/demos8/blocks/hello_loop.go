// The contested hello world of section 4, run 3,000 times; counts each order.
package main

import (
	"fmt"
	"runtime"
)

func main() {
	fmt.Println("GOMAXPROCS(0) =", runtime.GOMAXPROCS(0), " NumCPU =", runtime.NumCPU())
	const runs = 3000
	helloWorld, worldHello := 0, 0
	for i := 0; i < runs; i++ {
		c := make(chan string)
		go func() { c <- "hello" }()
		go func() { c <- "world" }()
		x, y := <-c, <-c // in place of fmt.Println(<-c, <-c)
		if x == "hello" && y == "world" {
			helloWorld++
		} else {
			worldHello++
		}
	}
	fmt.Println("hello world:", helloWorld)
	fmt.Println("world hello:", worldHello)
}
