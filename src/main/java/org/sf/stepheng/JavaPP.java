package org.sf.stepheng;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.IntHolder;

public class JavaPP {

    private String currentfilename = null ;

    private int filesize = 0 ;

    private LinkedList<Pattern> patterns = null ;
    
    // Patterns which use identical regex strings
    // do NOT match with p1.equals(p2) and so the
    // matcher functions don't always return true
    // when a human would expect them to.
    // So we need to keep the strings.
    //
    private LinkedList<String> patstrings = null ;
    
    private LinkedList<String> outputs = null ;
    
    private LinkedList<String> lines = null ;
    
    private Stack<Object> stack = null ;
    
    private int iflevel = 0 ;
    
    private Stack<Boolean> ifstate = null ;

    private boolean __debugme = false ;
	
    public static void report( Object... objs )
    {
        for( Object obj : objs )
        {
            System.err.print( obj ) ;
        }
        
        System.err.println( "" ) ;
        
        System.err.flush() ;
    }
    
    /**********************************************************
     */
    
    /*
     * Clean a pattern string ( prior to adding it )
     *
     * This means removing comments and extraneous spaces.
     */
     
    public static String cleanPattern( String pats )
    {
        int len = pats.length() ;
        
        if( len <= 1 )
        {
            // can't have any comment in it
        
            return pats.trim() ;
        }
        
        char c = pats.charAt(0) ;
        
        if( ( c == '/' ) && ( pats.charAt(1) == '/' ) )
        {
            // whole thing in a comment
            
            return "" ;
        }
        
        StringBuilder retsb = new StringBuilder() ;
        
        boolean incomment = false ;
        
        char lastc = (char)-1 ;
        
        int i = 1 ;
        
        while( i < len )
        {
            lastc = c ;
            c = pats.charAt(i) ;
        
            if( incomment )
            {
                // ignore stuff until we reach an end of comment marker
                
                if( ( lastc == '*' ) && ( c == '/' ) )
                {
                    incomment = false ;
                } 
            }
            else
            {
                if( ( lastc == '/' ) && ( c == '/' ) )
                {
                    // end of line comment marker
                    // we can terminate this entire process
                    
                    incomment = true ;
                    
                    break ;
                }
            
                if( ( lastc == '/' ) && ( c == '*' ) )
                {
                    incomment = true ;
                    
                    // can't end a comment for at least a couple of more chars
                    i += 2 ;
                    
                    continue ;
                }
                
                retsb.append( lastc ) ;
            }
        
            i++ ;
        };
        
        // at this point the last character (c) is not yet written to the buffer
        
        if( ! incomment )
        {
            retsb.append(c) ;
        }
        
        i = 0 ;
        
        while( retsb.charAt(i) == ' ' )
        {
            i++ ;
        };
        
        int j = retsb.length() - 1 ;
        
        while( ( j > 0 ) && ( retsb.charAt(j) == ' ' ) )
        {
            j-- ;
        };
        
        j++ ;
        
        return retsb.substring( i, j ) ;
    }
    
    /*
     * Add a new pattern to the list
     */
     
    public void addPattern( String pats, String outs )
    {
        __debugme = true ;
    
        String cleanouts = null ;
        
        cleanouts = cleanPattern( outs ) ;
        
        /*
        if( DEBUG_ENV.equals( "addPattern" ) )
        {
            debug( "Adding  pattern :: ", pats ) ;
            debug( "Adding  output  :: ", outs ) ;
            debug( "Cleaned output  :: ", cleanouts ) ;
            debug( "---------------------------------" ) ;
        }
        */
        
        patterns.add( Pattern.compile( pats, Pattern.MULTILINE ) ) ;
        
        patstrings.add( pats ) ;
        
        outputs.add( cleanouts ) ;
        
        __debugme = false ;
    }

    
    public void define( String defname ) {
    	
    	if( patterns == null )
        {
            patterns   = new LinkedList<Pattern>() ;
            
            outputs    = new LinkedList<String>() ;
            
            patstrings = new LinkedList<String>() ;
            
            if( ( patterns == null ) || ( outputs == null ) || ( patstrings == null ) )
            {
                report( "Could not create internal data structure to store macros." ) ;
                
                return ;
            }
        }
    	
        defname = makeToken( defname ) ;
        
        int pos = -1 ;
        
        pos = patstrings.indexOf( defname ) ;
        
        if( pos == -1 )
        {
            addPattern( defname, "" ) ;
        }
        else
        {
            String outs2 = null ;
            
            outs2 = cleanPattern( "" ) ;
            
            outputs.set( pos, outs2 ) ;
        }
    }
    
    /**********************************************************
     */
    




    








    public static void closeStream( Closeable st )
    {
        try
        {
            if( st != null )
            {
                if( st instanceof Flushable )
                {
                    ( (Flushable)st ).flush() ;
                }
                
                st.close() ;
            }
        }
        catch( IOException ioe ) { }
    }
    
    /*
     * Process an external command from lines at the given position,
     *
     * Stops when a line matches the regex "^#\\s*end-command\\s*(?://.*)?$"
     *
     * This code may have to insert lines into the line list.
     */
    
    private void processCommand( String cmd, int startingline )
    {
        // create a process
        
        ProcessBuilder pb = new ProcessBuilder( cmd.split(" ") ) ;
        
        if( pb == null )
        {
            // cannot create the process
            
            return ;
        }
    
        pb.redirectError( ProcessBuilder.Redirect.INHERIT ) ;
        
        Process proc = null ;
        
        try {  proc = pb.start()  ; } catch( IOException ioe ) { } ;
        
        if( proc == null )
        {
            report( "Could not start process from ", cmd, " on line ", startingline, " in ", currentfilename ) ;
        
            return ;
        }
        
        InputStreamReader pisr = new InputStreamReader( proc.getInputStream() ) ;
        
        OutputStreamWriter posw = new OutputStreamWriter( proc.getOutputStream() ) ;
        
        if( ( pisr == null ) || ( posw == null ) )
        {
            if( __debugme ) { report( "At line ", 316, " in ", currentfilename, " ",  "Could not get process streams."  ) ; } ;
            
            closeStream( pisr ) ;
            closeStream( posw ) ;
            
            return ;
        }
        
        // send input to the process
    
        int k = 0 ;
        
        int numlines = lines.size() ;
        
        LinkedList<String>  outlines = new LinkedList<String>() ;
        
        String t = null ;
        
        Pattern p = Pattern.compile( "^#\\s*end-command\\s*$" ) ;
        
        boolean endcommandfound = false;
        
        for( k = startingline+1 ; k < numlines ; k++ )
        {
            t = lines.get(k) ;
            
            if( p.matcher(t).matches() )
            {
                // found end of command input
                
                endcommandfound = true ;
            
                break ;
            }
            
            // feed that line to the output process
            
            try {  posw.write( t )  ; } catch( IOException ioe ) { } ;
                
            try {  posw.write( (int)'\n' )  ; } catch( IOException ioe ) { } ;
        }
        
        // NOTE : we use k later on !
        
        // Note that we could check for an "end-command" before
        // we run out of input lines, but in a practical sense
        // this would just force a developer to add a useless
        // end-command to the end of a file.
        //
        // we do need to at least warn about a missing directive
        
        if( ! endcommandfound )
        {
            report( "WARNING :: Did not find \"#end-command\" before end of file to match \"#command\" at line ", startingline, " in ", currentfilename ) ;
        }
        
        try {  posw.flush()  ; } catch( IOException ioe ) { } ;
        
        closeStream( posw ) ;
        
        // fetch the output of the command into lines
        
        StringBuilder sb = new StringBuilder() ;
        
        int c = 0 ;
        
        while( c != -1 )
        {
            try {  c = pisr.read()  ; } catch( IOException ioe ) { } ;
            
            if( ( c != -1 ) && ( c != (int)'\n' ) )
            {
                sb.append( (char)c ) ;
            }
            
            /*
            if( __debugme && ( c != -1 ) )
            {
                System.err.printf( "char = [%04x] %c\n", c, (char)c ) ;
            }
            */
            
            if( c == (int)'\n' )
            {
                // write the output
                
                outlines.add( sb.toString() ) ;
                
                // reset the string builder
            
                sb.setLength( 0 ) ;
            }
        }
        
        closeStream( pisr ) ;
        
        // now replace input lines with output lines
        // which lets the preprocessor process them further
        
        // remove all the lines from k to startingline inclusive
        // note the order we remove which is easier
        
        while( k >= startingline )
        {
            lines.remove( k ) ;
            
            k-- ;
        };
        
        // now add the new lines from starting line
        // note the order we add which is easier
        
        int i = outlines.size() ;
        
        while( i > 0 )
        {
            i-- ;
            
            lines.add( startingline, outlines.get(i) ) ;
            
            // if( __debugme ) { report( "At line ", 436, " in ", currentfilename, " ",  outlines.get(i)  ) ; } ;
        };
    }
    
    /**********************************************************
     */


    private boolean evaluate( String expr )
    {
        boolean errorhappened = false ;
    
        StringBuilder sb = new StringBuilder() ;
        
        // this routine builds a sting without spaces and with
        // the tokens replaced by true or false values
        // This simplifies parsing the expression, although it's
        // not the most efficient way to do it.
        
        boolean notpending = false ;
        
        int len = expr.length() ;
        
        int i = 0 ;
        
        char c = 0 ;
        
        // check for symbols and resolve them, removing spaces as we go
        
        for( i = 0 ; i < len ; i++ )
        {
            c = expr.charAt(i) ;
            
            if( Character.isJavaIdentifierStart(c) )
            {
                // read the identifier and determine if it
                // exists, output 't' or 'f' for that result.
                
                StringBuilder sb2 = new StringBuilder() ;
                
                sb2.append( c ) ;
                
                i++ ;
                
                while( i < len )
                {
                    c = expr.charAt(i) ;
                    
                    if( ! Character.isJavaIdentifierPart(c) )
                    {
                        // end of identifer.
                        // because identifiers are the first thing checked we
                        // can proceed with this new character on the rest of
                        // the checks in the main loop
                    
                        break ;
                    }
                    
                    sb2.append(c) ;
                    
                    i++ ;
                };
                
                // at this point we have an identifier in sb2
                
                // TODO :
                //
                // Test for a string equality sequence.
                // There are two possible forms :
                //   (1) x = y
                //   (2) y = "stuff"
                //
                // We use the ~ operator for case ignoring matches,
                // and the = operator for case sensitive matches.
                //
                // Note that we don't allow "stuff" == y
                
                while( ( i < len ) && Character.isWhitespace(c) )
                {
                    i++ ;
                    
                    c = expr.charAt(i) ;
                };
                
                boolean b = false ;
                
                if( ( c == '=' ) || ( c == '~' ) )
                {
                    boolean ignorecase = false ;
                
                    if( c == '~' )
                    {
                        ignorecase = true ;
                    }
                
                    // found a string equality test
                    
                    // read the next identifier
                    
                    i++ ;
                    
                    // check for double symbols i.e. we accept '=' or '=='
                    // as well as '~' and '~~'
                    
                    if( i < len )
                    {
                        char c2 = expr.charAt(i) ;
                        
                        if( c2 == c )
                        {
                            // consume the character
                        
                            i++ ;
                        }
                    }
                    
                    while( ( i < len ) && Character.isWhitespace( c = expr.charAt(i) ) )
                    {
                        i++ ;
                    };
                    
                    if( ! ( Character.isJavaIdentifierStart(c) || ( c == '"' ) ) ) 
                    {
                        report( "Expecting identifier start or quote after '=' in expression." ) ;
                        report( "\tExpression was [", expr, "]" ) ;
                        report( "\tProblem encountered at character number [", i, "] which is [", c, "]" ) ;
                        
                        return false ;
                    }
                    
                    StringBuilder sb3 = new StringBuilder() ;
                    
                    int j1 = 0 ;
                    int j2 = 0 ;
                    
                    j1 = patstrings.indexOf( makeToken( sb2.toString() ) ) ;
                    
                    String lefts = null ;
                    String rights = null ;
                    
                    lefts = outputs.get(j1) ;
                        
                    i++ ;
                    
                    // could be a string literal or a symbol next
                    
                    if( c == '"' )
                    {
                        while( ( i < len ) && ( ( c = expr.charAt(i) ) != '"' ) )
                        {
                            sb3.append( c ) ;
                            
                            i++ ;
                        }
                        
                        // move past the quote
                        
                        i++ ;
                        
                        j2 = 0 ;
                        
                        rights = sb3.toString() ;
                    }
                    else
                    {
                        sb3.append(c) ;
                        
                        while( ( i < len ) && Character.isJavaIdentifierPart( c = expr.charAt(i) ) )
                        {
                            sb3.append( c ) ;
                            
                            i++ ;
                        };
                        
                        j2 = patstrings.indexOf( makeToken( sb3.toString() ) ) ;
                        
                        if( j2 != -1 )
                        {
                            rights = outputs.get(j2) ;
                        }
                    }
                    
                    if( ( j1 == -1 ) || ( j2 == -1 ) )
                    {
                        b = false ;
                    }
                    else
                    {
                        if( ignorecase )
                        {
                            b = lefts.equalsIgnoreCase( rights ) ;
                        }
                        else
                        {
                            b = lefts.equals( rights ) ;
                        }
                    }
                }
                else
                {
                    // Just a simple identifier existence test.
                    
                    b = tokenExists( sb2.toString() ) ;
                }
                
                b = b ^ notpending ;
                
                notpending = false ;
                
                if( b )
                {
                    sb.append('t') ;
                }
                else
                {
                    sb.append('f') ;
                }
                
                if( i == len )
                {
                    // reached the end of the expression
                    // so force the end of the main loop
                    
                    break ;
                }
            }
            
            if( Character.isWhitespace(c) )
            {
                // ignore whitespace
            
                continue ;
            }
            
            switch( c )
            {
                case '(' :
                    if( notpending )
                    {
                        sb.append( '!' ) ;
                        
                        notpending = false ;
                    }
                    sb.append( '(' ) ;
                    break ;
                case ')' :
                case '&' :
                case '|' :
                case '^' :
                    if( notpending )
                    {
                        report( "Not operation preceeded '",c,"' character, which is illegal." ) ;
                        return false ;
                    }
                    sb.append(c) ;
                    break ;
                case '!' :
                    notpending = notpending ^ true ;
                    break ;
                default :
                    // an illegal character in a boolean expression
                    report( "Illegal character in boolean expression [", c, "] in expression [", expr, "]" ) ;
                    errorhappened = true ;
                    break ;
            }
            
            if( errorhappened )
            {
                return false ;
            }
        }
        
        return eval( sb.toString(), new IntHolder(0), sb.length(), false ) ;
    }
    

    public static boolean eval( String expr, IntHolder frompos, int topos, boolean bracketoutstanding )
    {
        // evaluate the boolean expression given
        
        boolean forcereturn = false ;
        
        boolean result = false ;
        
        boolean not_pending = false ;
        
        char c = 0 ;
        
        int i = frompos.value ;
        
        boolean operand = false ;
        
        boolean op_pending = false ;
        
        char operation = 0 ;
        
        // read chars and try to match them
        
        while( i < topos )
        {
            c = expr.charAt(i) ;
            
            switch( c )
            {
                case 't' :
                case 'f' :
                    if( c == 't' )
                    {
                        operand = true ;
                    }
                    else
                    {
                        operand = false ;
                    }
                    
                    if( op_pending )
                    {
                        if( operation == '&' )
                        {
                            result = result & operand ;
                        }
                        else if( operation == '|' )
                        {
                            result = result | operand ;
                        }
                        else if( operation == '^' )
                        {
                            result = result ^ operand ;
                        }
                        else
                        {
                            report( "Illegal operation in expression." ) ;
                            return false ;
                        }
                    }
                    else
                    {
                        result = operand ;
                    }

                    break ;
                case '(' :
                    // evaluate sub-expression
                    
                    boolean subresult = false ;
                    
                    // The ONLY standard Java class that is a container
                    // for a mutable integer is the IntHolder class from
                    // org.omg.CORBA.  The standard Integer class is
                    // immutable which means we cannot use it to recieve
                    // a return value from the function we call.
                    
                    IntHolder ih = new IntHolder( i+1 ) ;
                    
                    subresult = not_pending ^ eval( expr, ih, topos, true ) ;
                    
                    i = ih.value ;
                    
                    if( op_pending )
                    {
                        if( operation == '&' )
                        {
                            result = result & subresult ;
                        }
                        else if( operation == '|' )
                        {
                            result = result | subresult ;
                        }
                        else if( operation == '^' )
                        {
                            result = result ^ subresult ;
                        }
                    }
                    else
                    {
                        result = subresult ;
                    }
                    
                    break ;
                case ')' :
                    // a sub-expression must be ending
                    
                    // inform the calling code of the current position
                    // we are parsing
                    
                    frompos.value = i ;
                    
                    if( bracketoutstanding )
                    {
                        // end of sub-expression
                        
                        result = not_pending ^ result ;
                    }
                    else
                    {
                        // this is an error in the expression as
                        // we have no opening bracket matching
                        
                        report( "Opening bracket does not match  opening bracket in expression." ) ;
                        
                        result = false ;
                    }
                    
                    forcereturn = true ;
                    
                    break ;
                case '!' :
                    // we have preparsed so that a not operation here
                    // can only happen if we are opening a bracketted
                    // expression
                    not_pending = not_pending ^ true ;
                    break ;
                case '&' :
                    if( op_pending )
                    {
                        report( "Two operations not separated by operands in expression." ) ;
                        return false ;
                    }
                    op_pending = true ;
                    operation = '&' ;
                    break ;
                case '|' :
                    if( op_pending )
                    {
                        report( "Two operations not separated by operands in expression." ) ;
                        return false ;
                    }
                    op_pending = true ;
                    operation = '|' ;
                    break ;
                case '^' :
                    if( op_pending )
                    {
                        report( "Two operations not separated by operands in expression." ) ;
                        return false ;
                    }
                    op_pending = true ;
                    operation = '^' ;
                    break ;
                default :
                    report( "Illegal character found during parsing of boolean expression." ) ;
                    forcereturn = true ;
                    result = false ;
                    break ;
            }
            
            if( forcereturn )
            {
                return result ;
            }
            
            i++ ;
        }
        
        return result ;
    }


    
    /**********************************************************
     */

    /* Literal defined - e.g. #define WIBBLE 1234
     * need to have some extra regex stuff in the pattern string
     * in order to avoid false matches.
     * This ensures a consistent treatment of that extra regex stuff
     * in the application.
     *
     * Note that this method also calls the method that removes
     * extraneous whitespace and comments.
     */

    static String makeToken( String s )
    {
        String rets = null ;
        
        rets = "(?<!" + "\\p{javaJavaIdentifierPart}" + ")" + cleanPattern(s) + "(?![" + "\\p{javaJavaIdentifierPart}" + "\\(])" ;
        
        return rets ;
    }
    
    static int __pretokenlen  = ( "(?<!" + "\\p{javaJavaIdentifierPart}" + ")" ).length() ;
    static int __posttokenlen = ( "(?![" + "\\p{javaJavaIdentifierPart}" + "\\(])" ).length() ;
    
    private static String detokenize( String s )
    {
        String rets = null ;
        
        int len = s.length() ;
        
        rets = s.substring( __pretokenlen, len - __posttokenlen ) ;
        
        return rets ;
    }
    
    private boolean tokenExists( String s )
    {
        String t = makeToken(s) ;
        
        // if( __debugme ) { report( "At line ", 935, " in ", currentfilename, " ",  "Checking token ", t  ) ; } ;
        
        int pos = -1 ;
        
        pos = patstrings.indexOf(t) ;
        
        if( pos < 0 )
        {
            /*
            for( String st : patstrings )
            {
                debug( "\t\t", st ) ;
            }
            */
        
            return false ;
        }
        
        return true ;
    }
    

















    static int outputlinecount = 0 ;





    public void processMain(PrintStream out)
    {
        int i = 0 ;
        int j = 0 ;
        int k = 0 ;
        
        int numpatterns = 0 ;
        
        int numlines = 0 ;
        
        String s = null ;
        String s2 = null ;
        String t = null ;
        
        String[] parts = null ;
        
        StringBuilder sb = null ;
        
        Pattern p = null ;
        
        boolean linechanged = false ;
        
        for( i = 0 ; i < lines.size() ; i++ )
        {
            numlines = lines.size() ;
        
            if( linechanged )
            {
                // force a reparse of the new version of
                // the line which is in the 's' variable
            
                i-- ;
            }
            else
            {
                s = lines.get(i) ;
            }
            
            // if( __debugme ) { report( "At line ", 1057, " in ", currentfilename, " ",  ">>> LINE :: ", i  ) ; } ;
            
            int minlenprematch = s.length() ;
            int minlenindex = -1 ;
            String minlenpre = null ;
            String minlenpost = null ;
        
            linechanged = false ;
        
            sb = new StringBuilder() ;
            
            numpatterns = patterns.size() ;
            
            for( j = 0 ; j < numpatterns ; j++ )
            {
                // if( __debugme ) { report( "At line ", 1072, " in ", currentfilename, " ",  ">>> Line ", i, " Pattern ", j  ) ; } ;
                
                p = patterns.get(j) ;
                
                if( p == null )
                {
                    // a pattern was deleted
                
                    continue ;
                }
                
                // split() matches the 's' against the pattern 'p'
                // the 2 value tells it to match just once and split
                // the text into two part ( before the match and after ).
                //
                // If there is no match the split() method returns just
                // one element in an array !  So checking the length of
                // the return array is a quick way to know there is or
                // is not a match !
                
                // Note that we rely on the code to tell the loop that
                // the line has changed so it reparses the line with
                // the replaced macro again for any more occurences.
                // This same mechanism also means that macros contained
                // inside macro expansions will also be expanded.
                
                parts = p.split( s, 2 ) ;
                
                if( parts.length == 1 )
                {
                    // no match - move to next pattern
                    
                    continue ;
                }
                
                if( parts[0].length() < minlenprematch )
                {
                    // this pattern matches before the current bext option
                    
                    minlenprematch = parts[0].length() ;
                    
                    minlenindex = j ;
                    
                    minlenpre   = parts[0] ;
                    minlenpost  = parts[1] ;
                }
            }
            // end of pattern matching loop
            
            if( minlenindex == -1 )
            {
                // there was no match to any pattern
                
                // output this line and continue
                
                if( ( ! linechanged ) && ( ( iflevel == 0 ) || ( ( iflevel > 0 ) && ifstate.peek().booleanValue() ) ) ) { String[] souta = s.split( "\n" ) ; for( String sout : souta ) { outputlinecount++ ; out.println( sout ) ; } } ;
                
                continue ;
            }
            
            // if we reached here there was a match
            
            sb.append( minlenpre ) ;
            
            s2 = minlenpost ;
            
            k = 1 ;
            
            j = minlenindex ;
            
            p = patterns.get(minlenindex) ;
        
            t = outputs.get(j) ;
            
            if( t.equals( "@comment-ml-start" ) )
            {
                // if( __debugme ) { report( "At line ", 1148, " in ", currentfilename, " ",  "@comment-ml-start"  ) ; }
                
                // read input and output it verbatim
                // until we get to a line matching the end of comment
                // pattern
                
                if( ( ! linechanged ) && ( ( iflevel == 0 ) || ( ( iflevel > 0 ) && ifstate.peek().booleanValue() ) ) ) { String[] souta = s.split( "\n" ) ; for( String sout : souta ) { outputlinecount++ ; System.out.println( sout ) ; } } ;
                
                i++ ;
                
                while( i < lines.size() )
                {
                    s = lines.get(i) ;
                    
                    if( ( ! linechanged ) && ( ( iflevel == 0 ) || ( ( iflevel > 0 ) && ifstate.peek().booleanValue() ) ) ) { String[] souta = s.split( "\n" ) ; for( String sout : souta ) { outputlinecount++ ; System.out.println( sout ) ; } } ;
                    
                    if( s.matches( "^.*\\*/\\s*$" ) )
                    {
                        break ;
                    }
                    
                    i++ ;
                }
            }
            else if( t.equals( "@linenumber" ) )
            {
                // if( __debugme ) { report( "At line ", 1174, " in ", currentfilename, " ",  "@linenumber"  ) ; } ;
            
                sb.append( i+1 ) ;
                
                linechanged = true ;
            }
            else if( t.equals( "@filename" ) )
            {
                // if( __debugme ) { report( "At line ", 1182, " in ", currentfilename, " ",  "@filename"  ) ; } ;
                
                sb.append( currentfilename ) ;
                
                linechanged = true ;
            }
            else if( t.equals( "@command" ) )
            {
                // if( __debugme ) { report( "At line ", 1190, " in ", currentfilename, " ",  "@command"  ) ; } ;
                
                // Process the lines until the next "#end-command"
                // by feeding them to an extern process.
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1195, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1195, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1195, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String cmd = null ;
                    
                    cmd = m.group(1) ;
                    
                    if( cmd != null )
                    {
                        processCommand( cmd, i ) ;
                        
                        // the number of lines may have changed completely
                        
                        numlines = lines.size() ;
                    
                        // we must assume that lines are changed doing this
                    
                        linechanged = true ;
                        
                        sb = new StringBuilder( lines.get(i) ) ;
                    }
                }
            }
            else if( t.equals( "@include" ) )
            {
                // if( __debugme ) { report( "At line ", 1219, " in ", currentfilename, " ",  "@include"  ) ; } ;
                
                // find what the file name to include was
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1223, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1223, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1223, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String fn = null ;
                    
                    fn = m.group(1) ;
                    
                    // if( __debugme ) { report( "At line ", 1229, " in ", currentfilename, " ",  "File name found was [", fn, "]"  ) ; } ;
                    
                    stack.push( lines ) ;

                    stack.push( currentfilename ) ;
                    
                    currentfilename = fn ;
                    
                    process( fn, out ) ;
                    
                    currentfilename = (String)( stack.pop() ) ;
                    
                    lines = (LinkedList<String>)( stack.pop() ) ;
                }
                
                sb.append( "" ) ;
            }
            else if( t.equals( "@define-val" ) )
            {
                // if( __debugme ) { report( "At line ", 1248, " in ", currentfilename, " ",  "@define-val"  ) ; } ;
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1250, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1250, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1250, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String pats = null ;
                    String outs = null ;
                    
                    pats = m.group(1) ;
                    
                    outs = m.group(2) ;
                    
                    if( __debugme ) { report( "At line ", 1259, " in ", currentfilename, " ",  "pats = [", pats, "]"  ) ; } ;
                    if( __debugme ) { report( "At line ", 1260, " in ", currentfilename, " ",  "outs = [", outs, "]"  ) ; } ;
                    
                    // pats has to have some extra regex stuff added to prevent
                    // it making false matches
                    

                // end of _ADD_OR_REPLACE_TOKEN
                    




                // end of _ADD_OR_REPLACE_PATTERN

                    
                     pats = makeToken(  pats ) ; int _pos = -1 ; _pos = patstrings.indexOf(  pats ) ; if( _pos == -1 ) { addPattern(  pats,  outs  ) ; numpatterns++ ; } else { String _outs2 = null ; _outs2 = cleanPattern(  outs  ) ; outputs.set( _pos, _outs2 ) ; } ;
                }
            }
            else if( t.equals( "@define-nul" ) )
            {
                // if( __debugme ) { report( "At line ", 1299, " in ", currentfilename, " ",  "@define-nul"  ) ; } ;
                
                // just define a token as an empty string
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1303, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1303, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1303, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String pats = null ;
                    
                    pats = m.group(1) ;
                    
                     pats = makeToken(  pats ) ; int _pos = -1 ; _pos = patstrings.indexOf(  pats ) ; if( _pos == -1 ) { addPattern(  pats,  ""  ) ; numpatterns++ ; } else { String _outs2 = null ; _outs2 = cleanPattern(  ""  ) ; outputs.set( _pos, _outs2 ) ; } ;
                }
            }
            else if( t.equals( "@undef" ) )
            {
                // if( __debugme ) { report( "At line ", 1314, " in ", currentfilename, " ",  "@undef"  ) ; } ;
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1316, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1316, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1316, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String pats = null ;
                    
                    pats = m.group(1) ;
                    
                    // pats has to have some extra regex stuff added to prevent
                    // it making false matches
                    
                    pats = makeToken( pats ) ;
                    
                    int pos = -1 ;
                    
                    pos = patstrings.indexOf( pats ) ;
                    
                    if( pos >= 0 )
                    {
                        // delete an existing pattern
                        
                        // { __debugme = true ; if( __debugme ) { report( "At line ", 1335, " in ", currentfilename, " ", "" ) ; } ; __debugme = false ; } ;
                        
                        patterns.set( pos, null ) ;
                        patstrings.set( pos, null ) ;
                        outputs.set( pos, null ) ;
                    }
                    else
                    {
                        // tried to undef an undefined pattern
                        
                        report( "Tried to undef [", pats, "] at line ", i+1, " in file ", currentfilename, " but was not defined." ) ;
                        /*
                        for( String s3 : patstrings )
                        {
                            debug( "\t[", s3, "]" ) ;
                        }
                        */
                    }
                }
            }
            else if( t.equals( "@endif" ) )
            {
                // if( __debugme ) { report( "At line ", 1357, " in ", currentfilename, " ",  "@endif"  ) ; } ;
                
                 ;
                
                iflevel-- ;
                
                Boolean state = ifstate.pop() ;
                
                if( iflevel < 0 )
                {
                    // an error
                    
                    report( "Found #endif without matching #ifdef or #if." ) ;
                }
            }
            else if( t.equals( "@else" ) )
            {
                // if( __debugme ) { report( "At line ", 1374, " in ", currentfilename, " ",  "@else"  ) ; } ;
                // if( __debugme ) { report( "At line ", 1375, " in ", currentfilename, " ",  s  ) ; } ;
                
                // toggle the ifstate on the top of stack
                
                 ;
                
                Boolean state = ifstate.pop() ;
                
                if( state.booleanValue() )
                {
                    // if( __debugme ) { report( "At line ", 1385, " in ", currentfilename, " ",  "#else changing from true to false"  ) ; } ;
                
                    ifstate.push( Boolean.FALSE ) ;
                
                     ;
                }
                else
                {
                    // if( __debugme ) { report( "At line ", 1393, " in ", currentfilename, " ",  "#else changing from false to true"  ) ; } ;
                
                    ifstate.push( Boolean.TRUE ) ;
                
                     ;
                }
            }
            else if( t.equals( "@ifdef" ) )
            {
                // if( __debugme ) { report( "At line ", 1402, " in ", currentfilename, " ",  "@ifdef"  ) ; } ;
                // if( __debugme ) { report( "At line ", 1403, " in ", currentfilename, " ",  s  ) ; } ;
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1405, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1405, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1405, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String pats = null ;
                    
                    pats = m.group(1) ;
                    
                    iflevel++ ;
                        
                    if( tokenExists( pats ) )
                    {
                        // defined so continue and ignore
                        // matching endif
                        
                        // { __debugme = true ; if( __debugme ) { report( "At line ", 1418, " in ", currentfilename, " ", "" ) ; } ; __debugme = false ; } ;
                        
                        ifstate.push( Boolean.TRUE ) ;
                
                         ;
                    }
                    else
                    {
                        // not defined so ignore output
                        // until matching endif found
                        
                        // { __debugme = true ; if( __debugme ) { report( "At line ", 1429, " in ", currentfilename, " ", "" ) ; } ; __debugme = false ; } ;
                        
                        ifstate.push( Boolean.FALSE ) ;
                
                         ;
                    }
                }
            }
            else if( t.equals( "@if" ) )
            {
                // if( __debugme ) { report( "At line ", 1439, " in ", currentfilename, " ",  "@if"  ) ; } ;
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1441, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1441, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1441, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String pats = null ;
                    
                    pats = m.group(1) ;
                    
                    // if( __debugme ) { report( "At line ", 1447, " in ", currentfilename, " ",  "#if on [", pats, "]"  ) ; } ;
                    
                    // pats is a boolean expression and we need to
                    // evaluate it.
                    // that's too complex for an inline action
                    
                    iflevel++ ;
                    
                    if( evaluate( pats ) )
                    {
                        // defined so continue and output
                        // until matching endif
                        
                        // if( __debugme ) { report( "At line ", 1460, " in ", currentfilename, " ",  "#if ",pats, " was true"  ) ; } ;
                        
                        ifstate.push( Boolean.TRUE ) ;
                        
                         ;
                    }
                    else
                    {
                        // not defined so ignore output
                        // until matching endif found
                        
                        // if( __debugme ) { report( "At line ", 1471, " in ", currentfilename, " ",  "#if ",pats, " was false"  ) ; } ;
                        
                        ifstate.push( Boolean.FALSE ) ;
                        
                         ;
                    }
                }
            }
            else if( t.equals( "@define-fn" ) )
            {
                // if( __debugme ) { report( "At line ", 1481, " in ", currentfilename, " ",  "@define-fn"  ) ; } ;
                
                // single line macro function
                
                // first we parse the definition so we can create a new
                // pattern in the list
                






                // end of String fnname = null ; String params = null ; String[] paramarray = null ; fnname = m.group(1) ; params = m.group(2) ; paramarray = params.split( "\\s*,\\s*" ) ; StringBuilder patsb = new StringBuilder() ; patsb.append( "(?<!"+"\\p{javaJavaIdentifierPart}"+")" ) ; patsb.append( fnname ) ; patsb.append( "\\(" ) ; StringBuilder fnbodyb = new StringBuilder() ; boolean vaargs_used = false ; fnbodyb.append( "@defined-fn-use[" ) ; if( ! ( ( paramarray.length == 1 ) && ( paramarray[0].length() == 0 ) ) ) { int i2 = 0 ; while( i2 < paramarray.length ) { patsb.append( "\\s*" ) ; if( i2 > 0 ) { patsb.append( ',' ) ; fnbodyb.append( ',' ) ; patsb.append( "\\s*" ) ; } fnbodyb.append( paramarray[i2] ) ; if( paramarray[i2].equals( "..." ) ) { report( "NOTE : Variadic argument detected." ) ; if( vaargs_used ) { report( "Error : Variadic argument declared more than once in macro." ) ; } vaargs_used = true ; patsb.append( "([^\\)]+)" ) ; } else { patsb.append( "([^,\\(\\)]+(?:\\(+[^,]*,+[^,]*\\)(?:[^,\\(\\)])*)*)" ) ; } i2++ ; }; } if( vaargs_used ) { if( ! paramarray[ paramarray.length-1 ].equals( "..." ) ) { report( "Error ; variadic argument must be LAST parameter." ) ; } } fnbodyb.append( ']' ) ; patsb.append( "\\s*\\)" ) ; String pats = patsb.toString() ; report( "NOTE :: patsb = ", pats ) ;
                    


                    
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1577, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1577, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1577, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String fnname = null ; String params = null ; String[] paramarray = null ; fnname = m.group(1) ; params = m.group(2) ; paramarray = params.split( "\\s*,\\s*" ) ; StringBuilder patsb = new StringBuilder() ; patsb.append( "(?<!"+"\\p{javaJavaIdentifierPart}"+")" ) ; patsb.append( fnname ) ; patsb.append( "\\(" ) ; StringBuilder fnbodyb = new StringBuilder() ; boolean vaargs_used = false ; fnbodyb.append( "@defined-fn-use[" ) ; if( ! ( ( paramarray.length == 1 ) && ( paramarray[0].length() == 0 ) ) ) { int i2 = 0 ; while( i2 < paramarray.length ) { patsb.append( "\\s*" ) ; if( i2 > 0 ) { patsb.append( ',' ) ; fnbodyb.append( ',' ) ; patsb.append( "\\s*" ) ; } fnbodyb.append( paramarray[i2] ) ; if( paramarray[i2].equals( "..." ) ) { report( "NOTE : Variadic argument detected." ) ; if( vaargs_used ) { report( "Error : Variadic argument declared more than once in macro." ) ; } vaargs_used = true ; patsb.append( "([^\\)]+)" ) ; } else { patsb.append( "([^,\\(\\)]+(?:\\(+[^,]*,+[^,]*\\)(?:[^,\\(\\)])*)*)" ) ; } i2++ ; }; } if( vaargs_used ) { if( ! paramarray[ paramarray.length-1 ].equals( "..." ) ) { report( "Error ; variadic argument must be LAST parameter." ) ; } } fnbodyb.append( ']' ) ; patsb.append( "\\s*\\)" ) ; String pats = patsb.toString() ; report( "NOTE :: patsb = ", pats ) ;
                    
                    fnbodyb.append( m.group(3) ) ;
                    
                    int _pos = -1 ; _pos = patstrings.indexOf( pats ) ; if( _pos == -1 ) { addPattern( pats, fnbodyb.toString() ) ; numpatterns++ ; } else { String _outs2 = null ; _outs2 = cleanPattern( fnbodyb.toString() ) ; outputs.set( _pos, _outs2 ) ; }
                }
            }
            else if( t.equals( "@define-fn-ml" ) )
            {
                // if( __debugme ) { report( "At line ", 1588, " in ", currentfilename, " ",  "@define-fn-ml"  ) ; } ;
                
                // we parse lines until we get to "^\\s*#\\s*end-def"+"\\s*$"
                // at which point we're finished with this macro "function"
                
                // first we parse the definition so we can create a new
                // pattern in the list
                
                // $1 = fn name
                // $2 = parameter list
                // function body is everything after this until the #end-def directive
                // first we parse the definition so we can create a new
                // pattern in the list
                
                __debugme = true ;
                
                Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1604, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1604, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1604, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                {
                    String fnname = null ; String params = null ; String[] paramarray = null ; fnname = m.group(1) ; params = m.group(2) ; paramarray = params.split( "\\s*,\\s*" ) ; StringBuilder patsb = new StringBuilder() ; patsb.append( "(?<!"+"\\p{javaJavaIdentifierPart}"+")" ) ; patsb.append( fnname ) ; patsb.append( "\\(" ) ; StringBuilder fnbodyb = new StringBuilder() ; boolean vaargs_used = false ; fnbodyb.append( "@defined-fn-use[" ) ; if( ! ( ( paramarray.length == 1 ) && ( paramarray[0].length() == 0 ) ) ) { int i2 = 0 ; while( i2 < paramarray.length ) { patsb.append( "\\s*" ) ; if( i2 > 0 ) { patsb.append( ',' ) ; fnbodyb.append( ',' ) ; patsb.append( "\\s*" ) ; } fnbodyb.append( paramarray[i2] ) ; if( paramarray[i2].equals( "..." ) ) { report( "NOTE : Variadic argument detected." ) ; if( vaargs_used ) { report( "Error : Variadic argument declared more than once in macro." ) ; } vaargs_used = true ; patsb.append( "([^\\)]+)" ) ; } else { patsb.append( "([^,\\(\\)]+(?:\\(+[^,]*,+[^,]*\\)(?:[^,\\(\\)])*)*)" ) ; } i2++ ; }; } if( vaargs_used ) { if( ! paramarray[ paramarray.length-1 ].equals( "..." ) ) { report( "Error ; variadic argument must be LAST parameter." ) ; } } fnbodyb.append( ']' ) ; patsb.append( "\\s*\\)" ) ; String pats = patsb.toString() ; report( "NOTE :: patsb = ", pats ) ;
                    
                    // now we have to build the function body
                    
                    // add every line verbatim until we hit the
                    // magic #end-def sequence :
                    // end-def for multi-line macro function definitions
                    // regex = "^\\s*#\\s*end-def"+"\\s*$"
                    
                    report( "DEBUG :: i            = " + i ) ;
                    report( "DEBUG :: numlines     = " + numlines ) ;
                    report( "DEBUG :: lines.size() = " + lines.size() ) ;
                    
                    int istart = i ;
                    
                    String ln = null ;
                    
                    // note that i is the loop counter used in the main loop !
                    
                    while( i < numlines-1 )
                    {
                        i++ ;
                        
                        ln = lines.get(i) ;
                        
                        if( ln.matches( "^\\s*#\\s*end-def"+"\\s*$" ) )
                        {
                            break ;
                        }
                        
                        fnbodyb.append( ln ) ;
                        fnbodyb.append( '\n' ) ;
                    };
                    
                    if( i >= numlines )
                    {
                        // ran out of input
                        
                        report( "Ran out of lines while processing multi-line define at ", istart, " in ", currentfilename ) ;
                        report( "\t - probable cause : missing #end-def" ) ;
                        
                        // break out of pattern loop and up to lines loop
                        // which should end the processing.
                        
                        break ;
                    }
                    
                    int _pos = -1 ; _pos = patstrings.indexOf( pats ) ; if( _pos == -1 ) { addPattern( pats, fnbodyb.toString() ) ; numpatterns++ ; } else { String _outs2 = null ; _outs2 = cleanPattern( fnbodyb.toString() ) ; outputs.set( _pos, _outs2 ) ; }
                }
            }
            else if( t.startsWith( "@defined-fn-use" ) )
            {
                __debugme = true ;
                
                if( __debugme ) { report( "At line ", 1660, " in ", currentfilename, " ",  "@defined-in-use"  ) ; } ;
                
                if( __debugme ) { report( "At line ", 1662, " in ", currentfilename, " ",  p  ) ; } ;
                if( __debugme ) { report( "At line ", 1663, " in ", currentfilename, " ",  t  ) ; } ;
                
                // here we handle the special case of
                // function-like macros being used in the code.
                
                // The 't' value contains the parameter names followed
                // by the code to replace it with.
                
                // grab the parameter names
                
                int i2 = t.indexOf( "]" ) ;
                
                LinkedList<String> paramnames = new LinkedList<String>() ;
                
                int i3 = 16 ;
                
                int i4 = i3 ;
                
                StringBuilder sb2 = new StringBuilder() ;
                
                while( i3 < i2 )
                {
                    if( t.charAt(i3) == ',' )
                    {
                        // end of a parameter found
                        
                        paramnames.addLast( t.substring(i4,i3) ) ;
                        
                        sb2 = new StringBuilder() ;
                        
                        i4 = i3 + 1 ;
                    }
                    
                    i3++ ;
                };
                
                // add the last param
                
                paramnames.addLast( t.substring(i4,i3) ) ;
                
                // move the function body into the string builder we
                // will be making substitions into
                
                sb2 = new StringBuilder() ;
                
                sb2.append( t.substring( i2+1 ) ) ;
                
                // if( __debugme ) { report( "At line ", 1710, " in ", currentfilename, " ",  "sb2 = [", sb2, "]"  ) ; } ;
                
                // build our array of substitutes from variable s2
                // which was what we matched with this pattern (p)
                
                // catch case when there are NO paramters
                
                if( ! ( ( paramnames.size() <= 1 ) && ( paramnames.get(0).length() == 0 ) ) )
                {
                    LinkedList<String> paramsubs = new LinkedList<String>() ;
                    
                    /*
                    debug( "p  = [", p, "]" ) ;
                    debug( "s  = [", s, "]" ) ;
                    
                    for( i2 = 0 ; i2 <= k ; i2++ )
                    {
                        debug( "part[", i2, "] = [", parts[i2], "]" ) ;
                    }
                    */
                    
                    int pnameslen = paramnames.size() ;
                        
                    Matcher m = p.matcher( s ) ; if( ! m.find() ) { if( __debugme ) { report( "At line ", 1733, " in ", currentfilename, " ", ">>> WEIRD : matcher failed :: " ) ; } ; if( __debugme ) { report( "At line ", 1733, " in ", currentfilename, " ", "\t", m ) ; } ; if( __debugme ) { report( "At line ", 1733, " in ", currentfilename, " ", "\t[", s, "]" ) ; } ; break ; } else
                    {
                        for( i2 = 0 ; i2 < pnameslen ; i2++ )
                        {
                            paramsubs.addLast( cleanPattern( m.group(i2+1) ) ) ;
                            
                            if( __debugme ) { report( "At line ", 1739, " in ", currentfilename, " ",  "paramname[",i2,"] = [",paramnames.get(i2),"]"  ) ; } ;
                            if( __debugme ) { report( "At line ", 1740, " in ", currentfilename, " ",  "paramsubs[",i2,"] = [",paramsubs.get(i2),"]"  ) ; } ;
                        }
                    }
                    
                    // Need to check if we have a variadic parameter ( must be LAST ! )
                    
                    if( ( pnameslen > 0 ) && ( paramnames.get(pnameslen-1).equals("...") ) )
                    {
                        if( __debugme ) { report( "At line ", 1748, " in ", currentfilename, " ",  "Trying to add variadic parameters."  ) ; } ;
                        
                        // last param is variadic
                        
                        // split the parameter into more parameters at commas
                        // and add these variadic symbols to the parameter
                        // substitutions list
                        
                        // care must be taken not to steal commas inside quotes
                        // so this is a little trickier that it might seem.
                        
                        // our starting point is paramasubs.get(pnameslen-1) value
                        // that contains all the variadic parameters
                        
                        boolean inquotes = false ;
                        char quotetomatch = 0 ;
                        
                        String vargs = paramsubs.get(pnameslen-1) ;
                        
                        // we'll be completely replacing the last paramnames entry
                        
                        paramnames.remove( pnameslen-1 ) ;
                        paramsubs.remove( pnameslen-1 ) ;
                        
                        int vargslen = vargs.length() ;
                        
                        char c = 0 ;
                        
                        i2 = 0 ;
                        
                        i3 = 0 ;
                        
                        int vargnum = 0 ;
                        
                        // everything between the current i3 and i2 values inclusive
                        // will be a single parameter.
                        // So we search char by char using i2 until we find an unquoted
                        // comma.
                        
                        while( i2 < vargslen )
                        {
                            c = vargs.charAt(i2) ;
                            
                            if( ( c == '"' ) || ( c == '\'' ) )
                            {
                                // starting quotes
                                
                                inquotes = true ;
                                quotetomatch = c ;
                                
                                i2++ ;
                                
                                while( i2 < vargslen )
                                {
                                    c = vargs.charAt(i2) ;
                                    
                                    if( c == quotetomatch )
                                    {
                                        // check it's not escaped
                                        // note i2 must be > 0 to be in this loop
                                        
                                        if( vargs.charAt(i2-1) != '\\' )
                                        {
                                            inquotes = false ;
                                            
                                            break ;
                                        }
                                    }
                                };
                                
                                if( i2 >= vargslen )
                                {
                                    break ;
                                }
                            }
                            else if( c == ',' )
                            {
                                // end of parameter
                                
                                paramnames.addLast( "__VA_ARGS__$" + vargnum ) ;
                                
                                vargnum++ ;
                                
                                paramsubs.addLast( vargs.substring(i3,i2) ) ;
                                
                                i2++ ;
                                
                                i3 = i2 ;
                                
                                continue ;
                            }
                            
                            i2++ ;
                        };
                        
                        // add last parameter
                        
                        paramnames.addLast( "__VA_ARGS__$" + vargnum ) ;
                        paramsubs.addLast( vargs.substring(i3,i2) ) ;
                        
                        // finally add the complete vargs value
                        
                        paramnames.addLast( "__VA_ARGS__" ) ;
                        paramsubs.addLast( vargs ) ;
                    }
                    
                    {
                        int dbgi = 0 ;
                        
                        while( dbgi < paramnames.size() )
                        {
                            report( "DEBUG :: [" + paramnames.get(dbgi) + "] = [" + paramsubs.get(dbgi) + "]"  ) ;
                        
                            dbgi++ ;
                        };
                    }
                    
                    // make the substitutions
                    
                    String pname = null ;
                    String psub = null ;
                    
                    pnameslen = paramnames.size() ;
                    
                    for( i2 = 0 ; i2 < pnameslen ; i2++ )
                    {
                        pname = paramnames.get(i2) ;
                        
                        // scan for each parameter name and replace it
                        
                        // we want to match the name exactly
                        // which means the preceeding and following
                        // chars ( if any ) must not be a char that
                        // can be part of a literal
                        
                        // we cannot use regex's with StringBuilders
                        // so that means we're on a dumbscan
                        
                        i3 = 0 ;
                        
                        int plen = pname.length() ;
                        
                        char c2 = 0 ;
                        
                        int pos = 0 ;
                        
                        while( i3 < ( sb2.length() - plen ) )
                        {
                            i3 = sb2.indexOf( pname, i3 ) ;
                            
                            if( i3 < 0 )
                            {
                                // no match
                                
                                break ;
                            }
                            
                            if( i3 > 0 )
                            {
                                c2 = sb2.charAt(i3-1) ;
                                
                                if( Character.isJavaIdentifierPart(c2) )
                                {
                                    // a false match
                                    
                                    i3++ ;
                                    
                                    continue ;
                                }
                            }
                            
                            if( i3+plen < sb2.length() )
                            {
                                c2 = sb2.charAt(i3+plen) ;
                                
                                if( Character.isJavaIdentifierPart(c2) )
                                {
                                    // a false match
                                    
                                    i3++ ;
                                    
                                    continue ;
                                }
                            }
                            
                            // It was a match ( as far as we can tell )
                            // so we substitutue it
                            
                            report( "DEBUG :: replacing " + paramnames.get(i2) + " [" + sb2.substring(i3,i3+plen) + "] with [" + paramsubs.get(i2) + "]" ) ;
                            
                            sb2.replace( i3, i3+plen, paramsubs.get(i2) ) ;
                            
                            // move i3 to point at the end of the string we just substituted
                            
                            i3 += plen ;
                        };
                    }
                }
                
                // finally we output the result
                
                linechanged = true ;
                
                sb.append( sb2 ) ;
                
                if( __debugme ) { report( "At line ", 1953, " in ", currentfilename, " ",  "leaving @defined-in-use"  ) ; } ;
            }
            else
            {
                // { __debugme = true ; if( __debugme ) { report( "At line ", 1957, " in ", currentfilename, " ", "" ) ; } ; __debugme = false ; } ;
                
                // something defined was replaced
                
                linechanged = true ;
            
                sb.append( t ) ;
            }
            
            sb.append( s2 ) ;
            
            // if the new line was constructed by a defined function
            // oe external command it may have multiple newlines in it
            // and in that case we need to split it up and insert these
            // lines into our code.
            //
            // Note : we need to split them up because the parser won't
            // work correctly if we don't.
            
            if( ! linechanged )
            {
                // line 's' needs to be replaced by processed result of this pattern
                
                s = sb.toString() ;
            }
            else
            {
                // check if we need to insert lines
                
                StringBuilder sb2 = new StringBuilder() ;
                
                int len = sb.length() ;
            
                int i32 = 0 ;
                
                char c = 0 ;
                
                LinkedList<String> newlines = new LinkedList<String>() ;
                
                while( i32 < len )
                {
                    c = sb.charAt(i32) ;
                    
                    if( c == '\n' )
                    {
                        newlines.add( sb2.toString() ) ;
                    
                        sb2 = new StringBuilder() ;
                    }
                    else
                    {
                        sb2.append( c ) ;
                    }
                    
                    i32++ ;
                };
                
                if( c != '\n' )
                {
                    newlines.add( sb2.toString() ) ;
                }
                
                s = newlines.get(0) ;
                
                i32 = newlines.size() - 1 ;
                
                while( i32 > 0 )
                {
                    lines.add( i+1, newlines.get(i32) ) ;
                
                    i32-- ;
                }
                
                // some debug stuff
                
                /*
                 * very long line during debugging an error
                 * output this in chunks and break
                    
                if( s.length() > 1000 )
                {
                    debug( ">>> >>> Breaking out due to very wide line <<< <<<" ) ;
                    
                    int slen = s.length() ;
                    
                    int i4 = 0 ;
                    int i5 = 64 ;
                    
                    for( i4 = 0 ; i < slen ; i4 += 64 )
                    {
                        i5 = i4 + 64 ;
                        if( i5 > slen )
                        {
                            i5 = slen ;
                        }
                        
                        debug( "[" + s.substring( i4, i5 ) + "]" ) ;
                    }
                    
                    break ;
                }
                */
            
                // if( __debugme ) { report( "At line ", 2060, " in ", currentfilename, " ",  "CHANGED :: ", s  ) ; } ;
            }
                
            sb = new StringBuilder() ;
            
            // Output the result
            
            if( ( ! linechanged ) && ( ( iflevel == 0 ) || ( ( iflevel > 0 ) && ifstate.peek().booleanValue() ) ) ) { String[] souta = s.split( "\n" ) ; for( String sout : souta ) { outputlinecount++ ; System.out.println( sout ) ; } } ;
            
        } /* end of loop through linked list of input lines. */
    }
     
    /**********************************************************
     */
     
    public void process( String filename, PrintStream out )
    {
        FileReader fr = null ;
        
        BufferedReader br = null ;

        Scanner sc = null ;
        
        lines = null ;
        
        lines = new LinkedList<String>() ;
        
        if( lines == null )
        {
            return ;
        }
        
        try
        {
            fr = new FileReader( filename ) ;
        }
        catch( FileNotFoundException fnfe )
        {
            fr = null ;
        }
        
        currentfilename = filename ;
        
        if( fr == null )
        {
            report( "File [", filename, "] was not found." ) ;
        
            return ;
        }
        
        br = null ;
        
        br = new BufferedReader( fr ) ;
        
        if( br == null )
        {
            { __debugme = true ; if( __debugme ) { report( "At line ", 2116, " in ", currentfilename, " ", "" ) ; } ; __debugme = false ; } ;
            
            try {  fr.close()  ; } catch( IOException ioe ) { } ;
            
            return ;
        }
        
        // get a Scanner
        
        sc = null ;
        
        sc = new Scanner( br ) ;
        
        // Read all the lines from the file into a
        // linked list of StringBuilders
        
        StringBuilder sb = null ;
        
        String line = null ;
        
        while( sc.hasNextLine() )
        {
            line = sc.nextLine() ;
            
            if( line != null )
            {
                lines.add( line ) ;
            }
        }
        
        // initialize the pattern matcher
        
        if( patterns == null )
        {
            patterns = new LinkedList<Pattern>() ;
            
            patstrings = new LinkedList<String>() ;
            
            ifstate = new Stack<Boolean>() ;
            
            if( patterns != null )
            {
                outputs = new LinkedList<String>() ;
                
                if( outputs != null )
                {
                    // multi-line C style comment start
                    // we are only interested in comments that start at the begining
                    // of a line and end at end of a line ( maybe a different line ).
                    //
                    // This does NOT support nested comments
                    addPattern( "^\\s*/\\*.*$", "@comment-ml-start" ) ;
                
                    // special macro returns line number in file
                    addPattern( "__LINE__", "@linenumber" ) ;
                    
                    // special macro returns current file name
                    // note : outputs unquoted sequence fo chars
                    addPattern( "__FILE__", "@filename" ) ;
                    
                    // straight include file directive
                    // $1 = name of file to include
                    // unlike C/C++ no quotes or <> bracketing is used
                    // as no default search paths exists for includes.
                    addPattern( "^\\s*#\\s*include\\s+(\\S+)"+"\\s*$", "@include" ) ;
                    
                    // plain define with no output
                    // the sole purpose of these is to check for existence
                    // $1 = pattern to define
                    addPattern( "^\\s*#\\s*define\\s+"+"("+"\\p{javaJavaIdentifierStart}"+"\\p{javaJavaIdentifierPart}"+"*)"+"\\s*$", "@define-nul" ) ;
                    
                    // define a symbol as another set of characters
                    // $1 = pattern to define
                    // $2 = output sequence
                    addPattern( "^\\s*#\\s*define\\s+"+"("+"\\p{javaJavaIdentifierStart}"+"\\p{javaJavaIdentifierPart}"+"*)"+"\\s+(.+)"+"\\s*$", "@define-val" ) ;
                    
                    // undefine stated pattern
                    // $1 = pattern to undefine
                    addPattern( "^\\s*#\\s*undef\\s+"+"("+"\\p{javaJavaIdentifierStart}"+"\\p{javaJavaIdentifierPart}"+"*)"+""+"\\s*$", "@undef" ) ;
                    
                    // if <expression> is true macro
                    // $1 = boolean expression
                    addPattern( "^\\s*#\\s*if\\s+(.+)"+"\\s*$", "@if" ) ;
                    
                    // simple #ifdef directive on stated pattern
                    // $1 = pattern to check for existance of
                    addPattern( "^\\s*#\\s*ifdef\\s+(\\S+)"+"\\s*$", "@ifdef" ) ;
                    
                    // an else clause for the #if and #ifdef operations
                    addPattern( "^\\s*#\\s*else"+"\\s*$", "@else" ) ;
                    
                    // marks the end of the current #if or #ifdef
                    addPattern( "^\\s*#\\s*endif"+"\\s*$", "@endif" ) ;
                    
                    // external command
                    // $1 = command
                    // input for command is everything after this until #end-command directive
                    addPattern( "^\\s*#\\s*command\\s+(.+)"+"\\s*$", "@command" ) ;
                    
                    // When using variadic args in function-like macros :
                    //
                    // Uses form "define <fnname>(...) <function>
                    //
                    // Arguments may be referred to as __VA_ARGS__ as a lump
                    // or individually by __VA_ARGS__$<numeric index>

                    // single line macro-function define
                    // $1 = fn name
                    // $2 = parameter list
                    // $3 = function "body"
                    addPattern( "^\\s*#\\s*define\\s+"+"("+"\\p{javaJavaIdentifierStart}"+"\\p{javaJavaIdentifierPart}"+"*)"+"\\(\\s*(.*)\\s*\\)\\s+(.+)"+"\\s*$", "@define-fn" ) ;
                    
                    // multi-line macro function define
                    // $1 = fn name
                    // $2 = parameter list
                    // function body is everything after this until the #end-def directive
                    addPattern( "^\\s*#\\s*define\\s+" +"("+"\\p{javaJavaIdentifierStart}"+"\\p{javaJavaIdentifierPart}"+"*)"+"\\(\\s*(.*)\\s*\\)"+"\\s*$", "@define-fn-ml" ) ;
                    
                }
            }
        }
        
        if( patterns != null )
        {
            processMain( out ) ;
        }
        
        // finish up
        
        // note that Scanner's close method should also close the reader
        
        sc.close() ;
        
        if( iflevel > 0 )
        {
            report( iflevel, " unmatched #ifdef(s) after end of file.\n" ) ;
        }
    }
    
    /**********************************************************
     */
    
    static boolean checkDebug()
    {
    	boolean debugme = false;
        String ev = null ;
        
        ev = System.getenv("DEBUG_JAVAPP") ;
        
        if( ev != null )
        {
            debugme = true ;
        }
        
        return debugme;
    }
    
    public static void main( String[] args )
    {
        boolean debug = checkDebug() ;
    
        String defname = null ;

        JavaPP processor = new JavaPP();
        
        int i = 0 ;
        
        for( i = 0 ; i < args.length ; i++ )
        {
            if( args[i].equals( "-D" ) )
            {
                // add a definition to the patterns list
                
                i++ ;
                    
                if( i < args.length )
                {
                    defname = args[i] ;
                }
                else
                {
                    report( "No argument after -D option - one argument required." ) ;
                    
                    return ;
                }
                
                processor.define( defname );
                
                continue ;
            }
        
            // open each file argument in turn
            
            processor.process( args[i], System.out ) ;
        }
    }
}
