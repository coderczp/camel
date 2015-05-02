/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.hazelcast.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;

import org.apache.camel.Exchange;
import org.apache.camel.component.hazelcast.HazelcastComponentHelper;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastDefaultProducer;
import org.apache.camel.util.ObjectHelper;

public class HazelcastMapProducer extends HazelcastDefaultProducer {

    private final IMap<Object, Object> cache;

    public HazelcastMapProducer(HazelcastInstance hazelcastInstance, HazelcastMapEndpoint endpoint, String cacheName) {
        super(endpoint);
        this.cache = hazelcastInstance.getMap(cacheName);
    }

    public void process(Exchange exchange) throws Exception {

        Map<String, Object> headers = exchange.getIn().getHeaders();

        // get header parameters
        Object oid = null;
        Object ovalue = null;
        String query = null;

        if (headers.containsKey(HazelcastConstants.OBJECT_ID)) {
            oid = headers.get(HazelcastConstants.OBJECT_ID);
        }
        
        if (headers.containsKey(HazelcastConstants.OBJECT_VALUE)) {
            ovalue = headers.get(HazelcastConstants.OBJECT_VALUE);
        }

        if (headers.containsKey(HazelcastConstants.QUERY)) {
            query = (String) headers.get(HazelcastConstants.QUERY);
        }

        final int operation = lookupOperationNumber(exchange);
        switch (operation) {

        case HazelcastConstants.PUT_OPERATION:
            this.put(oid, exchange);
            break;

        case HazelcastConstants.GET_OPERATION:
            this.get(oid, exchange);
            break;
            
        case HazelcastConstants.GET_ALL_OPERATION:
            this.getAll(oid, exchange);
            break;

        case HazelcastConstants.DELETE_OPERATION:
            this.delete(oid);
            break;

        case HazelcastConstants.UPDATE_OPERATION:
            this.update(oid, exchange);
            break;

        case HazelcastConstants.QUERY_OPERATION:
            this.query(query, exchange);
            break;

        case HazelcastConstants.REPLACE_OPERATION:
            if (ObjectHelper.isEmpty(ovalue)) {
                this.replace(oid, exchange);
            } else {
                this.replace(oid, ovalue, exchange);
            }
            break;

        case HazelcastConstants.CLEAR_OPERATION:
            this.clear(exchange);
            break;
            
        default:
            throw new IllegalArgumentException(String.format("The value '%s' is not allowed for parameter '%s' on the MAP cache.", operation, HazelcastConstants.OPERATION));
        }

        // finally copy headers
        HazelcastComponentHelper.copyHeaders(exchange);

    }

    /**
     * query map with a sql like syntax (see http://www.hazelcast.com/)
     */
    private void query(String query, Exchange exchange) {
        Collection<Object> result;
        if (ObjectHelper.isNotEmpty(query)) {
            result = this.cache.values(new SqlPredicate(query));
        } else {
            result = this.cache.values();
        }
        exchange.getOut().setBody(result);
    }

    /**
     * update an object in your cache (the whole object will be replaced)
     */
    private void update(Object oid, Exchange exchange) {
        Object body = exchange.getIn().getBody();
        this.cache.lock(oid);
        this.cache.replace(oid, body);
        this.cache.unlock(oid);
    }

    /**
     * remove an object from the cache
     */
    private void delete(Object oid) {
        this.cache.remove(oid);
    }

    /**
     * find an object by the given id and give it back
     */
    private void get(Object oid, Exchange exchange) {
        exchange.getOut().setBody(this.cache.get(oid));
    }
    
    
    /**
     * get All objects and give it back
     */
    private void getAll(Object oid, Exchange exchange) {
        exchange.getOut().setBody(this.cache.getAll((Set<Object>) oid));
    }

    /**
     * put a new object into the cache
     */
    private void put(Object oid, Exchange exchange) {
        Object body = exchange.getIn().getBody();
        this.cache.put(oid, body);
    }
    
    /**
     * replace a value related to a specific key
     */
    private void replace(Object oid, Exchange exchange) {
        Object body = exchange.getIn().getBody();
        this.cache.replace(oid, body);
    }
    
    /**
     * Replaces the entry for given id with a specific value in the body, only if currently mapped to a given value
     */
    private void replace(Object oid, Object ovalue, Exchange exchange) {
        Object body = exchange.getIn().getBody();
        this.cache.replace(oid, ovalue, body);
    }
    
    /**
     * Clear all the entries
     */
    private void clear(Exchange exchange) {
        this.cache.clear();
    }
}