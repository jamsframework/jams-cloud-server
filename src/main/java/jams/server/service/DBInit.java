/*
 * DBInit.java
 * Created on 19.08.2015, 23:42:43
 *
 * This file is part of JAMS
 * Copyright (C) FSU Jena
 *
 * JAMS is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JAMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JAMS. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package jams.server.service;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

/**
 *
 * @author christian
 */
@Singleton
@Startup
public class DBInit {

    static private final Logger log = Logger.getLogger(DBInit.class.getName());

    @Resource(lookup = "jdbc/jamsserver")
    private DataSource dataSource;

    public static String DATABASE_URL = null;//"e:/test_server/uploaded/";
    public static String DATABASE_USER = null;//"E:/test_server/tmp/";
    public static String DATABASE_PW = null;//"E:/test_server/exec/";
    
    static Properties p = new Properties() {
        {
            File f = new File("settings.properties");
            if (f.exists()) {
                try {
                    load(new FileReader(f));
                } catch (Throwable ioe) {
                    log.log(Level.WARNING, ioe.getMessage(), ioe);
                }
            }
            // Environment variables take precedence over settings.properties (Docker-friendly).
            DATABASE_URL = cfg("DATABASE_URL", getProperty("databaseUrl"), null);
            DATABASE_USER = cfg("DATABASE_USER", getProperty("databaseUser"), null);
            DATABASE_PW = cfg("DATABASE_PASSWORD", getProperty("databasePw"), null);
        }
    };

    private static String cfg(String env, String prop, String def) {
        String v = System.getenv(env);
        if (v == null || v.isEmpty()) {
            v = prop;
        }
        if (v == null || v.isEmpty()) {
            v = def;
        }
        return v;
    }
    
    @PostConstruct
    private void onStartup() {
        if (dataSource == null) {
            log.severe("no datasource found to execute the db migrations!");
            throw new EJBException(
                    "no datasource found to execute the db migrations!");
        }
        
        
        Flyway flyway = Flyway.configure()
                .dataSource(DATABASE_URL, DATABASE_USER, DATABASE_PW)
                .baselineOnMigrate(true)
                .load();
        for (MigrationInfo i : flyway.info().all()) {
            log.info("migrate task: " + i.getVersion() + " : "
                    + i.getDescription() + " from file: " + i.getScript());
        }
        flyway.migrate();
                
    }
}
