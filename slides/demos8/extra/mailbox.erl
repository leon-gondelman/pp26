#!/usr/bin/env escript
%% mailbox.erl: the mailbox of an Erlang process, as the reference manual describes it.
%% Written for the lecturer's own run (briefs/s8_research_axes.md, section 6); shown nowhere.
%% Run: escript mailbox.erl
-mode(compile).

main(_) ->
    Self = self(),

    %% 1. A send returns at once, whether or not anybody receives.
    Self ! {note, "a send returns at once"},
    io:format("1. the send returned; the message waits in the mailbox~n"),
    receive {note, T} -> io:format("   received: ~s~n", [T]) end,

    %% 2. Two senders, one receiver: each sender's order is kept; the interleaving varies.
    spawn(fun() -> [Self ! {a, I} || I <- lists:seq(1, 5)] end),
    spawn(fun() -> [Self ! {b, I} || I <- lists:seq(1, 5)] end),
    Msgs = [receive {Tag, I} -> {Tag, I} end || _ <- lists:seq(1, 10)],
    io:format("2. arrival order: ~p~n", [Msgs]),
    io:format("   a's own order kept: ~p; b's own order kept: ~p~n",
              [[I || {a, I} <- Msgs] =:= lists:seq(1, 5),
               [I || {b, I} <- Msgs] =:= lists:seq(1, 5)]),

    %% 3. A selective receive takes the first matching message, from behind earlier ones.
    Self ! first,
    Self ! second,
    Self ! {urgent, 3},
    receive {urgent, N} ->
        io:format("3. the selective receive took {urgent, ~p} past 'first' and 'second'~n", [N])
    end,
    receive first -> ok end,
    receive second -> ok end,

    %% 4. A send to a dead process succeeds silently.
    Dead = spawn(fun() -> ok end),
    timer:sleep(50),
    io:format("4. is_process_alive(Dead) = ~p; sending anyway~n", [is_process_alive(Dead)]),
    hello = Dead ! hello,
    io:format("   the send returned its message; nothing reports the loss~n"),

    %% 5. A monitor turns the other side's death into a message.
    Pid = spawn(fun() -> receive stop -> exit(because) end end),
    Ref = monitor(process, Pid),
    Pid ! stop,
    receive {'DOWN', Ref, process, Pid, Reason} ->
        io:format("5. the monitor delivered {'DOWN', Ref, process, Pid, ~p}~n", [Reason])
    end.
