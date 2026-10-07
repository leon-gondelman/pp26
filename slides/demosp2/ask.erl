-module(ask).
-export([main/0, ask/2]).

%% Ask, and give up after a second: slow and stopped look the same.
ask(Account, Request) ->
    Account ! Request,
    receive
        Answer -> Answer
    after 1000 ->
        no_answer
    end.

main() ->
    Stopped = spawn(fun() -> ok end),            % a process that has already ended
    io:format("~p~n", [ask(Stopped, {withdraw, 50, self()})]).
