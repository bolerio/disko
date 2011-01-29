module namespace disco = "http://www.kobrix.com";
import module namespace functx = "http://www.functx.com" at "functx-1.0-doc-2007-01.xq";
 
declare function disco:clean 
	( $arg as xs:string* )  as xs:string {
	functx:trim(normalize-space(replace(string-join($arg,' '),'&#160;',' ')))
} ;
 
declare function disco:remove-formatting 
	( $nodes as node()*)  as node()* {
	functx:remove-elements-not-contents($nodes, ('a','strong','p'))	
} ;

declare function disco:get-phones
	( $arg as xs:string? ) as xs:string* {
	string-join(functx:get-matches($arg, '[^0-9] *[0-9]{3}[ -\)]+[0-9A-Z]{3}[ -][0-9A-Z]{4}'), ' ')
} ;

(: Return TD cells that don't have other cells inside and start with the given argument :)
declare function disco:get-cell-starting-with($context as node()*, $arg as xs:string) as xs:string {
disco:clean(replace($context//td[starts-with(., $arg) and not(exists(.//td))], $arg, ''))
} ;