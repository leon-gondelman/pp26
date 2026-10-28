// shared_pointer.go: a pointer sent on a channel stays shared, and a write by the sender after
// the send races with the receiver's read. "Share by communicating" is convention (Effective Go).
// Written for the lecturer's own run (briefs/s8_research_axes.md, section 6); shown nowhere.
// Run: go run -race shared_pointer.go
package main

import (
	"fmt"
	"sync"
)

type Account struct{ Balance int }

func main() {
	ch := make(chan *Account)
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		acct := <-ch
		fmt.Println("receiver reads the balance:", acct.Balance)
	}()
	acct := &Account{Balance: 100}
	ch <- acct         // the pointer is sent: the receiver now shares the struct
	acct.Balance = 200 // the sender writes after the send: a data race with the receiver's read
	wg.Wait()
	fmt.Println("sender's final balance:", acct.Balance)
}
