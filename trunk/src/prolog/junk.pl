
%-----------------------------------------------------------------------------------------------------------
% SOME OLD STUF...
%-----------------------------------------------------------------------------------------------------------	
makeSense :-
  java_object('java.util.ArrayList', [], rules),
  occurrencesToFacts,
  similarityThreshold(Threshold),
  similarityMeasure(Measure),
  findCoOccurrences(Threshold, Measure),
  findMonosemous,  
  applyRules.
 
applyRules :-
  rules <- clone returns Copy,  rules <- clear.
  forall [collection_item(Copy, Rule)] ==> [fire(Rule)],
  applyRules.
applyRules :- write('Done with rules.'), nl.


findMonosemous :-
  forall [
	Occurrence : word(Lemma, PoSpeech, Position, Atom),
	posSenseType(PoSpeech, SenseType),
    (1 is hg_count(and(type(SenseType), incident(Atom))) 
        -> wordSense(SenseType, Atom, S)
        ; entitySense(Lemma, S))
  ] ==> [
    senseFound(Occurrence, S, 1.0, [])
  ].


% senseFound will assert a new fact about the newly found sense if the inference is valid
senseFound(Occurrence, Sense, Confidence, Premises) :-
    validateSenseInference(Premises, Occurrence),
    Fact = senseOf(Occurrence, Sense, Confidence, Premises),
    assert(Fact),
    addRulesFor(Fact).
senseFound(_, _, _, _).

% An sense inference is valid if the set of premises used does not involve
% a sense of the occurrence about which we are making the inference. 
validateSenseInference([senseOf(X, _, _, Premises) | Rest], Occurrence) :-
	!,
	X \= Occurrence,
	validateSenseInference(Premises, Occurrence),
	validateSenseInference(Rest, Occurrence).
validateSenseInference([_ | Rest], Occurrence) :-
	!,
	validateSenseInference(Rest, Occurrence).	
validateSenseInference([], _).
	   
addRulesFor(senseOf(Occurrence, Sense, Confidence, Premises)) :-
   forall [metarule(Rule, senseOf(Occurrence, Sense, Confidence, Premises))]
   	==> [rules <- add(Rule)].

% This rule implements the one sense per discourse heuristic
metarule(Rule, SenseFact) :-
    SenseFact = senseOf(Occurrence, Sense, Confidence, _),
    Occurrence = word(_, PoSpeech, Position, Atom),
    Y : word(_, PoSpeech, Position2, Atom),
    Position \= Position2,
    Rule = (forone [Y] ==> [senseFound(Y, Sense, Confidence, [SenseFact])]).

% This rule finds senses based on similarity between grammatically co-occurring
% words.
metarule(Rule, SenseFact) :-
	SenseFact = senseOf(Occurrence, Sense, Confidence, _),
	CoOccFact = co_occurrence(Relation, Occurrence, word(Lemma, Speech, Pos, Atom), CoWeight),
    Premises = [CoOccFact, similarSense(Atom, Sense, Result, SimWeight)],
    Conclusion = [NewConfidence is CoWeight*SimWeight*Confidence,
    			  senseFound(word(Lemma, Speech, Pos, Atom), 
    						 Result, 
    						 Confidence,
    						 [SenseFact, CoOccFact])],    				
    Rule = (forall Premises ==> Conclusion).

