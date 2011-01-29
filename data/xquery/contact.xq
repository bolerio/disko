import module namespace functx = "http://www.functx.com" at "../../xquery-lib/functx-1.0-doc-2007-01.xq";
import module namespace disco = "http://www.kobrix.com" at "../../xquery-lib/disco.xq";
declare namespace java = "java:org.disco.flow.analyzers.XQueryAnalyzer";

for $row in //tr

let $left-column := disco:remove-formatting($row/td[1])
let $right-column := disco:remove-formatting($row/td[2])
let $name := disco:clean($left-column//text()[1])
let $info := disco:clean($left-column//text()[position() > 1])
let $phone := disco:clean($right-column//text())

where java:isPhone($phone) and not(functx:all-whitespace($name)) and string-length($name)<50
return
if (java:isAddress($info)) then
<contact>
	<name>{$name}</name>
	<address>{$info}</address>
	<phone>{$phone}</phone>
</contact>
else
<contact>
	<name>{$name}</name>
	<info>{$info}</info>
	<phone>{$phone}</phone>
</contact> 