module com.zaxxer.hikari {
    requires java.sql;
    requires java.management;
    requires java.naming;
    requires org.slf4j;
    /* static phase */ requires org.hibernate.orm.core;
    /* static phase */ requires simpleclient;
    /* static phase */ requires metrics.core;
    /* static phase */ requires metrics.healthchecks;
    /* static phase */ requires micrometer.core;

    exports com.zaxxer.hikari;
    exports com.zaxxer.hikari.hibernate;
    exports com.zaxxer.hikari.metrics;
    exports com.zaxxer.hikari.metrics.dropwizard;
    exports com.zaxxer.hikari.metrics.micrometer;
    exports com.zaxxer.hikari.metrics.prometheus;
    exports com.zaxxer.hikari.pool;
    exports com.zaxxer.hikari.util;

}

