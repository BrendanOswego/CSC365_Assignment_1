package com.example.brendan.mainpackage.model;

import java.util.List;

/**
 * Model class for Location from JSON
 */

public class LocationModel {

    private Metadata metadata;
    private List<Result> results = null;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
