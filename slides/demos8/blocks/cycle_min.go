// cycle_min.go: the four lines of cycle.go that the notes show, capacity 0, one send each.
package main

import "fmt"

func main() {
	toMain, toOther := make(chan int), make(chan int)
	go func() { toMain <- 101; fmt.Println(<-toOther) }()
	toOther <- 1
	fmt.Println(<-toMain)
}
