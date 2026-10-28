// cycle.go: two goroutines, each sends to the other first and receives afterwards.
// Run: go run cycle.go <capacity> <sends>   (go run cycle.go 0 1, 1 1, 1 2)
package main

import (
	"fmt"
	"os"
	"strconv"
)

func main() {
	k, _ := strconv.Atoi(os.Args[1])     // the capacity of both channels
	sends, _ := strconv.Atoi(os.Args[2]) // the values each side sends before it receives
	toMain, toOther := make(chan int, k), make(chan int, k)
	done := make(chan bool)
	go func() {
		for i := 1; i <= sends; i++ {
			toMain <- 100 + i // send first
		}
		for i := 1; i <= sends; i++ {
			fmt.Println("other received", <-toOther) // then receive
		}
		done <- true
	}()
	for i := 1; i <= sends; i++ {
		toOther <- i // send first
	}
	for i := 1; i <= sends; i++ {
		fmt.Println("main received", <-toMain) // then receive
	}
	<-done
	fmt.Println("finished")
}
