disco_home('d:/work/disco/trunk').
hgdb_location('c:/tmp/graphs/wordnet').

init_tests :-
  consult('d:/work/disco/trunk/src/test/test/disco/relex/nlp_features.pl'),
  consult('c:/temp/relexTests.pl').

testParses(File) :-
  consult(File),
  tell(stdout),
  init_disco,
  loopAllParses,
  told.

init_disco :-
  disco_home(Home),
  hgdb_location(HGDB),
  java_object('org.disco.Disco', [Home, HGDB], D),
  D <- init,
  assert(disco(D)),!.

init_disco :- write("Failed to initialize Disco!").

loopAllParses :-
  relexParse(Sentence, Parse),
  checkParse(Sentence, Parse),
  fail.
loopAllParses.

checkParse(Sentence, Parse) :-
  disco(D),
  java_object('org.disco.StringTextDocument', [Sentence], Document),
  D <- pushDocument(Document) returns Context,
  D <- process(Context),
  parseResult(Context, ParseResult),
  unifyParseResult(Parse, ParseResult, Sentence),
  !.
checkParse(Sentence, Parse) :- write('Testing failing, checkout exceptions....'), nl.

unifyParseResult(ParseData, ParseResult, Sentence) :-
  unify_fs(ParseData, ParseResult).
unifyParseResult(ParseData, ParseResult, Sentence) :-
  assert(testFailure(relexParse, Sentence, ParseData, ParseResult)).

parseResult(Context, ParseResult) :-
  Context <- getTemporary('RelexParses') returns ParseMap,
  map_entry(ParseMap, SentenceHandle, Parses),
  map_entry(Parses, Parse, Relations),
  class('test.disco.relex.TestUtils') <- getHeadAndBackground(Parse) returns TestCase,
  ParseResult is parse_term(TestCase).

loopFailures :-
  testFailure(X,Y,Z,D),
  write('Failed on sentence '), write(Y), nl,
  fail.
loopFailures.

doit :-  init_tests, init_disco, loopAllParses, loopFailures.