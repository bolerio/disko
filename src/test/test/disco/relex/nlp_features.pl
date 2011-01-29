:- op(500,xfy, ':').
:- op(500,xfy, '@').

unify_fs(Dag1,Dag2) :-
	unify0(Dag1,Dag2),
	nl,
	write('Result of unification is: '),
	nl,
	nl,
	write(Dag1),
	nl.

val(Feature,Value1,[Feature : Value2|Rest],Rest) :-
	!,
	unify0(Value1,Value2).

val(Feature,Value,[Dag|Rest],[Dag|NewRest]) :-
	!,
	val(Feature,Value,Rest,NewRest).

unify0(Dag,Dag) :- !.

unify0(X,Y) :-
 X=L@R, Y=Z@W, !, L=R, Z=W, unify0(R,W);
 X=L@R, !, L=R, unify0(R,Y);
 Y=Z@W, !, Z=W, unify0(X,W).

unify0([Feature:Value|Rest],Dag) :- val(Feature,Value,Dag,StripDag), unify0(Rest,StripDag).

traverseFS(X) :- not(compound(X)), !.
traverseFS([]).
traverseFS(X@L) :-  X=L, traverseFS(L), !.
traverseFS([F:V|Rest]) :-  traverseFS(V), traverseFS(Rest).
circular(FS) :- not(traverseFS(FS)).
