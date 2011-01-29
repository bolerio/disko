declare default element namespace "http://www.w3.org/1999/xhtml";
declare namespace java = "java:org.disco.flow.analyzers.XQueryAnalyzer";

for $d in //td[@class="bioTd"]
let $name := data($d//td/strong/a)
let $contact := data($d//tr[2])
let $address := replace(data($d//tr[3]),'Address: ','')
let $phone := replace(data($d//tr[4]),'Phone: ','')
let $mission := replace(data($d//tr[5]//p),'Mission: ','')
where java:isPhone($phone) and java:isAddress($address)
return
<department>
<name>{$name}</name>
<contact>{$contact}</contact>
<address>{$address}</address>
<phone>{$phone}</phone>
<mission>{$mission}</mission>
</department>
