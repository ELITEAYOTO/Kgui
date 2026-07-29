/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.libs.hikaricp.pool;

import java.sql.CallableStatement;
import pk.ajneb97.libs.hikaricp.pool.ProxyConnection;
import pk.ajneb97.libs.hikaricp.pool.ProxyPreparedStatement;

public abstract class ProxyCallableStatement
extends ProxyPreparedStatement
implements CallableStatement {
    protected ProxyCallableStatement(ProxyConnection connection, CallableStatement statement) {
        super(connection, statement);
    }
}

