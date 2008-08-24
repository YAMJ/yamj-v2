function passparm()
{    
   var parameters = location.search.substring(1).split("&");
   var temp = parameters[0].split("=");
   var key  = temp[0];              

   if (key=='pnow')
   {
     pnow = unescape(temp[1]);
   }

   temp = parameters[1].split("=");
   key  = temp[0];
  
   if (key=='pselsort')
   {
     pselsort = unescape(temp[1]);
   }

   temp = parameters[2].split("=");
   key  = temp[0];

   if (key=='pselidx')
   {
      pselidx = unescape(temp[1]);
   }
  
   // did we get good parameters
   if (pnow &&  pselsort &&  pselidx)
      {
         parms    = 1;
         selsort  = parseInt(pselsort);
         selidx   = parseInt(pselidx);
         now      = parseInt(pnow);
      }
}