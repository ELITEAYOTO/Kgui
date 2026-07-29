/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp.pool;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyCallableStatement;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyConnection;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyDatabaseMetaData;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyPreparedStatement;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyResultSet;
import pk.ajneb97.libs.hikaricp.pool.HikariProxyStatement;
import pk.ajneb97.libs.hikaricp.pool.PoolEntry;
import pk.ajneb97.libs.hikaricp.pool.ProxyConnection;
import pk.ajneb97.libs.hikaricp.pool.ProxyLeakTask;
import pk.ajneb97.libs.hikaricp.pool.ProxyStatement;
import pk.ajneb97.libs.hikaricp.util.FastList;

public final class ProxyFactory {
    private ProxyFactory() {
    }

    static ProxyConnection getProxyConnection(PoolEntry poolEntry, Connection connection, FastList<Statement> fastList, ProxyLeakTask proxyLeakTask, long l, boolean bl, boolean bl2) {
        return new HikariProxyConnection(poolEntry, connection, (FastList)fastList, proxyLeakTask, l, bl, bl2);
    }

    static Statement getProxyStatement(ProxyConnection proxyConnection, Statement statement) {
        return new HikariProxyStatement(proxyConnection, statement);
    }

    static CallableStatement getProxyCallableStatement(ProxyConnection proxyConnection, CallableStatement callableStatement) {
        return new HikariProxyCallableStatement(proxyConnection, callableStatement);
    }

    static PreparedStatement getProxyPreparedStatement(ProxyConnection proxyConnection, PreparedStatement preparedStatement) {
        return new HikariProxyPreparedStatement(proxyConnection, preparedStatement);
    }

    static ResultSet getProxyResultSet(ProxyConnection proxyConnection, ProxyStatement proxyStatement, ResultSet resultSet) {
        return new HikariProxyResultSet(proxyConnection, proxyStatement, resultSet);
    }

    static DatabaseMetaData getProxyDatabaseMetaData(ProxyConnection proxyConnection, DatabaseMetaData databaseMetaData) {
        return new HikariProxyDatabaseMetaData(proxyConnection, databaseMetaData);
    }
}

