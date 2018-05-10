package org.neo4j.tools.pit;

public class PointInTimeRecover
{
    public static void main( String... argv )
    {
        System.exit( mainReturn( argv ) );
    }

    public static int mainReturn( String... argv )
    {
        if ( argv.length == 0 )
        {
            System.out.println( "usage: java -jar pit.jar <command> [<args>]\n" +
                    "\n" +
                    "overview\n" +
                    "  pit takes two store directories - a 'backup' and a 'head', and recovers the backup to\n" +
                    "  any transaction between the backup and head. It only works if head has been configured\n" +
                    "  to keep transaction history up to and including the last transaction in the backup.\n" +
                    "\n" +
                    "commands\n" +
                    "  transactions <backup> <head>      Print available recovery points, including date and txid\n" +
                    "  recover <backup> <head> <txid>    Recover <backup> up to <txid>\n" +
                    "\n" );
        }

        if ( argv[0].equals( "transactions" ) )
        {
            return new TransactionsCommand().execute( argv );
        }

        if ( argv[0].equals( "recover" ) )
        {
            return new RecoverCommand().execute( argv );
        }

        return 3;
    }
}
