package ru.flexpay.eirc.mb_transformer.util;

/**
 * @author Pavel Sknar
 */
public class Properties {
    final static String LOG_DIRECTORY = "logs";

    final static int SERVER_PORT = 8888;
    final static int DB_PORT = 13306;

    final static String DB_USER = "eirc";
    final static String DB_PASSWORD = "eirc";

    final static String DB_BASEDIR = "bin\\mysql";
    final static String DB_DATADIR = "data";

    final static String CLIENT_URL = "http://localhost:" + SERVER_PORT + "/transformer";

    final static String INIT_DB_URL = "jdbc:mysql:mxj://localhost:" + DB_PORT + "/eirc?" +
            "server.basedir=" + DB_BASEDIR +
            "&server.datadir=" + DB_DATADIR +
            "&createDatabaseIfNotExist=true" +
            "&server.initialize-user=true" +
            "&server.default-character-set=utf8" ;

    final static String DB_URL = "jdbc:mysql://localhost:" + DB_PORT + "/eirc";

    final static String JDBC_POOL_URL = "jdbc\\:mysql\\:mxj\\://localhost\\:" + DB_PORT + "/eirc";


    final static String SERVER_UPDATE_URL = "http://localhost:8080/server/download/";

}
