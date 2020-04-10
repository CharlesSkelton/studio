package studio.kdb;

public class ReloadQKeywords {
    public ReloadQKeywords(final Server server) {
        if (server != null) {
            Runnable runner = new Runnable() {
                public void run() {
                    kx.c c = null;
                    Object r = null;

                    try {
                        c = ConnectionPool.getInstance().leaseConnection(server);
                        c.k(new K.KCharacterVector("key`.q"));
                        r = c.getResponse();
                    }
                    catch (Throwable t) {
                        System.err.println("Error in getting connection to " + server.getConnectionString(true) + ": " + t);
                        t.printStackTrace(System.err);
                        ConnectionPool.getInstance().purge(server);
                        c = null;
                    }
                    finally {
                        if (c != null)
                            ConnectionPool.getInstance().freeConnection(server,c);
                    }
                    if (r instanceof K.KSymbolVector)
                        Config.getInstance().saveQKeywords((String[]) ((K.KSymbolVector) r).getArray());
                }
                ;
            };
            Thread t = new Thread(runner);
            t.setName("QKeywordReloader");
            t.setDaemon(true);
            t.start();
        }
    }
}
