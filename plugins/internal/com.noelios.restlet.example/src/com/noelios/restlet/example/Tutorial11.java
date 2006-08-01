/*
 * Copyright 2005-2006 Noelios Consulting.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * http://www.opensource.org/licenses/cddl1.txt
 * If applicable, add the following below this CDDL
 * HEADER, with the fields enclosed by brackets "[]"
 * replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.example;

import java.util.List;

import org.restlet.AbstractRestlet;
import org.restlet.Call;
import org.restlet.DefaultRouter;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.component.RestletContainer;
import org.restlet.data.ChallengeSchemes;
import org.restlet.data.MediaTypes;
import org.restlet.data.Protocols;

import com.noelios.restlet.DirectoryHandler;
import com.noelios.restlet.GuardFilter;
import com.noelios.restlet.HostRouter;
import com.noelios.restlet.LogFilter;
import com.noelios.restlet.StatusFilter;
import com.noelios.restlet.data.StringRepresentation;

/**
 * Routers and hierarchical URIs
 * @author Jerome Louvel (contact@noelios.com) <a href="http://www.noelios.com/">Noelios Consulting</a>
 */
public class Tutorial11
{
   public static void main(String[] args)
   {
      try
      {
         // Create a new Restlet container
      	RestletContainer myContainer = new RestletContainer();

         // Add an HTTP server connector to the Restlet container. 
         // Note that the container is the call restlet.
         myContainer.getServers().put("HTTP Server", Protocols.HTTP, 8182);

         // Attach a log Filter to the container
         LogFilter log = new LogFilter(myContainer, "com.noelios.restlet.example");
         myContainer.setRoot(log);

         // Attach a status Filter to the log Filter
         StatusFilter status = new StatusFilter(myContainer, true, "webmaster@mysite.org", "http://www.mysite.org");
         log.setTarget(status);

         // Create a host router matching calls to the server
         HostRouter host = new HostRouter(myContainer, 8182);
         status.setTarget(host);

         // Attach a guard Filter to secure access the the chained directory Restlet
         GuardFilter guard = new GuardFilter(myContainer, "com.noelios.restlet.example", true, ChallengeSchemes.HTTP_BASIC , "Restlet tutorial", true);
         guard.getAuthorizations().put("scott", "tiger");
         host.getScorers().add("/docs/", guard);

         // Create a directory Restlet able to return a deep hierarchy of Web files
         DirectoryHandler directory = new DirectoryHandler(myContainer, "file:///D:/Restlet/www/docs/api/", true, "index");
         guard.setTarget(directory);

         // Create the user router
         Router user = new DefaultRouter(myContainer);
         host.getScorers().add("/users/[a-z]+", user);

         // Create the account Restlet
         Restlet account = new AbstractRestlet()
            {
         		public void handleGet(Call call)
               {
                  // Print the requested URI path
                  String output = "Account of user named: " + call.getBaseRef().getLastSegment();
                  call.setOutput(new StringRepresentation(output, MediaTypes.TEXT_PLAIN));
               }
            };
         user.getScorers().add("$", account);

         // Create the orders Restlet
         Restlet orders = new AbstractRestlet(myContainer)
            {
               public void handleGet(Call call)
               {
                  // Print the user name of the requested orders
                  List<String> segments = call.getBaseRef().getSegments();
                  String output = "Orders of user named: " + segments.get(segments.size() - 2);
                  call.setOutput(new StringRepresentation(output, MediaTypes.TEXT_PLAIN));
               }
            };
         user.getScorers().add("/orders$", orders);

         // Now, let's start the container!
         myContainer.start();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}
