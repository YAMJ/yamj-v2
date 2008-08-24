function loadsort() 
{
         if(selsort==0 && selidx==0)
         { 
             sel0();
    
         }
         else if (selsort==0 && selidx==1)
         { 
             sela();

         } 
         else if (selsort==0 && selidx==2)
         { 
             selb();

         }
         else if (selsort==0 && selidx==3)
         { 
             selc();

         }
         else if (selsort==0 && selidx==4)
         {
             seld();

         }
         else if (selsort==0 && selidx==5)
         {
             sele();

         }
         else if (selsort==0 && selidx==6)
         {
             self();

         }
         else if (selsort==0 && selidx==7)
         { 
             selg();

         }
         else if (selsort==0 && selidx==8)
         { 
             selh();

         }
         else if (selsort==0 && selidx==9)
         { 
             seli();

         }
         else if (selsort==0 && selidx==10)
         { 
             selj();

         }
         else if (selsort==0 && selidx==11)
         { 
             selk();

         }
         else if (selsort==0 && selidx==12)
         { 
             sell();

         }
         else if (selsort==0 && selidx==13)
         { 
             selm();

         }
         else if (selsort==0 && selidx==14)
         { 
             seln();

         }
         else if (selsort==0 && selidx==15)
         { 
             selo();

         }
         else if (selsort==0 && selidx==16)
         { 
             selp();

         }
         else if (selsort==0 && selidx==17)
         { 
             selq();

         }
         else if (selsort==0 && selidx==18)
         {
             selr();

         }
         else if (selsort==0 && selidx==19)
         { 
             sels();

         }
         else if (selsort==0 && selidx==20)
         {
             selt();

         }
         else if (selsort==0 && selidx==21)
         { 
             selu();

         }
         else if (selsort==0 && selidx==22)
         { 
             selv();

         }
         else if (selsort==0 && selidx==23)
         {
             selw();

         }
         else if (selsort==0 && selidx==24)
         {
             selx();

         }
         else if (selsort==0 && selidx==25)
         {
             sely();

         }
         else if (selsort==0 && selidx==26)
         { 
             selz();

         }
         else if (selsort==1 && selidx==0)
         { 
             selaction();

         }
         else if (selsort==1 && selidx==1)
         { 
             selall();

         }
         else if (selsort==1 && selidx==2)
         { 
             selanimation();

         }
         else if (selsort==1 && selidx==3)
         { 
             selcomedy();

         }
         else if (selsort==1 && selidx==4)
         {
             selcrime();

         }
         else if (selsort==1 && selidx==5)
         { 
             seldrama();

         }
         else if (selsort==1 && selidx==6)
         { 
             selfamily();

         }
         else if (selsort==1 && selidx==7)
         { 
             selfantasy();

         }
         else if (selsort==1 && selidx==8)
         { 
             selhd();

         }
         else if (selsort==1 && selidx==9)
         { 
             selhistory();

         }
         else if (selsort==1 && selidx==10)
         { 
             selhorror();

         }
         else if (selsort==1 && selidx==11)
         {
             selmusical();

         }
         else if (selsort==1 && selidx==12)
         { 
             selother();

         }
         else if (selsort==1 && selidx==13)
         { 
             selrealitytv();

         }
         else if (selsort==1 && selidx==14)
         { 
             selscifi();

         }
         else if (selsort==1 && selidx==15)
         { 
             selthriller();

         }
         else if (selsort==1 && selidx==16)
         {
             seltvshow();
         }  
   if (parms==0)
   {
     if ( total>2)
     {
       now = total/2+1;
     }
     else
     {
       now = total/2;
     }         
   }
   loop();
}