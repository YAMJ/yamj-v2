function passit(jumploc) // pass the sort option and the now value to the help screen
{
 var jumpurl = jumploc+'?pnow=';
     jumpurl = jumpurl+now+'';
     jumpurl = jumpurl+'&pselsort=';
     jumpurl = jumpurl+selsort+'';
     jumpurl = jumpurl+'&pselidx=';
     jumpurl = jumpurl+selidx+'';    

     location.assign(jumpurl);
}