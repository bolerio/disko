import module namespace functx = "http://www.functx.com" at "../../xquery-lib/functx-1.0-doc-2007-01.xq";
import module namespace disco = "http://www.kobrix.com" at "../../xquery-lib/disco.xq";
declare namespace java = "java:org.disco.flow.analyzers.XQueryAnalyzer";


let $name := disco:clean(//p[@class='bodytitle'])
let $hours := disco:get-cell-starting-with(.,"Library Hours:")
let $phone := disco:get-cell-starting-with(.,"Telephone:")
let $address := disco:get-cell-starting-with(.,"Address")
return 
if (not(functx:all-whitespace($hours))) then 
<library>
	<name>{$name}</name>
	<hours>{$hours}</hours>
	<phone>{$phone}</phone>
	<address>{$address}</address>
</library> 
else
<library />
