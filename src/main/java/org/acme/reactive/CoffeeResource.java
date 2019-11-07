/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.acme.reactive;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.axle.pgclient.PgPool;

@Path("coffee")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoffeeResource {

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @Inject
    PgPool client;

    @PostConstruct
    void config() {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS fruits")
                .thenCompose(r -> client.query("CREATE TABLE coffee (id SERIAL PRIMARY KEY, name TEXT NOT NULL)"))
                .thenCompose(r -> client.query("INSERT INTO coffee (name) VALUES ('Americano')"))
                .thenCompose(r -> client.query("INSERT INTO coffee (name) VALUES ('Latte')"))
                .thenCompose(r -> client.query("INSERT INTO coffee (name) VALUES ('Mocha')"))
                .toCompletableFuture()
                .join();
    }

    @GET
    public CompletionStage<Response> get() {
        return Coffee.findAll(client)
                .thenApply(Response::ok)
                .thenApply(ResponseBuilder::build);
    }

    @GET
    @Path("{id}")
    public CompletionStage<Response> getSingle(@PathParam Long id) {
        return Coffee.findById(client, id)
                .thenApply(coffee -> coffee != null ? Response.ok(coffee) : Response.status(Status.NOT_FOUND))
                .thenApply(ResponseBuilder::build);
    }

    @POST
    public CompletionStage<Response> create(Coffee coffee) {
        return coffee.save(client)
                .thenApply(id -> URI.create("/coffee/" + id))
                .thenApply(uri -> Response.created(uri).build());
    }

    @PUT
    @Path("{id}")
    public CompletionStage<Response> update(@PathParam Long id, Coffee coffee) {
        return coffee.update(client)
                .thenApply(updated -> updated ? Status.OK : Status.NOT_FOUND)
                .thenApply(status -> Response.status(status).build());
    }

    @DELETE
    @Path("{id}")
    public CompletionStage<Response> delete(@PathParam Long id) {
        return Coffee.delete(client, id)
                .thenApply(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
                .thenApply(status -> Response.status(status).build());
    }
}