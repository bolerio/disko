%-----------------------------------------------------------------------------------------------------------
% SIMPLY SYNTAX FOR FORWARD CHAINING
%-----------------------------------------------------------------------------------------------------------
:- op(260, fx, 'rule').
:- op(230, xfx, '==>').
:- op(250, fx, 'forall').
:- op(250, fx, 'forone').
:- op(700, xfx, ':').  % the clause Var : Term will call Term and unify the result with Var, e.g. 'X : fact(A,B)'  

':'(Var, Term):- call(Term), Var=Term.

fwdchain :-
  call(rule ID : X ==> Y),
  write([rule, ID]),nl,
  match(X), 
  write(matched), nl,
  process(Y), 
  write(processed), nl,
  fail.
fwdchain :- write(done).


match([]).
match([Cond | Rest]) :- call(Cond), match(Rest).

process([]).
process([Action | Rest]) :-  
  write(Action),nl,
  call(Action), !, process(Rest).
