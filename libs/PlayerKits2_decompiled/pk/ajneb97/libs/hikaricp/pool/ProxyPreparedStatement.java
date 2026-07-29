/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp.pool;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import pk.ajneb97.libs.hikaricp.pool.ProxyConnection;
import pk.ajneb97.libs.hikaricp.pool.ProxyFactory;
import pk.ajneb97.libs.hikaricp.pool.ProxyStatement;

public abstract class ProxyPreparedStatement
extends ProxyStatement
implements PreparedStatement {
    ProxyPreparedStatement(ProxyConnection connection, PreparedStatement statement) {
        super(connection, statement);
    }

    @Override
    public boolean execute() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).execute();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        this.connection.markCommitStateDirty();
        ResultSet resultSet = ((PreparedStatement)this.delegate).executeQuery();
        return ProxyFactory.getProxyResultSet(this.connection, this, resultSet);
    }

    @Override
    public int executeUpdate() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).executeUpdate();
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).executeLargeUpdate();
    }
}

