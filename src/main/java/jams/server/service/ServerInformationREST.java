/*
 * UserFacadeREST.java
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

import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author Christian Fischer <christian.fischer.2@uni-jena.de>
 */
@Stateless
@Path("")
public class ServerInformationREST  {

    final String VERSION = "0.1.0.1";
    
    public ServerInformationREST() {
    }
        
    @GET
    @Path("version")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response version(@Context HttpServletRequest req) {    
        req.getSession();
        return Response.ok(VERSION).build();
    }
}
