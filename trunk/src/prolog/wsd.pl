% Given a Noun1, find another Noun2 that is the object of the same
% verb as Noun1
commonObjectContext(Noun1, Noun2, Verb) :-
    rx_object(VerbOcc, Noun1),
    wordOccurrence(VerbOcc, Verb, _, _),
    not(ignoreVerb(Verb)),
    functor(AnyVerbOcc, Verb, 2),
    rx_object(AnyVerbOcc, Noun2),
    wordOccurrence(Noun1, _, _, W1),
    wordOccurrence(Noun2, _, _, W2),
    W1 \= W2.

singleSense(Relation) :-
    functor(Relation, Kind),
    relationSenseType(Kind, SenseType),
    arg(1, Relation, Occurrence),    
    singleSense(SenseType, Occurrence).
    
singleSense(SenseType, Occurrence) :- 
    wordOccurrence(Occurrence, _, _, Atom),
    1 is hg_count(and(type(SenseType), incident(Atom))),
    wordSense(SenseType, Atom, S),    
    !,
	assert(senseOf(Occurrence, S, 1.0)).
        
% Find the sense of a monosemous noun
findSense(rx_noun(X)) :- singleSense(nounSenseType, X).
findSense(rx_adjective(X)) :- singleSense(adjSenseType, X).
findSense(rx_adverb(X)) :- singleSense(adverbSenseType, X).
findSense(rx_verb(X)) :- singleSense(verbSenseType, X).

% Find the sense of a noun based on the (already found) sense of another
% that occurs as the object of the same verb
findSense(rx_noun(X)) :-    
%    write(['try 2', rx_noun(X)]),
	commonObjectContext(X, Y, V),
%    write(['common verb context ', X, Y, V]),
    rx_noun(Y),
    wordOccurrence(X, _, _, Atom),
	( senseOf(Y, S, _) 
        -> similarSense(Atom, S, S1, Weight);
            wordOccurrence(Y, _, _, YAtom), 
            wordSense(nounSenseType, YAtom, S),
            similarSense(Atom, S, S1, Weight)),
    !,
	Confidence is 0.9*Weight,
	assert(senseOf(X, S1, Confidence)),       
    (bound(YAtom) ->  assert(senseOf(Y, S, Confidence))).

% Find the sense of a noun based on its verb context and an already disambiguated
% occurrence of the same noun in another verb context that is similar in sense.
findSense(rx_noun(X)) :-
    rx_object(VerbOcc, X), wordOccurrence(VerbOcc, _, _, VerbAtom),
    wordOccurrence(X, Noun, _, _),
    functor(AnyNounOcc, Noun, 2),
    rx_object(OtherVerb, AnyNounOcc),
    AnyNounOcc \= X,
    senseOf(AnyNounOcc, NounSense, Confidence),
    (senseOf(OtherVerb, S, _) 
        -> similarSense(VerbAtom, S, _, _);
            wordOccurrence(OtherVerb, _, _, OtherVerbAtom),
            wordSense(verbSenseType, OtherVerbAtom, Sense),
            similarSense(VerbAtom, Sense, _, _)),
    !,
    assert(senseOf(X, NounSense, 0.7)).


findSense(_) :- !.

% Synonymous
similarSense(Word, ToSense, Result, Weight) :-
    senseType(ToSense, SenseType),
    wordSense(SenseType, Word, ToSense),
    Result=ToSense,
    Weight=1.0.

% Hyponymous
similarSense(Word, ToSense, Result, Weight) :-
    senseType(ToSense, SenseType),
	wordSense(SenseType, Word, Result),
	directIsa(ToSense, Result),
    Weight=0.98.

% Sibling in the IS-A hierarchy
similarSense(Word, ToSense, Result, Weight) :-
    directIsa(ToSense, Parent),
    senseType(ToSense, SenseType),
	wordSense(SenseType, Word, Result),
    directIsa(Result, Parent),
    Weight=0.888888888888888.
        
wordOccurrence(Word, Token, Pos, Atom) :-
	functor(Word, Token, _),
	arg(1, Word, Pos),
	arg(2, Word, Atom).
	
findHypernym(Sense, WithWord, Hypernym) :-
	senseType(Sense, SenseType),
	wordSense(SenseType, WithWord, Hypernym),
	senseIsa(Sense, Hypernym).

senseIsa(Child, Parent) :- directIsa(Child, Parent), !.
senseIsa(Child, Parent) :-	directIsa(Child, X),	senseIsa(X, Parent).
directIsa(Child, Parent) :- wn_kindof(Child, X) ; wn_instanceof(Child, X).
        
wordSense(SenseType, Word, Sense) :- 
  (bound(Word) ->
  	(bound(Sense) ->
  		hg <- target(Sense) returns C, C <- satisfies(graph, Word) returns true;
  		AllSenses is hg_find_all(and(type(SenseType), incident(Word))), collection_item(AllSenses, Sense)
  	 );
  	 (bound(Sense) -> 
  		All is hg_find_all(target(Sense)), collection_item(All, Word);
  		wn_word(Word), wordSense(SenseType, Word, Sense))).

senseType(Sense, Type) :-
	graph <- get(Sense) returns SenseValue,
	SenseValue <- getClass returns Type.

findNounSenses :-
	rx_noun(X),
%    write(['looking for noun sense ', X]),
	not(senseOf(X, _, _)), 
%    write(['going to infer sense of ', X]),
	findSense(rx_noun(X)),
%%   write(['finished inferring sense ', X]),  
    fail.
findNounSenses :-
	write("Done with nouns.").
		  		
makeSense :-
	findNounSenses.
        
ignoreVerb(have).
ignoreVerb(be).

relationSenseType(rx_noun, nounSenseType).
relationSenseType(rx_adjective, adjSenseType).
relationSenseType(rx_adverb, adverbSenseType).
relationSenseType(rx_verb, verbSenseType).

% WordNet 2.1 synset offset id for entity types
entitySynsetId(entity_location, 152507).

