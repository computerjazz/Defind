package com.danielmerrill.ocrlive;

/**
 * Created by danielmerrill on 5/29/15.
 *
 * Defines an interface for asynctasks to return data to clients
 */
public interface AsyncResponse {
    void processFinish(String output, AsyncTypes.Type type);
}
