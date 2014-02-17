package org.neo4j.kernel.codegen;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

public class ASTType
{
    public static Type type( Class<?> clazz, AST ast)
    {
        return type( clazz.getCanonicalName(), ast );
    }

    public static Type type( String name, AST ast )
    {
        Map<String, PrimitiveType.Code> primitiveCodes = new HashMap<>();
        primitiveCodes.put( "float", PrimitiveType.FLOAT );
        primitiveCodes.put( "double", PrimitiveType.DOUBLE );
        primitiveCodes.put( "long", PrimitiveType.LONG );
        primitiveCodes.put( "int", PrimitiveType.INT );
        primitiveCodes.put( "short", PrimitiveType.SHORT );
        primitiveCodes.put( "char", PrimitiveType.CHAR );
        primitiveCodes.put( "byte", PrimitiveType.BYTE );
        primitiveCodes.put( "boolean", PrimitiveType.BOOLEAN );
        primitiveCodes.put( "void", PrimitiveType.VOID );

        if(name.endsWith( "[]" ))
        {
            return ast.newArrayType( type(name.substring( 0, name.length() - 2 ), ast) );
        }
        else
        {
            if(primitiveCodes.containsKey( name ))
            {
                return ast.newPrimitiveType( primitiveCodes.get( name ) );
            }
            else
            {
                try
                {
                    String[] parts = name.split( "\\." );
                    Type type = null;
                    for(String p : parts)
                    {
                        if(type == null)
                        {
                            type = ast.newSimpleType( ast.newSimpleName( p ) );
                        }
                        else
                        {
                            type = ast.newQualifiedType( type, ast.newSimpleName( p ) );
                        }
                    }
                    return type;
                } catch(IllegalArgumentException e)
                {
                    throw new RuntimeException( "Can't define " + name + ", is it a primitive or a complex name?" );
                }
            }
        }
    }

    /** Remove any type parameters from a given type */
    public static Type ensureNoTypeParams(Type type)
    {
        if(type.isParameterizedType())
        {
            ParameterizedType par = (ParameterizedType)type;
            return par.getType();
        }
        return type;
    }
}
