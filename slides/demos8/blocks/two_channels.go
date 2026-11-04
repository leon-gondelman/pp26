// two_channels.go: one channel per sender, and main reads a then b: Kahn's discipline on Go
// channels. The waiting differs between runs; the output never does.
package main

import "fmt"

func main() {
	a, b := make(chan string), make(chan string)
	go func() { a <- "hello" }()
	go func() { b <- "world" }()
	fmt.Println(<-a, <-b)
}
