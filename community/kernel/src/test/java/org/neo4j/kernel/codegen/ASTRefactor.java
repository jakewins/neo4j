package org.neo4j.kernel.codegen;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import static org.eclipse.jdt.core.dom.TypeDeclaration.MODIFIERS2_PROPERTY;

/**
 * Higher-level methods for JDT rewrite.
 */
public class ASTRefactor
{
    private final ASTRewrite rewriter;
    private final AST ast;

    public ASTRefactor( ASTRewrite rewrite, AST ast )
    {
        this.rewriter = rewrite;
        this.ast = ast;
    }

    public void setModifiers( TypeDeclaration node, Modifier.ModifierKeyword ... modifierKeywords )
    {
        ListRewrite listRewrite = rewriter.getListRewrite( node, MODIFIERS2_PROPERTY );
        for ( Object o : listRewrite.getOriginalList() )
        {
            if(o instanceof Modifier )
            {
                listRewrite.remove( (Modifier) o, null );
            }
        }
        for ( Modifier.ModifierKeyword modifier : modifierKeywords )
        {
            listRewrite.insertLast( ast.newModifier(modifier), null );
        }
    }
}
