
 char delay_line[] = {0,0,0,0};

 char d1,d2,d3,d4; 

char fifo(char a,char b)
{
    int i;
    char result = delay_line[3];
	

    if(a>0)
    {
            
        for(i=2,a=a+1; i-->0; )
        {
            delay_line[i] = delay_line[i - 1];
            a=a+b;
        }
	
	/*delay_line[3] = delay_line[2];
	  delay_line[2] = delay_line[1];
	  delay_line[1] = delay_line[0];
	*/
	   
	delay_line[0] = a+i;
    }
    return(result);
	

    /*int result = d1;
      d1 = d2;
      d2 = d3;
      d3 = d4;
      d4 = a;
      return result;
    */
}
