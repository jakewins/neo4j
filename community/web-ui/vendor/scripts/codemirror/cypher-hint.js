(function () {
  function forEach(arr, f) {
    for (var i = 0, e = arr.length; i < e; ++i) f(arr[i]);
  }
  
  function arrayContains(arr, item) {
    if (!Array.prototype.indexOf) {
      var i = arr.length;
      while (i--) {
        if (arr[i] === item) {
          return true;
        }
      }
      return false;
    }
    return arr.indexOf(item) != -1;
  }

  function scriptHint(editor, keywords, getToken, options) {
    // Find the token at the cursor
    var cur = editor.getCursor(), token = getToken(editor, cur), tprop = token;
    // If it's not a 'word-style' token, ignore the token.
		if (!/^[\w$_]*$/.test(token.string)) {
      token = tprop = {start: cur.ch, end: cur.ch, string: "", state: token.state,
                       className: token.string == "." ? "property" : null};
    }
    // If it is a property, find out what it is a property of.
    while (tprop.className == "property") {
      tprop = getToken(editor, {line: cur.line, ch: tprop.start});
      if (tprop.string != ".") return;
      tprop = getToken(editor, {line: cur.line, ch: tprop.start});
      if (tprop.string == ')') {
        var level = 1;
        do {
          tprop = getToken(editor, {line: cur.line, ch: tprop.start});
          switch (tprop.string) {
          case ')': level++; break;
          case '(': level--; break;
          default: break;
          }
        } while (level > 0);
        tprop = getToken(editor, {line: cur.line, ch: tprop.start});
				if (tprop.className == 'variable')
					tprop.className = 'function';
				else return; // no clue
      }
      if (!context) var context = [];
      context.push(tprop);
    }
    return {list: getCompletions(token, context, keywords, options),
            from: {line: cur.line, ch: token.start},
            to: {line: cur.line, ch: token.end}};
  }

  CodeMirror.cypherHint = function(editor, options) {
    return scriptHint(editor, cypherKeywords,
                      function (e, cur) {return e.getTokenAt(cur);},
                      options);
  };

  var cypherKeywords = [
    "node", "nodes", "relationship", "relationships", 
    "any", "all", "none", "single", "length", "type", "id", 
    "coalesce", "head", "last", "extract", "filter", "tail", 
    "range", "reduce", "abs", "round", "sqrt", "sign", "str",
    "replace", "substring", "left", "right", "ltrim", "rtrim",
    "trim", "lower", "upper", 
    
    'START', 'MATCH', 'WHERE', 'RETURN',
    'CREATE', 'UNIQUE', 'SET', 'DELETE',
    'FOREACH', 'IN',
    'ORDER', 'BY', 'SKIP', 'LIMIT',

    'COUNT', 'SUM', 'AVG', 'PERCENTILE', 'DISC',
    'DISTINCT', 'COLLECT', 'MIN', 'MAX', 'CONT'];

  function getCompletions(token, context, keywords, options) {
    var found = [], start = token.string;
    function maybeAdd(str) {
      if (str.indexOf(start) == 0 && !arrayContains(found, str)) found.push(str);
    }
    function gatherCompletions(obj) {
      for (var name in obj) maybeAdd(name);
    }

    if (context) {
      var obj = context.pop(), base;
      if (obj.className == "variable") {
        if (options && options.additionalContext)
          base = options.additionalContext[obj.string];
        base = base || window[obj.string];
      } else if (obj.className == "string") {
        base = "";
      } else if (obj.className == "atom") {
        base = 1;
      } 
      while (base != null && context.length)
        base = base[context.pop().string];
      if (base != null) gatherCompletions(base);
    }
    else {
      forEach(token.state.vars, function(v) {
        if(v.name != token.string) {
          maybeAdd(v.name); 
        }
      });
      forEach(keywords, maybeAdd);
    }
    return found;
  }
  
  CodeMirror.commands.cypherAutoComplete = function(cm) {
    CodeMirror.showHint(cm, CodeMirror.cypherHint);
  }
})();
