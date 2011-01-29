/* IMPORTANT: Put script after tooltip div or
     put tooltip div just before </BODY>. */

var dom = (document.getElementById) ? true : false;
var ns5 = ((navigator.userAgent.indexOf("Gecko")>-1) && dom) ? true: false;
var ie5 = ((navigator.userAgent.indexOf("MSIE")>-1) && dom) ? true : false;
var ns4 = (document.layers && !dom) ? true : false;
var ie4 = (document.all && !dom) ? true : false;
var nodyn = (!ns5 && !ns4 && !ie4 && !ie5) ? true : false;

// avoid error of passing event object in older browsers
if (nodyn) event = "nope";

///////////////////////  CUSTOMIZE HERE   ////////////////////
// settings for tooltip
// Do you want tip to move when mouse moves over link?

var tipFollowMouse	= true;
var tipWidth 			 	= 240;
var tipFontFamily 	= "Verdana, arial, helvetica, sans-serif";
var tipFontSize			= "8pt";
var tipFontColor		= "#000000";
var tipBgColor 			= "#DDECFF";
var tipBorderColor 	= "#000000";
var tipBorderWidth 	= 1;
var tipBorderStyle 	= "solid";
var tipPadding		 	= 4;
var tipHideAfter                = 100;

//document.writeln('<DIV id=tipDiv style="POSITION: absolute; VISIBILITY: hidden; Z-INDEX: 100"></DIV>');

var offX					 	= 8;	// how far from mouse to show tip
var offY					 	= 12;
var tipFontSizeNum = 8;

////////////////////  END OF CUSTOMIZATION AREA  ///////////////////

////////////////////////////////////////////////////////////
//  initTip	- initialization for tooltip.
//		Global variables for tooltip.
//		Set styles for all but ns4. Set width of tooltip.
//		Set up mousemove capture if tipFollowMouse set true.
////////////////////////////////////////////////////////////
var tooltip, tipcss;
function initTip() {
    if (nodyn) return;
    tooltip = (ns4)? document.tipDiv.document: (ie4)? document.all['tipDiv']: (ie5||ns5)? document.getElementById('tipDiv'): null;
    tipcss = (ns4)? document.tipDiv: tooltip.style;
    if (ns4) tooltip.width = tipWidth;
    else if (ns5) tipcss.width = tipWidth;
    else if (ie4||ie5) tipcss.pixelWidth = tipWidth;
    if (ie4||ie5||ns5) {	// ns4 would lose all this on rewrites
        tipcss.fontFamily = tipFontFamily;
        tipcss.fontSize = tipFontSize;
        tipcss.fontColor = tipFontColor;
        tipcss.backgroundColor = tipBgColor;
        tipcss.borderColor = tipBorderColor;
        tipcss.borderWidth = tipBorderWidth;
        tipcss.padding = tipPadding;
        tipcss.borderStyle = tipBorderStyle;
    }
    if (tipFollowMouse) {
        if (ns4) document.captureEvents(Event.MOUSEMOVE);
        document.onmousemove = trackMouse;
    }
}
window.onload = initTip;

/////////////////////////////////////////////////////////////
//  Note on tipFollowMouse:
//		If tipFollowMouse is set true, positionTip is called
//		from trackMouse and tooltip is made visible at end of
//		doTooltip function.
//		If tipFollowMouse is set false, positionTip is called
//		from doTooltip and the tooltip is made visible at the
//		end of the positionTip function.
/////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////
//  showtip function
//			Assembles content for tooltip and writes it to tipDiv.
//			Call positionTip function from here if tipFollowMouse
//			is set to false.
//////////////////////////////////////////////////////////////
var t1,t2;	// for setTimeouts
function showtip(evt,txt) {
    if (!tooltip) return;
    if (t1) clearTimeout(t1);	if (t2) clearTimeout(t2);

    //Un ugly workaround for the case when the passed text is
    //considerably smaller that the defined tipWidth
    //Must be a better way to do this
    var tempWidth=tipWidth;
    if ((txt.length * tipFontSizeNum)<(0.8*tipWidth))
       tipWidth=txt.length * tipFontSizeNum;

    if (ns4) {
        tip = '<TABLE BGCOLOR="' + tipBorderColor + '" WIDTH="' + tipWidth + '" CELLSPACING="0" CELLPADDING="' + tipBorderWidth + '" BORDER="0"><TR><TD><TABLE BGCOLOR="' + tipBgColor + '" WIDTH="100%" CELLSPACING="0" CELLPADDING="' + tipPadding + '" BORDER="0"><TR><TD><SPAN STYLE="font-family:' + tipFontFamily + '; font-size:' + tipFontSize + '; color:' + tipFontColor + ';">' + txt  + '</SPAN></TD></TR></TABLE></TD></TR></TABLE>';
        tooltip.write(tip);
        tooltip.close();
    } else if (ie4||ie5||ns5) {
        tip = txt;
        tipcss.width = tipWidth;
        tooltip.innerHTML = tip;
    }
    tipWidth=tempWidth;
    if (!tipFollowMouse) positionTip(evt);
    else {
        if (t1) clearTimeout(t1); if (t2) clearTimeout(t2);
        t1=setTimeout("tipcss.visibility='visible'",1000);
            t2=setTimeout("tipcss.visibility='hidden'",((tipHideAfter+1)*1000));
    }
}

var mouseX, mouseY;
function trackMouse(evt) {
    if (ns4||ns5) {
        mouseX = evt.pageX;
        mouseY = evt.pageY;
    } else if (ie4||ie5) {
        mouseX = window.event.clientX + document.body.scrollLeft;
        mouseY = window.event.clientY + document.body.scrollTop;
    }
    positionTip(evt);
}

/////////////////////////////////////////////////////////////
//  positionTip function
//		If tipFollowMouse set false, so trackMouse function
//		not being used, get position of mouseover event.
//		Calculations use mouseover event position,
//		offset amounts and tooltip width to position
//		tooltip within window space available.
/////////////////////////////////////////////////////////////
function positionTip(evt) {
    if (ns4) {
        if (!tipFollowMouse) {mouseX=evt.pageX; mouseY=evt.pageY;	}
        if ((mouseX+offX+tooltip.width) > (window.innerWidth-20 + window.pageXOffset)) tipcss.left = mouseX - (tooltip.width + offX);
        else tipcss.left = mouseX + offX;
        if ((mouseY+offY+tooltip.height) > (window.innerHeight-20 + window.pageYOffset)) tipcss.top = mouseY - (tooltip.height + offY);
        else tipcss.top = mouseY + offY;
    }

    else if (ie4||ie5) {
        if (!tipFollowMouse) {
            mouseX = window.event.clientX + document.body.scrollLeft;
            mouseY = window.event.clientY + document.body.scrollTop;
        }
        if ((mouseX+offX+tooltip.clientWidth) > (document.body.clientWidth + document.body.scrollLeft))
            tipcss.pixelLeft = mouseX-(tooltip.clientWidth + offX);
        else tipcss.pixelLeft = mouseX + offX;

        if ((mouseY+offY+tooltip.clientHeight) > (document.body.clientHeight+document.body.scrollTop))
            tipcss.pixelTop = mouseY-(tooltip.clientHeight + offY);
        else tipcss.pixelTop = mouseY + offY;
    }

    else if (ns5) {
        if (!tipFollowMouse) {mouseX=evt.pageX; mouseY=evt.pageY;}
        if ((mouseX+offX+tooltip.offsetWidth) > (window.innerWidth-20 + window.pageXOffset))
            tipcss.left = mouseX - (tooltip.offsetWidth + offX);
      else tipcss.left = mouseX + offX;

        if ((mouseY+offY+tooltip.offsetHeight) > (window.innerHeight-20 + window.pageYOffset))
            tipcss.top = mouseY - (tooltip.offsetHeight + offY);
      else tipcss.top = mouseY + offY;
    }
    if (!tipFollowMouse) {
        if (t1) clearTimeout(t1); if (t2) clearTimeout(t2);
        t1=setTimeout("tipcss.visibility='visible'",800);
            t2=setTimeout("tipcss.visibility='hidden'",(tipHideAfter+1)*1000);
    }
}

function hidetip() {
    if (!tooltip) return;
    tipcss.visibility = "hidden";
    if (t1) clearTimeout(t1); if (t2) clearTimeout(t2);
}
