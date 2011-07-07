/**
 *     Copyright 2011 Membase, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.couchbase.mock.memcached;

import java.security.AccessControlException;
import java.util.Map;

/**
 * A small little datastore.. Please note that since this is a dummy
 * datastore I'm using in my test program, I don't care if the operations
 * are atomic... feel free to change that if you like...
 *
 * @author Trond Norbye
 */
public class DataStore {

    volatile long casCounter = 1;
    private VBucket vBucketMap[];

    public DataStore(int size) {
        vBucketMap = new VBucket[size];
        for (int ii = 0; ii < size; ++ii) {
            vBucketMap[ii] = new VBucket(null);
        }
    }

    public VBucket getVBucket(short vbucket) {
        if (vbucket >= vBucketMap.length) {
            // Illegal vbucket.. just report as no access..
            throw new AccessControlException("Illegal vbucket");
        }
        return vBucketMap[vbucket];
    }

    private Map<String, Item> getMap(MemcachedServer server, short vbucket) throws AccessControlException {
        if (vbucket >= vBucketMap.length) {
            // Illegal vbucket.. just report as no access..
            throw new AccessControlException("Illegal vbucket");
        }
        return vBucketMap[vbucket].getMap(server);
    }

    public void setOwnership(int vbucket, MemcachedServer server) {
        vBucketMap[vbucket].setOwner(server);
    }

    public ErrorCode add(MemcachedServer server, short vBucketId, Item item) {
        // I don't give a shit about atomicy right now..
        Map<String, Item> map = getMap(server, vBucketId);
        if (map.get(item.getKey()) != null || item.getCas() != 0) {
            return ErrorCode.KEY_EEXISTS;
        }

        item.setCas(++casCounter);
        map.put(item.getKey(), item);
        return ErrorCode.SUCCESS;
    }

    public ErrorCode replace(MemcachedServer server, short vBucketId, Item item) {
        // I don't give a shit about atomicy right now..
        Map<String, Item> map = getMap(server, vBucketId);
        Item old = map.get(item.getKey());
        if (old == null) {
            return ErrorCode.KEY_ENOENT;
        }

        if (item.getCas() != old.getCas()) {
            if (item.getCas() != 0) {
                return ErrorCode.KEY_EEXISTS;
            }
        }

        item.setCas(++casCounter);
        map.put(item.getKey(), item);
        return ErrorCode.SUCCESS;
    }

    public ErrorCode set(MemcachedServer server, short vBucketId, Item item) {
        Map<String, Item> map = getMap(server, vBucketId);
        if (item.getCas() == 0) {
            item.setCas(++casCounter);
            map.put(item.getKey(), item);
            return ErrorCode.SUCCESS;
        }
        return replace(server, vBucketId, item);
    }

    ErrorCode delete(MemcachedServer server, short vBucketId, String key, long cas) {
        // I don't give a shit about atomicy right now..
        Map<String, Item> map = getMap(server, vBucketId);
        if (cas == 0) {
            map.remove(key);
            return ErrorCode.SUCCESS;
        } else {
            Item i = map.get(key);
            if (i.getCas() != cas) {
                return ErrorCode.KEY_EEXISTS;
            }
            map.remove(key);
            return ErrorCode.SUCCESS;
        }
    }

    Item get(MemcachedServer server, short vBucketId, String key) {
        Map<String, Item> map = getMap(server, vBucketId);
        return map.get(key);
    }
}
