function nonEnum(value) {
    return {
        configurable: true,
        enumerable: false,
        value: value,
        writable: true
    };
}

function getMoveNext(innerFunction, self) {
    return function(ctx) {
        while (true) {
            try {
                return innerFunction.call(self, ctx);
            } catch (ex) {
                handleCatch(ctx, ex);
            }
        }
    };
}

function GeneratorFunction() {}
function GeneratorFunctionPrototype() {}
GeneratorFunction.prototype = GeneratorFunctionPrototype;
Object.defineProperty(GeneratorFunctionPrototype, 'constructor', nonEnum(GeneratorFunction));
GeneratorFunctionPrototype.prototype = {
    constructor: GeneratorFunctionPrototype,
    next: function(v) {
        return nextOrThrow(this[ctxName], this[moveNextName], 'next', v);
    },
    throw: function(v) {
        return nextOrThrow(this[ctxName], this[moveNextName], 'throw', v);
    },
    return: function(v) {
        this[ctxName].oldReturnValue = this[ctxName].returnValue;
        this[ctxName].returnValue = v;
        return nextOrThrow(this[ctxName], this[moveNextName], 'throw', RETURN_SENTINEL);
    }
};
Object.defineProperties(GeneratorFunctionPrototype.prototype, {
    constructor: {enumerable: false},
    next: {enumerable: false},
    throw: {enumerable: false},
    return: {enumerable: false}
});
Object.defineProperty(GeneratorFunctionPrototype.prototype, Symbol.iterator, nonEnum(function() {
    return this;
}));

function createGeneratorInstance(innerFunction, functionObject, self) {
    var moveNext = getMoveNext(innerFunction, self);
    var ctx = {};//new GeneratorContext();
    var object = Object.create(functionObject.prototype);
    //object[ctxName] = ctx;
    //object[moveNextName] = moveNext;
    return object;
}

function initGeneratorFunction(functionObject) {
    functionObject.prototype = Object.create(GeneratorFunctionPrototype.prototype);
    print( "funcObj.prototype> " + functionObject.prototype );
    functionObject.__proto__ = GeneratorFunctionPrototype;
    return functionObject;
}

var genFunc = initGeneratorFunction( function myGenerator() {
    return createGeneratorInstance(function($ctx) { });
});

createGeneratorInstance( function(){}, genFunc, this );