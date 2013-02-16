module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('<!DOCTYPE html>\n<html lang="en" ng-app="app">\n  <head>\n    <meta charset="utf-8">\n    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">\n    <meta name="viewport" content="width=device-width" initial-scale="1.0">\n    <meta name="description" content="The Neo4j Web UI">\n    <meta name="author" content="Neo Technology">\n    <title ng-bind-template="{{pageTitle}}"></title>\n    <link rel="stylesheet" href="css/app.css">\n    <link rel="shortcut icon" href="img/favicon.ico"><!--[if lte IE 7]>\n    <script src="http://cdnjs.cloudflare.com/ajax/libs/json2/20110223/json2.js"></script><![endif]--><!--[if lte IE 8]>\n    <script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script>\n    <script src="build/angular-ui-ieshiv.js"></script><![endif]-->\n    <script>\n      window.brunch = window.brunch || {};\n      window.brunch[\'auto-reload\'] = {\n        enabled: true\n      };\n    </script>\n    <script src="js/vendor.js"></script>\n    <script src="js/app.js"></script>\n  </head>\n  <body ng-controller="AppCtrl">\n    <div class="wrapper">\n      <div class="navbar navbar navbar-inverse navbar-fixed-top">\n        <div class="navbar-inner">\n          <div class="container-fluid"><a ng-href="#/" class="brand"><img src="img/logo.png"></a></div>\n        </div>\n      </div>\n      <div ng-include="\'partials/sidebar.html\'" class="sidebar-container"></div>\n      <div ng-view class="main-content"></div>\n    </div>\n  </body>\n</html>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="data-browser">\n  <div class="data-browser-dash"> \n    <div class="data-browser-query-wrap">\n      <div ng-click="execute()" class="btn btn-primary data-browser-exec">Execute &raquo;</div>\n      <div class="code-mirror-wrap">\n        <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\', extraKeys:{\'Ctrl-Space\': \'cypherAutoComplete\',\'Ctrl-Enter\': \'cypherAutoComplete\'}}" ng-model="query"></textarea>\n      </div>\n    </div>\n  </div>\n  <div ng-show="error" class="row-fluid">\n    <div class="span12">\n      <h4>{{ error.exception }}</h4>\n      <pre>{{ error.message }}</pre>\n      <p>\n        <div data-toggle="collapse" data-target=".data-browser ul.stacktrace" class="btn btn-mini">Stacktrace</div>\n      </p>\n      <ul class="stacktrace collapse">\n        <li ng-repeat="stack in error.stacktrace"> \n          <pre>{{ stack }}</pre>\n        </li>\n      </ul>\n    </div>\n  </div>\n  <div class="row-fluid">\n    <div class="span12">\n      <table class="table">\n        <tr>\n          <th ng-repeat="column in columns">{{ column }}</th>\n        </tr>\n        <tr ng-repeat="row in rows">\n          <td ng-repeat="cell in row">{{ cell }}</td>\n        </tr>\n      </table>\n    </div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="data-console">\n  <div ng-repeat="interaction in interactions" class="row-fluid">\n    <div class="span12">\n      <pre class="data-console-statement">{{ interaction.statement }}</pre>\n      <pre class="data-console-result">   {{ interaction.result }}</pre>\n    </div>\n  </div>\n  <div class="row-fluid">\n    <div ng-click="execute()" class="btn btn-primary data-console-exec">Execute &raquo;</div>\n    <div>\n      <div class="prefix">></div>\n      <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\'}" ng-model="statement"></textarea>\n    </div>\n  </div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div ng-controller="SidebarController" class="sidebar">\n  <p><span>2.0 Community Edition</span></p>\n  <ul class="main-menu">\n    <li ng-repeat="item in menuItems" ng-class="{active: item.active}"><a ng-href="{{ item.href }}"><span class="icon icon-{{ item.icon }}"></span>{{ item.title }}</a></li>\n  </ul>\n  <div class="sidebar-resources"><small>Resources</small><br/><a href="http://docs.neo4j.org/chunked/stable/tutorials-rest.html" class="btn btn-link btn-mini">Getting started</a><a href="http://docs.neo4j.org/chunked/stable/" class="btn btn-link btn-mini">Documentation</a></a><a href="https://github.com/neo4j/neo4j/issues/new" class="btn btn-link btn-mini">Report an issue</a></div>\n</div>');
}
return buf.join("");
};module.exports = function anonymous(locals, attrs, escape, rethrow, merge) {
attrs = attrs || jade.attrs; escape = escape || jade.escape; rethrow = rethrow || jade.rethrow; merge = merge || jade.merge;
var buf = [];
with (locals || {}) {
var interp;
var __indent = [];
buf.push('\n<div class="row-fluid">\n  <div class="span12">\n    <div class="hero-unit hero-unit-inverse neo-20-announcement-hero">\n      <div class="hero-query-wrapper">\n        <div class="hero-query-logo-wrapper"><img src="img/big-logo.png" class="hero-query-logo"/>\n          <div ng-click="execute()" class="btn btn-large btn-primary splash-exec">Execute &raquo;</div>\n          <div class="code-mirror-wrap">\n            <textarea ui-codemirror="{theme:\'eclipse\',mode:\'cypher\'}" ng-model="query" class="hero-query-input"></textarea>\n          </div>\n        </div>\n      </div>\n    </div>\n  </div>\n</div>\n<div class="row-fluid">\n  <div class="span4">\n    <h2>New to Neo4j?</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui.</p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n  <div class="span4">\n    <h2>Labels!</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui.</p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n  <div class="span4">\n    <h2>Cypher performance</h2>\n    <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>\n    <p><a class="btn">View details &raquo;</a></p>\n  </div>\n</div>');
}
return buf.join("");
};