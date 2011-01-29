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

fire(forall LHS ==> RHS) :- 
  write([forall, LHS, RHS]), nl,
  match(LHS),
  write(matched),nl,
  process(RHS), fail.
fire(forall LHS ==> RHS) :- !.
fire(forone LHS ==> RHS) :- match(LHS), !, process(RHS).
fire(forone LHS ==> RHS) :- !.

match([]).
match([Cond | Rest]) :- call(Cond), match(Rest).

process([]).
process([Action | Rest]) :-  
  write(Action),nl,
  call(Action), !, process(Rest).

forall(Premises ==> Actions) :- fire(forall Premises ==> Actions).
forone(Premises ==> Actions) :- fire(forone Premises ==> Actions).

listMax([], _).
listMax([X | Rest], M) :-
  listMax(Rest, Temp),
  max(Temp, X, M).
max(X, Y, M) :-
  X > Y -> M = X; M = Y.

assertUnique(Fact) :- not(Fact) -> assert(Fact); true.
%-----------------------------------------------------------------------------------------------------------

%-----------------------------------------------------------------------------------------------------------
% Construct and run the SAN. The results are read in the Java portion of the algorithm.
%-----------------------------------------------------------------------------------------------------------
buildSAN :-
% assume 'san' object is created by the calling Java code, along with other objects.
  findCoOccurrences,
  createSAN,
  san <- run.

%-----------------------------------------------------------------------------------------------------------
% CO-OCCURRENCES
%-----------------------------------------------------------------------------------------------------------
% co_occurrence(Relation, X, Y, CoWeight) state that X and Y appear in the same position of two
% binary relations occurrences of the same type where the sense similarity between the words
% in the other position in the relations is given by CoWeight. Thus, two nouns appearing in the same verb
% context yields co_occurrence(rx_object, Noun1, Noun2, 1.0) while if the verb context is not
% exactly the same, but there's sense similarity of 0.8 b/w the two verbs, we'd have
% co_occurrence(rx_object, Noun1, Noun2, 0.8).

findCoOccurrences :-
    similarityThreshold(Threshold),
	exrel(Relation, X, Y),
	exrel(Relation, X1, Y1),
	(X \= X1, samePartOfSpeech(X, X1), checkCoOccurrence(Y, Y1, WeightY), WeightY > Threshold -> 
          assertCoOccurrence(Relation,X,X1,WeightY); true),
    (Y \= Y1, samePartOfSpeech(Y, Y1), checkCoOccurrence(X, X1, WeightX), WeightX > Threshold ->
          assertCoOccurrence(Relation,Y,Y1,WeightX); true),
    fail.
findCoOccurrences :- write('Co-occurrence fact base generated.'), nl.

% Check whether two word occurrences in a relation can be deemed common enough
% to define a co-occurrence.
checkCoOccurrence(word(Lx, Px, _, Ax), word(Ly, Py, _, Ay), Weight) :-
  Px=Py,
  not(ignoreWord(Lx)),
  not(ignoreWord(Ly)), 
  (Ax=Ay -> Weight=1.0; verySimilarAtoms(Px, Ax, Ay, Weight)), 
  !.

% We assert a co_occurrence between X, Y via the Relation if no such
% co_occurrence has already been assert. If it has, we take the max of the weights,
% which is similar to fuzzy logic  or. 
assertCoOccurrence(Relation, X, Y, Weight) :-
%    write(['asserting co occurence ', Relation, X, Y, Weight]),
	co_occurrence(Relation, X, Y, CurrWeight),
	!,
 %   write(['possibly retracting with current weight ', CurrWeight]),
	(Weight > CurrWeight -> retract(co_occurrence(Relation, X, Y, CurrWeight)), 
				assert(co_occurrence(Relation, X, Y, Weight))).
assertCoOccurrence(Relation, X, Y, Weight) :-
%  write('asserting new  '), nl,
  assert(co_occurrence(Relation, X, Y, Weight)).
%-----------------------------------------------------------------------------------------------------------


%-----------------------------------------------------------------------------------------------------------
% CREATE THE SPREADING ACTIVATION NETWORK
%-----------------------------------------------------------------------------------------------------------	  
createSAN :-
  addOccurrenceNetwork,   
  addGrammaticalNetwork,   
  addCoOccurrenceNetwork.

% Add a node for each sense of all word occurrences in the text. Negative edges with
% weight 1/#senses are added. The initial node weights are also 1/#senses because we
% do not have WordNet sense frequency info at this time.
%
% The first rule does this by looping over all 'word' predicates using failure.
% The second rule connects all nodes that share the same lemma. 
addOccurrenceNetwork :-
  word(Lemma, PoSpeech, Position, Atom),
  posSenseType(PoSpeech, SenseType),
  SenseCount is hg_count(and(type(SenseType), incident(Atom))),
  (SenseCount = 0 -> write([sensecount0, Lemma, PoSpeech, Position, Atom]), fail; true),  
  NodeWeight is 1.0 / SenseCount,  EdgeWeight is -NodeWeight,  
  java_object('java.util.ArrayList', [], NodeList),  
  forall [ wordSense(SenseType, Atom, Sense)] ==> 
  		 [ makeNode(Atom, Position, Sense, Node), san <- addNode(Node, NodeWeight), NodeList <- add(Node)],
  forall [ collection_item(NodeList, Node1) ] ==> 
  		 [ forall [collection_item(NodeList, Node2), Node1 \= Node2] ==> 
  		 		  [ san <- addEdge(Node1, Node2, EdgeWeight)]],
  fail. 
addOccurrenceNetwork :-  
  san <- getNodes returns NodeList, 
  forall [ collection_item(NodeList, Node1) ] ==>
  		 [ forall [ collection_item(NodeList, Node2), sameSenseAndLemma(Node1, Node2)] ==>
  		 		  [ san <- addBiEdge(Node1, Node2, 1.0)]],
  fail.
addOccurrenceNetwork :- write('Added occurrence network to SAN.'), nl.
  
addGrammaticalNetwork :-
  exrel(Relation, word(_, PoSpeechX, PositionX, AtomX), word(_, PoSpeechY, PositionY, AtomY)),
  posSenseType(PoSpeechX, _),
  posSenseType(PoSpeechY, _),
  wnSemTools <- getConceptualDensity returns CD,
  densityPoSpeech(PoSpeechX, PoSpeechY, PoSpeech),
  CD <- getDensityMap(AtomX, PoSpeechX, AtomY, PoSpeechY, PoSpeech, true) returns DensityMap,
  forall [ map_entry(DensityMap, Senses, Densities) ] ==>
  		 [ Senses <- getFirst returns XSense,
  		   Senses <- getSecond returns YSense,
  		   Densities <- getFirst returns XDensity,
  		   Densities <- getSecond returns YDensity,
  		   makeNode(AtomX, PositionX, XSense, NodeX),
           makeNode(AtomY, PositionY, YSense, NodeY),
  		   (YDensity > 0 -> san <- addEdge(NodeX, NodeY, YDensity); true),
  		   (XDensity > 0 -> san <- addEdge(NodeY, NodeX, XDensity); true)],
  fail.
addGrammaticalNetwork :- write('Added relations edges to SAN.'), nl.

addCoOccurrenceNetwork :-
  similarityThreshold(Threshold),
  co_occurrence(Relation, word(_, PoSpeechX, PositionX, AtomX), word(_, PoSpeechY, PositionY, AtomY), CoWeight),
  PoSpeechX = PoSpeechY,
  posSenseType(PoSpeechX, SenseType),
  %% TODO - the following will blatantly fail for adverbs because we only know
  %% how to deal with them starting from the lexical level. 
  forall [ wordSense(SenseType, AtomX, SenseX), wordSense(SenseType, AtomY, SenseY) ] ==>
         [ makeNode(AtomX, PositionX, SenseX, NodeX),
           makeNode(AtomY, PositionY, SenseY, NodeY),
           similar(SenseType, SenseX, SenseY, SimilarityWeight),
%           write([cooccurrence, similarity, NodeX, NodeY, SimilarityWeight]),
           EdgeWeight is SimilarityWeight * CoWeight,
           (SimilarityWeight > Threshold -> (san <- addBiEdge(NodeX, NodeY, EdgeWeight)); true)
         ],
  fail.

addCoOccurrenceNetwork :- write('Added co-occurrence edges to SAN.'), nl.

% Nodes are represented as HGDB Pair objects for 
makeNode(AtomLemma, Position, Sense, Node) :-
  java_object('org.hypergraphdb.util.Pair', [AtomLemma, Position], P1),
  java_object('org.hypergraphdb.util.Pair', [P1, Sense], Node).

% Check whether two SAN nodes have the same lemma and the same sense.
sameSenseAndLemma(X, Y) :-
  X <- getFirst returns X1,  Y <- getFirst returns Y1,
  X1 <- getFirst returns LX, Y1 <- getFirst returns LY,  
  X <- getSecond returns SX, Y <- getSecond returns SY,
  LX = LY, SX = SY.

% The following need experimentation. The simplest strategy is to always count
% overlapping nouns in glosses since they are the most prominent part of speech.
% We just pust a slight modification to count common verbs in case we are
% interested in the conceptual density b/w two adverbs
densityPoSpeech(adverb, adverb, verb) :- !.
densityPoSpeech(_, _, noun) :- !.

%-----------------------------------------------------------------------------------------------------------
% SIMILARITY MEASURE
%
% The 3 argument version works on occurrences and it returns the weight of the two most similar senses of
% those occurrences. It will yield weight=0 if the two occurrence are not of the same part of speech.
% The 4 argument version works on two senses of a given sense type.
% The auxilary similar2 predicate takes care of the adverb case where our best guess is when the 
% adverbs are derived from an adjective.
%-----------------------------------------------------------------------------------------------------------
similar(X, Y, Weight) :-
%  write(['checking sense for ', X, ' and ', Y]),
  X = word(_, PoSpeechX, _, AtomX),
%  write(['posx=', PoSpeechX]),
  Y = word(_, PoSpeechY, _, AtomY),
%  write(['posy=', PoSpeechY]),  
  PoSpeechX = PoSpeechY,
%  write('same POS'),
  similar2(PoSpeechX, AtomX, AtomY, Weight).

% For adverbs
similar2(adverb, AtomX, AtomY, Weight) :-
  wn_derivedfrom(AtomX, AdjectiveX),
  wn_derivedfrom(AtomY, AdjectiveY),
  similar2(adjective, AdjectiveX, AdjectiveY, Weight).

similar2(PoSpeech, AtomX, AtomY, Weight) :-
  posSenseType(PoSpeech, SenseType),
%  write(['checking sense for ', AtomX, ' and ', AtomY, ' pos=', PoSpeech]),  
  (AtomX = AtomY -> Weight=1.0;
   assert(currentMax(0)),
   forall [ wordSense(SenseType, AtomX, SenseX), wordSense(SenseType, AtomY, SenseY)]
   ==> [similar(SenseType, SenseX, SenseY, W),
        currentMax(M),
        (W > M -> retract(currentMax(_)), assert(currentMax(W)); true)],
   currentMax(Weight),
   retract(currentMax(_))
  ).

% we do not care to find the max here - it is going to be >= 0.9 if this succeeds
% anyway, which is good enough
verySimilarAtoms(Pos, X, Y, Weight) :-
  posSenseType(Pos, SenseType),
  wordSense(SenseType, X, Sx),
  wordSense(SenseType, Y, Sy),
  verySimilar(SenseType, Sx, Sy, Weight),
  !.
      
% synonymy
verySimilar(_, X, X, 1.0).
% hyponymous
verySimilar(SenseType, X, Y, 0.95) :-
  (SenseType=nounSenseType;SenseType=verbSenseType),
  (directIsa(X, Y); directIsa(Y, X)),!.
% siblings in hypernymy tree
verySimilar(SenseType, X, Y, 0.9) :-
  (SenseType=nounSenseType;SenseType=verbSenseType),
  directIsa(X, P), 
  directIsa(Y, P), 
  X <- toString returns SX, Y <- toString returns SY,
  !.
verySimilar(adjSenseType, X, Y, 0.9) :-
   wn_similar(X, Y), !.
       
similar(SenseType, X, Y, Weight) :- verySimilar(SenseType, X, Y, Weight), !.
similar(SenseType, X, Y, Weight) :-
  (SenseType=nounSenseType;SenseType=verbSenseType),
  wnSemTools <- getWuPalmerSimilarity(X, Y) returns Weight, !.

% ADJECTIVES - we use the 'Similar' synset link for adjectives
% we define similar as pow(0.9, dist(X,Y)) where distance is the number
% of similarity links that must be traversed. If there is no connection, the
% weight is 0
similar(adjSenseType, X, Y, Weight) :-
   wnSemTools <- getPathLength(X, Y) returns Length,
   Length > 0 -> Weight is 0.9 ** Length; Weight is 0.

% There is nothing we can do for adverbs - their similarity must be assessed
% through their adjective form (obtain via WordNet DerivedFrom link) whenever
% available

%-----------------------------------------------------------------------------------------------------------
% UTILITIES AND BASIC FACTS
%-----------------------------------------------------------------------------------------------------------
posSenseType(noun, nounSenseType).
posSenseType(adjective, adjSenseType).
posSenseType(adverb, adverbSenseType).
posSenseType(verb, verbSenseType).

wordSense(SenseType, Word, Sense) :- 
  (bound(Word) ->
  	(bound(Sense) ->
  		hg <- target(Sense) returns C, C <- satisfies(graph, Word) returns true;
  		AllSenses is hg_find_all(and(type(SenseType), incident(Word))), collection_item(AllSenses, Sense)
  	 );
  	 (bound(Sense) -> 
  		All is hg_find_all(target(Sense)), collection_item(All, Word);
  		wn_word(Word), wordSense(SenseType, Word, Sense))).

senseIsa(Child, Parent) :- directIsa(Child, Parent), !.
senseIsa(Child, Parent) :-	directIsa(Child, X), senseIsa(X, Parent).
directIsa(Child, Parent) :- wn_kindof(Child, Parent) ; wn_instanceof(Child, Parent).

atomLemma(Lemma, Atom) :-
  wngraph <- findWord(Lemma) returns Atom.

samePartOfSpeech(word(_, Pos, _, _), word(_, Pos, _, _)).

ignoreWord(have).
ignoreWord(be).
ignoreWord('I').
ignoreWord(them).
ignoreWord(they).
ignoreWord(their).
ignoreWord(he).
ignoreWord(his).
ignoreWord(him).
ignoreWord(she).
ignoreWord(her).
ignoreWord(hers).
ignoreWord(you).
ignoreWord(your).
ignoreWord(yours).
