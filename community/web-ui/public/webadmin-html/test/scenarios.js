"use strict";

describe("Neo4j Web UI", function() {
  beforeEach(function() {
    return browser().navigateTo("/");
  });
  it("should show a set of frontpage articles", function() {
    return expect(repeater("[ng-view] h2").count()).toEqual(3);
  });
  it("should have a try-me cypher statement", function() {
    var query;
    query = "START n=node(0) RETURN 'hello', 'world'";
    input(".hero-query-wrapper textarea").enter(query);
    element("[ng-view] .hero-query-wrapper .btn").click();
    return expect(repeater("[ng-view] table tr").count()).toEqual(2);
  });
  return describe("data browser", function() {
    return beforeEach(function() {
      return browser().navigateTo("#/data/browser");
    });
  });
});

