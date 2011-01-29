(: Drafts and fragments; ignore :)
(:

let $n := $raw/td/table/tbody/tr/td/br/following-sibling::* 
let $name := $raw//br/ancestor::*[1]/child::text()[1] 
let $name := $raw//child::*[text()][1] 
let $info := $raw//br/following-sibling::text() 
let $info := string-join($left-column//text(),';')
<raw>{$raw}</raw>
let $phone := string-join($right-column//text(),' - ') 

:)

