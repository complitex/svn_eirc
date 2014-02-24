@ECHO off

SET GLASSFISH_ASADMIN=C:\glassfish4\bin\asadmin.bat

ECHO ---------------------------------------------------
ECHO Local database and Realm
ECHO ---------------------------------------------------
ECHO.
ECHO Register the JDBC connection pool
call %GLASSFISH_ASADMIN% create-jdbc-connection-pool --datasourceclassname com.mysql.jdbc.jdbc2.optional.MysqlXADataSource --restype javax.sql.XADataSource --property url=jdbc\:mysql\://localhost\:3306/transformer:user=transformer:password=transformer:characterResultSets=utf8:characterEncoding=utf8:useUnicode=true:connectionCollation=utf8_unicode_ci:autoReconnect=true transformerPool

ECHO.
ECHO Create a JDBC resource with the specified JNDI name
call %GLASSFISH_ASADMIN% create-jdbc-resource --connectionpoolid transformerPool jdbc/transformerResource

ECHO.
ECHO Add the named authentication realm
call %GLASSFISH_ASADMIN% create-auth-realm --classname com.sun.enterprise.security.ee.auth.realm.jdbc.JDBCRealm --property jaas-context=jdbcRealm:datasource-jndi=jdbc/transformerResource:user-table=user:user-name-column=login:password-column=password:group-table=usergroup:group-name-column=group_name:charset=UTF-8:digest-algorithm=MD5 transformerRealm

drop database if exists transformer;
create database transformer DEFAULT CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
use transformer;
grant all privileges on transformer.* to transformer identified by 'transformer';
flush privileges;