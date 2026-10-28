%% The counter of section 9: two clients each get, then set V + 1.
%% An increment is lost when both get before either sets.
-module(counter).
-export([counter/1, client/2, main/0, run/1, start/0]).

%% Copied verbatim from the notes, section 9.
counter(N) ->
    receive
        {get, From} -> From ! {value, N}, counter(N);
        {set, M}    -> counter(M)
    end.

%% A client: the two lines of the notes, then tell the parent it is done.
client(C, Parent) ->
    C ! {get, self()},
    receive {value, V} -> C ! {set, V + 1} end,
    Parent ! done.

main() ->
    C = spawn(fun() -> counter(0) end),
    Self = self(),
    spawn(fun() -> client(C, Self) end),
    spawn(fun() -> client(C, Self) end),
    receive done -> ok end,
    receive done -> ok end,
    C ! {get, self()},
    V = receive {value, X} -> X end,
    exit(C, kill),               % the counter loops forever; stop it
    V.

%% Counts how often the final value is 1 (an increment lost) and 2.
run(N) ->
    Vs = [main() || _ <- lists:seq(1, N)],
    {length([V || V <- Vs, V =:= 1]), length([V || V <- Vs, V =:= 2])}.

start() ->
    io:format("Erlang/OTP ~s, ERTS ~s, schedulers ~p~n",
              [erlang:system_info(otp_release), erlang:system_info(version),
               erlang:system_info(schedulers_online)]),
    {L, T} = run(3000),
    io:format("run(3000): final value 1 (lost): ~p, final value 2: ~p~n", [L, T]).
