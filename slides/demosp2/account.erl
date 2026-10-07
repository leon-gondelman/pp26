-module(account).
-export([main/0, loop/1]).

%% The bank's account as a process: it handles one message at a time.
loop(Balance) ->
    receive
        {deposit, N, From} ->
            From ! ok, loop(Balance + N);
        {withdraw, N, From} when N =< Balance ->
            From ! ok, loop(Balance - N);
        {withdraw, _, From} ->
            From ! refused, loop(Balance)
    end.

main() ->
    Account = spawn(account, loop, [100]),
    lists:foreach(fun(Request) ->
        Account ! erlang:append_element(Request, self()),
        receive Answer -> io:format("~p~n", [Answer]) end
    end, [{deposit, 20}, {withdraw, 150}, {withdraw, 120}]).
