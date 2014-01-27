#!/bin/sh

GLASSFISH_ASADMIN=asadmin

echo ---------------------------------------------------
echo Local database and Realm
echo ---------------------------------------------------
echo
echo Register the JDBC connection pool
$GLASSFISH_ASADMIN create-jdbc-connection-pool --datasourceclassname="com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource" --restype="javax.sql.ConnectionPoolDataSource" --property="url=jdbc\:mysql\://localhost\:3306/eirc:user=eirc:password=eirc:characterResultSets=utf8:characterEncoding=utf8:useUnicode=true:connectionCollation=utf8_unicode_ci:autoReconnect=true" eircPool

echo
echo Create a JDBC resource with the specified JNDI name
$GLASSFISH_ASADMIN create-jdbc-resource --connectionpoolid eircPool jdbc/eircResource

echo
echo Add the named authentication realm
$GLASSFISH_ASADMIN create-auth-realm --classname="com.sun.enterprise.security.ee.auth.realm.jdbc.JDBCRealm" --property="jaas-context=jdbcRealm:datasource-jndi=jdbc/eircResource:user-table=user:user-name-column=login:password-column=password:group-table=usergroup:group-name-column=group_name:charset=UTF-8:digest-algorithm=MD5" eircRealm
