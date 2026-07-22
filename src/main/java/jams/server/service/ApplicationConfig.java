/*
 * ApplicationConfig.java
 * Created on 01.03.2014, 21:37:11
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 *
 * @author Christian Fischer <christian.fischer.2@uni-jena.de>
 */
@jakarta.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

    public static String SERVER_UPLOAD_DIRECTORY = null;//"e:/test_server/uploaded/";
    public static String SERVER_TMP_DIRECTORY=  null;//"E:/test_server/tmp/";
    public static String SERVER_EXEC_DIRECTORY = null;//"E:/test_server/exec/";
    public static String SERVER_MAX_MEM = null;
    
    static Properties p = new Properties() {
        {
            Logger log = Logger.getLogger(ApplicationConfig.class.getName());
            File f = new File("settings.properties");
            if (f.exists()) {
                try {
                    load(new FileReader(f));
                } catch (Throwable ioe) {
                    log.log(Level.WARNING, ioe.getMessage(), ioe);
                }
            } else {
                log.info("settings.properties not found; using environment variables");
            }
            // Environment variables take precedence over settings.properties (Docker-friendly).
            SERVER_UPLOAD_DIRECTORY = cfg("UPLOAD_DIRECTORY", getProperty("upload-directory"), null);
            SERVER_TMP_DIRECTORY = cfg("TMP_DIRECTORY", getProperty("tmp-directory"), null);
            SERVER_EXEC_DIRECTORY = cfg("EXEC_DIRECTORY", getProperty("exec-directory"), null);
            SERVER_MAX_MEM = cfg("SERVER_MAX_MEM", getProperty("server-max-mem"), "8g");
            log.info("JAMS server paths [" +
                    SERVER_UPLOAD_DIRECTORY + ", " +
                    SERVER_TMP_DIRECTORY + ", " +
                    SERVER_EXEC_DIRECTORY + "], max-mem " + SERVER_MAX_MEM);
            // Ensure the working directories exist. With a bind-mounted data volume
            // these subdirectories are not provided by the image, so create them.
            for (String dir : new String[]{SERVER_UPLOAD_DIRECTORY, SERVER_TMP_DIRECTORY, SERVER_EXEC_DIRECTORY}) {
                if (dir != null && !dir.isEmpty()) {
                    try {
                        new File(dir).mkdirs();
                    } catch (Throwable t) {
                        log.log(Level.WARNING, "Could not create directory " + dir, t);
                    }
                }
            }
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
    @Override
    public Set<Class<?>> getClasses() {                                        
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        if (!resources.contains(MultiPartFeature.class))
            resources.add(MultiPartFeature.class);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(cors.CrossOriginResourceSharingFilter.class);
        resources.add(jams.server.service.FileFacadeREST.class);
        resources.add(jams.server.service.JobFacadeREST.class);
        resources.add(jams.server.service.ServerInformationREST.class);
        resources.add(jams.server.service.UserFacadeREST.class);
        resources.add(jams.server.service.WorkspaceFacadeREST.class);
    }
    
}
