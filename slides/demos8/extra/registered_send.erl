#!/usr/bin/env escript
%% registered_send.erl: a send to the pid of a dead process against a send to a registered name
%% that no longer exists (the story, section 9, "Mailbox semantics in Erlang").
%% Run: escript registered_send.erl
-mode(compile).

main(_) ->
    Pid = spawn(fun() -> receive stop -> ok end end),
    register(worker, Pid),
    Ref = monitor(process, Pid),
    Pid ! stop,
    receive {'DOWN', Ref, process, Pid, _} -> ok end,
    %% the process is dead, and its name was unregistered when it died
    io:format("whereis(worker) after the death: ~p~n", [whereis(worker)]),
    io:format("Pid ! hello returned: ~p~n", [Pid ! hello]),
    try worker ! hello of
        R -> io:format("worker ! hello returned: ~p~n", [R])
    catch
        error:Reason -> io:format("worker ! hello raised: error:~p~n", [Reason])
    end.
