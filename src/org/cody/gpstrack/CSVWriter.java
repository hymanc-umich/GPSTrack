package org.cody.gpstrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import android.util.Log;

///implements writing snapshot data to the comma separated CSV - file

public class CSVWriter 
{
	public static final String TAG = "CSVWriter";
    private RandomAccessFile raf;
    private char separator;
    private char quotechar;
    private char escapechar;
    private String lineEnd;

    /// The character used for escaping quotes. /
    public static final char DEFAULT_ESCAPE_CHARACTER = '"';

    /// The default separator to use if none is supplied to the constructor. 
    public static final char DEFAULT_SEPARATOR = ',';

    ///The default quote character to use if none is supplied to the constructor.
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    ///The quote constant to use when you wish to suppress all quoting.
    public static final char NO_QUOTE_CHARACTER = '\u0000';

    /// The escape constant to use when you wish to suppress all escaping. /
    public static final char NO_ESCAPE_CHARACTER = '\u0000';

    /// Default line terminator uses platform encoding. 
    public static final String DEFAULT_LINE_END = "\n";

    private ArrayList<String> list =new ArrayList<String>();
    
    private File mfile;
    
    public boolean is_created = true;
    
    ///Constructs CSVWriter using a comma for the separator.
    public CSVWriter(File file) throws FileNotFoundException 
    {
        this(file, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
            DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
    }
    
   /// Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
	public CSVWriter(File file, char separator, char quotechar, char escapechar, String lineEnd) throws FileNotFoundException {
        this.raf = new RandomAccessFile(file,"rw");
        this.separator = separator;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;
        this.mfile = file;
        
        is_created = true;
    }
    
    /// Writes the next line to the file.
   public void writeNext(ArrayList<String> snapshotData) 
   {   
	   try
	   {
		   list=snapshotData;
		   Log.i(TAG,"writeNext");
		   if (snapshotData == null)
			   return;
		   StringBuffer sb = new StringBuffer();
		   for (int i = 0; i <  list.size(); i++) 
		   {
			
			   ///add comma
			   if (i != 0) 
			   {
				   sb.append(separator);
	           }

			   String nextElement = list.get(i);
			   if (nextElement == null)
				   continue;
	           for (int j = 0; j < nextElement.length(); j++) 
	           {
	        	   char nextChar = nextElement.charAt(j);
	        	   if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) 
	        	   {
	        		   sb.append(escapechar).append(nextChar);
	        	   } 
	        	   else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) 
	        	   {
	        		   sb.append(escapechar).append(nextChar);
	        	   } 
	        	   else 
	        	   {
	        		   sb.append(nextChar);
	        	   }
	           }
	          
	        }
	
	        sb.append(lineEnd);
	        try 
	        {
	        	if(raf!=null)
	        		raf.writeBytes(sb.toString());
			} 
	        catch (IOException e) 
	        {
	        	Log.e(TAG, "IOException in writeNext");
			}
	   }
	   catch(Exception e)
	   {
		   Log.e(TAG, "Exception in writeNext:" + e.toString() + ":" + e.getMessage(),e);
	   }

    }
   
   public void changePostionToHeader()
   {
	   try 
	   {
		   raf.seek(0);
	   } 
	   catch (IOException e) 
	   {
		   e.printStackTrace();
	   }
   }
   
   public long getFileLength() throws IOException
   {
	 return raf.length();
   }
   
   /// Close the underlying stream writer flushing any buffered content.
   public void close() throws IOException 
   {
	   Log.i(TAG, "Close()");
	   is_created = false; 
	   raf.close();
   }
}
